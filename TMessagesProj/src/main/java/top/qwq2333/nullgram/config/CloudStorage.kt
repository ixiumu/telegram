
@file:Suppress("EnumEntryName")

package top.qwq2333.nullgram.config

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.telegram.messenger.AccountInstance
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import top.qwq2333.nullgram.utils.Log
import java.util.concurrent.CompletableFuture

class CloudStorage(instance: Int) : AccountInstance(instance) {
    private var botUser: TLRPC.User? = null

    @Serializable
    data class KeyPair(val key: String, val value: String? = null)

    private enum class Method(val method: String) {
        get("getStorageValues"),
        set("saveStorageValue"),
        delete("deleteStorageValues"),
        getKeys("getStorageKeys")
    }

    @Throws(IllegalStateException::class)
    private fun invokeStorageMethod(method: Method, params: KeyPair): CompletableFuture<KeyPair?> {
        if (botUser == null) {
            throw IllegalStateException("For some reason, unable to get bot user")
        }
        val req = TLRPC.TL_bots_invokeWebViewCustomMethod().apply {
            bot = messagesController.getInputUser(botUser)
            custom_method = method.method
            this.params = TLRPC.TL_dataJSON().apply {
                data = if (method == Method.getKeys) {
                    "{}"
                } else {
                    Json.encodeToString(KeyPair.serializer(), params)
                }
            }
        }

        return CoroutineScope(Dispatchers.IO).future {
            connectionsHelper.sendRequestAndDo(req) { response, err ->
                if (err != null) {
                    throw IllegalStateException("Error while invoking storage method: ${err.text}")
                }
                if (response is TLRPC.TL_dataJSON) {
                    Log.d("CloudStorage", "invokeStorageMethod: data: ${response.data}")
                    return@sendRequestAndDo Json.decodeFromString(KeyPair.serializer(), response.data)
                } else null
            }
        }
    }

    fun get(key: String): CompletableFuture<KeyPair?> {
        return invokeStorageMethod(Method.get, KeyPair(key))
    }

    fun set(key: String, value: String) {
        invokeStorageMethod(Method.set, KeyPair(key, value))
    }

    fun delete(key: String) {
        invokeStorageMethod(Method.delete, KeyPair(key))
    }

    companion object {
        private val Instance by lazy {
            Array(UserConfig.MAX_ACCOUNT_COUNT) {
                CloudStorage(it).apply {
                    messageUtils.searchUser("gao_cai_sheng_2_bot") {
                        botUser = it
                    }
                }
            }
        }

        @JvmStatic
        fun getInstance(num: Int): CloudStorage {
            return Instance[num]
        }
    }

}
