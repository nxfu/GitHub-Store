package zed.rainxch.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.GithubDeviceStart
import zed.rainxch.core.domain.model.GithubDeviceTokenSuccess

interface AuthenticationRepository {
    val accessTokenFlow: Flow<String?>

    suspend fun startDeviceFlow(): DeviceFlowStart

    suspend fun awaitDeviceToken(start: GithubDeviceStart): GithubDeviceTokenSuccess

    suspend fun pollDeviceTokenOnce(
        deviceCode: String,
        path: AuthPath,
    ): PollOutcome

    /**
     * Saves a user-supplied Personal Access Token as the active auth
     * credential.
     *
     * Validation flow:
     *   1. Client-side format check (rejects obvious paste-errors).
     *   2. Network-side check against GitHub's `/user` endpoint — if
     *      GitHub returns 401/403 we reject and do NOT persist. If GitHub
     *      is unreachable (timeout/DNS/block), we persist optimistically.
     *      A bad-but-unreachable token will surface a 401 on the first
     *      real authenticated call, same as any expired token.
     *
     * Use case: users on networks where the browser-side of device flow
     * (reaching `github.com/login/device`) is unreliable — they generate
     * a PAT on a device where GitHub works, paste it here, and skip the
     * browser dance entirely. Unreachable-but-save-anyway is deliberate:
     * the whole reason this feature exists is for users who can't reach
     * GitHub reliably in the moment.
     *
     * @return [Result.success] on persist, [Result.failure] on client-side
     *   format error or GitHub-side 401/403 rejection. On [Result.failure]
     *   the caller should keep the input sheet open so the user can fix
     *   the token.
     */
    suspend fun signInWithPat(token: String): Result<Unit>
}

enum class AuthPath { Backend, Direct }

data class DeviceFlowStart(
    val start: GithubDeviceStart,
    val path: AuthPath,
)

data class PollOutcome(
    val result: DevicePollResult,
    val path: AuthPath,
)

sealed interface DevicePollResult {
    data class Success(val token: GithubDeviceTokenSuccess) : DevicePollResult

    data object Pending : DevicePollResult

    data object SlowDown : DevicePollResult

    data class Failed(val error: Throwable) : DevicePollResult
}
