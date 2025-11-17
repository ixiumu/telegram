

package top.qwq2333.nullgram.helpers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLObject
import org.telegram.tgnet.TLRPC
import java.util.concurrent.CountDownLatch

class ConnectionsHelper(instance: Int) : AccountInstance(instance) {
    companion object {
        private val Instance by lazy {
            Array(UserConfig.MAX_ACCOUNT_COUNT) {
                ConnectionsHelper(it)
            }
        }

        @JvmStatic
        fun getInstance(num: Int): ConnectionsHelper {
            return Instance[num]
        }
    }

    suspend fun <T> sendRequestAndDo(req: TLObject, flags: Int = 0, action: (TLObject?, TLRPC.TL_error?) -> T?): T? {
        lateinit var result: Pair<TLObject?, TLRPC.TL_error?>
        val latch = CountDownLatch(1)
        return withContext(Dispatchers.IO) {
            connectionsManager.sendRequest(req, { response: TLObject?, error: TLRPC.TL_error? ->
                result = Pair(response, error)
                latch.countDown()
            }, flags)
            latch.await()
            action(result.first, result.second)
        }
    }
}
