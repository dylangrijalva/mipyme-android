package co.mipyme.sync.internal

import co.mipyme.contract.MipymeError
import co.mipyme.contract.MipymeErrorCode
import co.mipyme.network.NetworkError
import co.mipyme.sync.SyncError
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncErrorMappingTest {
    @Test
    fun `connectivity failures map to network`() {
        assertEquals(SyncError.Network, NetworkError.NetworkUnavailable(IOException()).toSyncError())
        assertEquals(SyncError.Network, NetworkError.Timeout(IOException()).toSyncError())
    }

    @Test
    fun `payload and unexpected failures map to unknown`() {
        assertEquals(SyncError.Unknown, NetworkError.Serialization(IllegalStateException()).toSyncError())
        assertEquals(SyncError.Unknown, NetworkError.Unexpected(IllegalStateException()).toSyncError())
    }

    @Test
    fun `retryable statuses map to server`() {
        assertEquals(SyncError.Server, response(500, MipymeErrorCode.INTERNAL_SERVER_ERROR).toSyncError())
        assertEquals(SyncError.Server, response(503, MipymeErrorCode.SYNC_UNAVAILABLE).toSyncError())
        assertEquals(SyncError.Server, response(429, MipymeErrorCode.RATE_LIMIT_EXCEEDED).toSyncError())
    }

    @Test
    fun `auth statuses map to unauthorized and forbidden`() {
        assertEquals(SyncError.Unauthorized, response(401, MipymeErrorCode.INVALID_OAUTH_TOKEN).toSyncError())
        assertEquals(SyncError.Forbidden, response(403, MipymeErrorCode.FORBIDDEN_ERROR).toSyncError())
    }

    @Test
    fun `other client errors map to unknown`() {
        assertEquals(SyncError.Unknown, response(400, MipymeErrorCode.VALIDATION_ERROR).toSyncError())
    }

    @Test
    fun `only the sync token invalid response is a token rejection`() {
        assertTrue(response(400, MipymeErrorCode.SYNC_TOKEN_INVALID).isSyncTokenInvalid())
        assertFalse(response(400, MipymeErrorCode.VALIDATION_ERROR).isSyncTokenInvalid())
        assertFalse(NetworkError.Timeout(IOException()).isSyncTokenInvalid())
    }

    private fun response(status: Int, code: MipymeErrorCode): NetworkError.Response {
        return NetworkError.Response(
            MipymeError(code = code, message = "m", details = emptyList(), requestId = "r", status = status),
        )
    }
}
