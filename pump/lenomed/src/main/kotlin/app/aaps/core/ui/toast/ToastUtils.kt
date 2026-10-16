package app.aaps.core.ui.toast

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

/**
 * Compatibility shim for the ported Lenomed (Embecta) driver.
 *
 * Mirrors the Kotlin `object ToastUtils` expected by the JADX-decompiled call
 * sites (which reference `ToastUtils.INSTANCE` and the synthetic
 * `okToast$default` bridge for the default argument).
 */
object ToastUtils {
    fun errorToast(context: Context, @StringRes message: Int) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun errorToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun okToast(context: Context, message: String, silent: Boolean = false) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun infoToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
