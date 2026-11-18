package co.mipyme.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import co.mipyme.R
import co.mipyme.contract.ProductType
import co.mipyme.ui.Moshi
import java.io.Serializable
import java.util.UUID
import kotlinx.coroutines.launch

internal data class ProductEditorResult(val callerKey: String, val productId: String) : Serializable

private val LocalSavedProduct = staticCompositionLocalOf<MutableState<ProductEditorResult?>> {
    error("Missing product editor result host")
}

internal val LocalFinishProductEditor = staticCompositionLocalOf<(Route.ProductEditor, String) -> Unit> {
    error("Missing product editor result host")
}

@Composable
internal fun rememberProductEditorLauncher(onSaved: (String) -> Unit = {}): (ProductType, String?) -> Unit {
    val backStack = LocalNavBackStack.current
    val result = LocalSavedProduct.current
    val callerKey = rememberSaveable { UUID.randomUUID().toString() }
    val callback = rememberUpdatedState(onSaved)
    val delivery = result.value
    LaunchedEffect(delivery) {
        if (delivery != null && delivery.callerKey == callerKey) {
            result.value = null
            callback.value(delivery.productId)
        }
    }
    return { type, id ->
        if (backStack.lastOrNull() !is Route.ProductEditor) {
            backStack.add(Route.ProductEditor(type, id, callerKey))
        }
    }
}

@Composable
internal fun ProductEditorResultHost(content: @Composable () -> Unit) {
    val result = rememberSaveable { mutableStateOf<ProductEditorResult?>(null) }
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val backStack = LocalNavBackStack.current
    val finish: (Route.ProductEditor, String) -> Unit = { route, id ->
        if (backStack.lastOrNull() == route) {
            result.value = ProductEditorResult(route.callerKey, id)
            backStack.removeLastOrNull()
            val message = when (route.type) {
                ProductType.ITEM -> if (route.productId == null) R.string.product_editor_product_created else R.string.product_editor_product_updated
                ProductType.SERVICE -> if (route.productId == null) R.string.product_editor_service_created else R.string.product_editor_service_updated
            }
            scope.launch { host.showSnackbar(resources.getString(message)) }
        }
    }
    CompositionLocalProvider(LocalSavedProduct provides result, LocalFinishProductEditor provides finish) {
        Box(Modifier.fillMaxSize()) {
            content()
            SnackbarHost(host, Modifier.align(Alignment.BottomCenter).navigationBarsPadding()) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Moshi.colors.background.inverse,
                    contentColor = Moshi.colors.foreground.inverse,
                )
            }
        }
    }
}
