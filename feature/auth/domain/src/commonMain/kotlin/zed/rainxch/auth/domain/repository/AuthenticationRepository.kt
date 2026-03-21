package zed.rainxch.auth.domain.repository

import kotlinx.coroutines.flow.Flow
import zed.rainxch.core.domain.model.GithubDeviceStart
import zed.rainxch.core.domain.model.GithubDeviceTokenSuccess

interface AuthenticationRepository {
    val accessTokenFlow: Flow<String?>

    suspend fun startDeviceFlow(): GithubDeviceStart

    suspend fun awaitDeviceToken(start: GithubDeviceStart): GithubDeviceTokenSuccess

    /**
     * Single poll attempt. Returns:
     * - [Result.success] with non-null [GithubDeviceTokenSuccess] if user authorized
     * - [Result.success] with null if authorization is still pending (keep polling)
     * - [Result.failure] on terminal errors (denied, expired, invalid code)
     */
    suspend fun pollDeviceTokenOnce(deviceCode: String): Result<GithubDeviceTokenSuccess?>
}
