package co.mipyme.startup

import android.content.Context
import androidx.startup.Initializer
import co.mipyme.R
import co.mipyme.auth.MipymeAuth

class AuthInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        MipymeAuth.initialize(context, context.getString(R.string.clerk_publishable_key))
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> {
        return listOf(TimberInitializer::class.java)
    }
}
