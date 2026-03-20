package zed.rainxch.home.presentation.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.vectorResource
import zed.rainxch.githubstore.core.presentation.res.*
import zed.rainxch.home.domain.model.HomePlatform

@Composable
fun HomePlatform.toIcons(): List<ImageVector> =
    when (this) {
        HomePlatform.All -> {
            listOf(
                vectorResource(Res.drawable.ic_platform_android),
                vectorResource(Res.drawable.ic_platform_linux),
                vectorResource(Res.drawable.ic_platform_macos),
                vectorResource(Res.drawable.ic_platform_windows),
            )
        }

        HomePlatform.Android -> {
            listOf(vectorResource(Res.drawable.ic_platform_android))
        }

        HomePlatform.Macos -> {
            listOf(vectorResource(Res.drawable.ic_platform_macos))
        }

        HomePlatform.Windows -> {
            listOf(vectorResource(Res.drawable.ic_platform_windows))
        }

        HomePlatform.Linux -> {
            listOf(vectorResource(Res.drawable.ic_platform_linux))
        }
    }
