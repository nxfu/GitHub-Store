package zed.rainxch.core.data.services

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES
import android.os.Build
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import zed.rainxch.core.domain.model.ApkPackageInfo
import zed.rainxch.core.domain.system.InstallerInfoExtractor
import java.io.File
import java.security.MessageDigest

class AndroidInstallerInfoExtractor(
    private val context: Context,
) : InstallerInfoExtractor {
    override suspend fun extractPackageInfo(filePath: String): ApkPackageInfo? =
        withContext(Dispatchers.IO) {
            try {
                val packageManager = context.packageManager

                val packageInfo = parseApk(packageManager, filePath)

                if (packageInfo == null) {
                    Logger.e {
                        "Failed to parse APK at $filePath, file exists: ${
                            File(filePath).exists()
                        }, size: ${File(filePath).length()}"
                    }
                    return@withContext null
                }

                val appInfo = packageInfo.applicationInfo
                appInfo?.sourceDir = filePath
                appInfo?.publicSourceDir = filePath

                val appName = appInfo?.let { packageManager.getApplicationLabel(it) }.toString()
                val versionCode =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo.versionCode.toLong()
                    }

                val fingerprint = extractSigningFingerprint(packageManager, packageInfo, filePath)

                ApkPackageInfo(
                    appName = appName,
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName ?: "unknown",
                    versionCode = versionCode,
                    signingFingerprint = fingerprint,
                )
            } catch (e: Exception) {
                Logger.e { "Failed to extract APK info: ${e.message}, file: $filePath" }
                null
            }
        }

    /**
     * Tries to parse the APK with full flags first (metadata + signing).
     * If that fails, retries with minimal flags since some APKs / Android
     * versions choke on GET_SIGNING_CERTIFICATES combined with other flags.
     */
    private fun parseApk(
        packageManager: PackageManager,
        filePath: String,
    ): android.content.pm.PackageInfo? {
        val fullFlags =
            PackageManager.GET_META_DATA or
                PackageManager.GET_ACTIVITIES or
                GET_SIGNING_CERTIFICATES

        val full = getPackageArchiveInfoCompat(packageManager, filePath, fullFlags)
        if (full != null) return full

        Logger.w {
            "Full-flag parse failed for $filePath, retrying with minimal flags"
        }

        // Retry without signing — the fingerprint will be extracted
        // separately in extractSigningFingerprint.
        val minimalFlags = PackageManager.GET_META_DATA
        return getPackageArchiveInfoCompat(packageManager, filePath, minimalFlags)
    }

    private fun getPackageArchiveInfoCompat(
        packageManager: PackageManager,
        filePath: String,
        flags: Int,
    ): android.content.pm.PackageInfo? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageArchiveInfo(
                filePath,
                PackageManager.PackageInfoFlags.of(flags.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageArchiveInfo(filePath, flags)
        }

    /**
     * Extracts the signing fingerprint from an already-parsed PackageInfo
     * if available, otherwise does a separate lightweight parse with only
     * the signing flag.
     */
    private fun extractSigningFingerprint(
        packageManager: PackageManager,
        packageInfo: android.content.pm.PackageInfo,
        filePath: String,
    ): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Try from the already-parsed info first (works when
            // the full-flag parse succeeded).
            val sigInfo = packageInfo.signingInfo
            if (sigInfo != null) {
                val certs =
                    if (sigInfo.hasMultipleSigners()) {
                        sigInfo.apkContentsSigners
                    } else {
                        sigInfo.signingCertificateHistory
                    }
                certs?.firstOrNull()?.toByteArray()?.let { certBytes ->
                    return sha256Fingerprint(certBytes)
                }
            }

            // Signing info missing (minimal-flag fallback path) —
            // do a separate parse with only the signing flag.
            val sigOnly = getPackageArchiveInfoCompat(
                packageManager,
                filePath,
                GET_SIGNING_CERTIFICATES,
            )
            val fallbackSigInfo = sigOnly?.signingInfo
            if (fallbackSigInfo != null) {
                val certs =
                    if (fallbackSigInfo.hasMultipleSigners()) {
                        fallbackSigInfo.apkContentsSigners
                    } else {
                        fallbackSigInfo.signingCertificateHistory
                    }
                certs?.firstOrNull()?.toByteArray()?.let { certBytes ->
                    return sha256Fingerprint(certBytes)
                }
            }

            return null
        } else {
            @Suppress("DEPRECATION")
            val legacyInfo =
                packageManager.getPackageArchiveInfo(
                    filePath,
                    PackageManager.GET_SIGNATURES,
                )
            @Suppress("DEPRECATION")
            return legacyInfo?.signatures?.firstOrNull()?.toByteArray()?.let { certBytes ->
                sha256Fingerprint(certBytes)
            }
        }
    }

    private fun sha256Fingerprint(certBytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(certBytes)
            .joinToString(":") { "%02X".format(it) }
}
