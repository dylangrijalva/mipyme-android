package co.mipyme.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

internal object NavigationTransitions {
    val slideUpWithDownPop = NavDisplay.transitionSpec {
        (slideInVertically(initialOffsetY = { it }) togetherWith ExitTransition.None).apply {
            targetContentZIndex = 1f
        }
    } + NavDisplay.popTransitionSpec {
        (EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })).apply {
            targetContentZIndex = -1f
        }
    } + NavDisplay.predictivePopTransitionSpec {
        (EnterTransition.None togetherWith slideOutVertically(targetOffsetY = { it })).apply {
            targetContentZIndex = -1f
        }
    }

    val forward = NavDisplay.transitionSpec {
        slideInHorizontally(initialOffsetX = { it }) togetherWith
            slideOutHorizontally(targetOffsetX = { -it })
    }

    val backward = NavDisplay.transitionSpec {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
    }

    val forwardWithBackwardPop = forward + NavDisplay.popTransitionSpec {
        slideInHorizontally(initialOffsetX = { -it }) togetherWith
            slideOutHorizontally(targetOffsetX = { it })
    }
}
