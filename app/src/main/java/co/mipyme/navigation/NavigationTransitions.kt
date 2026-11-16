package co.mipyme.navigation

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.navigation3.ui.NavDisplay

internal object NavigationTransitions {
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
