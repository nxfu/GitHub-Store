package zed.rainxch.core.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import zed.rainxch.core.domain.model.InstallSource

@Entity(tableName = "installed_apps")
data class InstalledAppEntity(
    @PrimaryKey val packageName: String,
    val repoId: Long,
    val repoName: String,
    val repoOwner: String,
    val repoOwnerAvatarUrl: String,
    val repoDescription: String?,
    val primaryLanguage: String?,
    val repoUrl: String,
    val installedVersion: String,
    val installedAssetName: String?,
    val installedAssetUrl: String?,
    val latestVersion: String?,
    val latestAssetName: String?,
    val latestAssetUrl: String?,
    val latestAssetSize: Long?,
    val appName: String,
    val installSource: InstallSource,
    val signingFingerprint: String?,
    val installedAt: Long,
    val lastCheckedAt: Long,
    val lastUpdatedAt: Long,
    val isUpdateAvailable: Boolean,
    val updateCheckEnabled: Boolean = true,
    val releaseNotes: String? = "",
    val systemArchitecture: String,
    val fileExtension: String,
    val isPendingInstall: Boolean = false,
    val installedVersionName: String? = null,
    val installedVersionCode: Long = 0L,
    val latestVersionName: String? = null,
    val latestVersionCode: Long? = null,
    val latestReleasePublishedAt: String? = null,
    val includePreReleases: Boolean = false,
    /**
     * Per-app regex applied to asset (file) names. When non-null, only assets
     * whose name matches the pattern are considered installable for this app.
     * Used to track a single app inside a monorepo that ships multiple apps
     * (e.g. `ente-auth.*` against `ente-io/ente`).
     */
    val assetFilterRegex: String? = null,
    /**
     * When true, the update checker walks backward through past releases until
     * it finds one whose assets match [assetFilterRegex]. Required for
     * monorepos where the latest release is for a *different* app.
     */
    val fallbackToOlderReleases: Boolean = false,
)
