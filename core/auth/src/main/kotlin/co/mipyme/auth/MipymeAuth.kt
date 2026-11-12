package co.mipyme.auth

import android.content.Context
import com.clerk.api.Clerk

object MipymeAuth {
    fun initialize(context: Context, publishableKey: String) {
        Clerk.initialize(context.applicationContext, publishableKey)
    }
}
