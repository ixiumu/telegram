
package top.qwq2333.nullgram.utils

import android.app.Application
import android.util.Base64
import com.google.firebase.crashlytics.FirebaseCrashlytics
import org.telegram.messenger.BuildConfig
import org.telegram.messenger.UserConfig
import java.util.Arrays


object AnalyticsUtils {
    private var isInit = false
    private val isEnabled = BuildConfig.APPLICATION_ID != Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT).contentToString()

    @JvmStatic
    fun start(app: Application) {
        Log.d("Analytics: ${Base64.encodeToString(BuildConfig.APPLICATION_ID.toByteArray(), Base64.DEFAULT)}")
        Log.d("Analytics: ${BuildConfig.APPLICATION_ID != Arrays.toString(Base64.decode("dG9wLnF3cTIzMzMubnVsbGdyYW0=", Base64.DEFAULT))}")

        if (isInit && UserConfig.getActivatedAccountsCount() < 1) return // stop analytics if no user login
        try {
            val currentUser = UserConfig.getInstance(UserConfig.selectedAccount)
            Log.d("FirebaseCrashlytics start: set user id: " + currentUser.getClientUserId())
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setUserId(currentUser.getClientUserId().toString())
            crashlytics.setCustomKey("Build Time", BuildConfig.BUILD_TIME)
            for (i in 0 until  UserConfig.MAX_ACCOUNT_COUNT) {
                UserConfig.getInstance(i)?.let {
                    if (!it.isClientActivated) return@let
                    crashlytics.setCustomKey("User $i", it.getClientUserId().toString())
                }
            }
        } catch (ignored: Exception) { }

        if (isEnabled) {
            isInit = true
            return
        }
        isInit = true
    }

    @JvmStatic
    fun setUserId(id: Long) {
        if (isEnabled) return
        Log.d("FirebaseCrashlytics reset: set user id: $id")
        FirebaseCrashlytics.getInstance().setUserId(id.toString())
    }

    @JvmStatic
    fun trackCrashes(thr: Throwable) {
        if (isEnabled) return
        FirebaseCrashlytics.getInstance().recordException(thr)
    }
}
