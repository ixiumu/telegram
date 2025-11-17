

package top.qwq2333.nullgram.utils

import android.content.Context
import android.widget.Toast
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.LocaleController
import org.telegram.messenger.R
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.AlertDialog
import top.qwq2333.nullgram.ui.BottomBuilder


object AlertUtil {

    @JvmStatic
    fun showToast(e: Throwable) = showToast(e.message ?: e.javaClass.simpleName)

    @JvmStatic
    fun showToast(e: TLRPC.TL_error?) {
        if (e == null) return
        showToast("${e.code}: ${e.text}")
    }

    @JvmStatic
    fun showToast(text: String) = ApplicationLoader.applicationHandler.post {
        Toast.makeText(
            ApplicationLoader.applicationContext,
            text.takeIf { it.isNotBlank() }
                ?: "Rua !",
            Toast.LENGTH_LONG
        ).show()
    }

    @JvmStatic
    fun showSimpleAlert(ctx: Context?, error: Throwable) {

        showSimpleAlert(ctx, null, error.message ?: error.javaClass.simpleName)

    }

    @JvmStatic
    @JvmOverloads
    fun showSimpleAlert(
        ctx: Context?,
        text: String,
        listener: ((AlertDialog.Builder) -> Unit)? = null
    ) {

        showSimpleAlert(ctx, null, text, listener)

    }

    @JvmStatic
    @JvmOverloads
    fun showSimpleAlert(
        ctx: Context?,
        title: String?,
        text: String,
        listener: ((AlertDialog.Builder) -> Unit)? = null
    ) = ApplicationLoader.applicationHandler.post {

        if (ctx == null) return@post

        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(title ?: LocaleController.getString("AppName", R.string.AppName))
        builder.setMessage(text)

        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK)) { _, _ ->

            builder.dismissRunnable?.run()
            listener?.invoke(builder)

        }

        builder.show()

    }

    @JvmStatic
    @JvmOverloads
    fun showConfirm(
        ctx: Context,
        title: String,
        text: String? = null,
        icon: Int,
        button: String,
        red: Boolean,
        listener: Runnable
    ) = ApplicationLoader.applicationHandler.post {
        val builder = BottomBuilder(ctx)

        if (text != null) {
            builder.addTitle(title, text)
        } else {
            builder.addTitle(title)
        }

        builder.addItem(button, icon, red) {
            listener.run()
        }
        builder.addCancelItem()
        builder.show()

    }


}
