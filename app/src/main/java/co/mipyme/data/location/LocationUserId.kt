package co.mipyme.data.location

import co.mipyme.auth.AuthState

val AuthState.locationUserId: String?
    get() {
        return (this as? AuthState.Authenticated)?.account?.publicMetadata?.employeeId
    }
