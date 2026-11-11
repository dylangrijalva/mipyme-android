package co.mipyme.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.mipyme.ui.component.MoshiText
import co.mipyme.ui.component.MoshiTextRole

@Composable
internal fun WelcomeScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        MoshiText(text = "Welcome", role = MoshiTextRole.Display)
    }
}
