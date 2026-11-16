package co.mipyme.screen.inventory

import org.junit.Assert.*
import org.junit.Test

class InventoryScannerTest {
    @Test
    fun `one launch stays in flight and success cancellation failure and empty payload return unchanged`() {
        for (result in listOf(InventoryScanResult.Success("WANO"), InventoryScanResult.Success(""), InventoryScanResult.Cancelled, InventoryScanResult.Failure)) {
            var launches = 0
            lateinit var callback: (InventoryScanResult) -> Unit
            val session = InventoryScanSession { listener -> launches++; callback = listener }
            val results = mutableListOf<InventoryScanResult>()
            session.launch(results::add)
            session.launch(results::add)
            assertEquals(1, launches)
            assertTrue(session.scanning)
            callback(result)
            assertFalse(session.scanning)
            assertEquals(listOf(result), results)
        }
    }

    @Test
    fun `disposed and duplicate old callbacks cannot overwrite a later query`() {
        val callbacks = mutableListOf<(InventoryScanResult) -> Unit>()
        val session = InventoryScanSession { callbacks += it }
        val results = mutableListOf<InventoryScanResult>()
        session.launch(results::add)
        callbacks[0](InventoryScanResult.Cancelled)
        session.launch(results::add)
        callbacks[0](InventoryScanResult.Success("old"))
        assertTrue(session.scanning)
        assertEquals(listOf(InventoryScanResult.Cancelled), results)
        session.dispose()
        callbacks[1](InventoryScanResult.Success("late"))
        session.launch(results::add)
        assertEquals(2, callbacks.size)
        assertEquals(listOf(InventoryScanResult.Cancelled), results)
    }
}
