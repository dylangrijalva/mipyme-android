package co.mipyme.screen.inventory

import android.content.Context
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

internal sealed interface InventoryScanResult {
    data class Success(val text: String) : InventoryScanResult
    data object Cancelled : InventoryScanResult
    data object Failure : InventoryScanResult
}

internal fun interface InventoryScanner {
    fun scan(onResult: (InventoryScanResult) -> Unit)
}

internal class GoogleInventoryScanner(context: Context) : InventoryScanner {
    private val client = GmsBarcodeScanning.getClient(
        context,
        GmsBarcodeScannerOptions.Builder().enableAutoZoom().build(),
    )

    override fun scan(onResult: (InventoryScanResult) -> Unit) {
        try {
            client.startScan()
                .addOnSuccessListener { barcode -> onResult(InventoryScanResult.Success(barcode.rawValue.orEmpty())) }
                .addOnCanceledListener { onResult(InventoryScanResult.Cancelled) }
                .addOnFailureListener { onResult(InventoryScanResult.Failure) }
        } catch (_: Exception) {
            onResult(InventoryScanResult.Failure)
        }
    }
}

/** Owned by one screen visit. Disposing invalidates callbacks from an earlier visit. */
internal class InventoryScanSession(private val scanner: InventoryScanner) {
    var scanning: Boolean = false
        private set
    private var active = true
    private var requestId = 0

    fun launch(onResult: (InventoryScanResult) -> Unit) {
        if (!active || scanning) return
        scanning = true
        val launchedRequest = ++requestId
        scanner.scan { result ->
            if (active && scanning && requestId == launchedRequest) {
                scanning = false
                requestId++
                onResult(result)
            }
        }
    }

    fun dispose() {
        active = false
        scanning = false
    }
}
