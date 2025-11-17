

package top.qwq2333.nullgram.utils

import android.media.AudioRecord
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import top.qwq2333.gen.Config

object AudioUtils {
    private var automaticGainControl: AutomaticGainControl? = null
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    private var noiseSuppressor: NoiseSuppressor? = null

    @JvmStatic
    fun initVoiceEnhance(
        audioRecord: AudioRecord
    ) {
        if (!Config.enchantAudio) return
        if (AutomaticGainControl.isAvailable()) {
            automaticGainControl = AutomaticGainControl.create(
                audioRecord.audioSessionId
            )
            automaticGainControl?.enabled = true
        }
        if (AcousticEchoCanceler.isAvailable()) {
            acousticEchoCanceler = AcousticEchoCanceler.create(
                audioRecord.audioSessionId
            )
            acousticEchoCanceler?.enabled = true
        }
        if (NoiseSuppressor.isAvailable()) {
            noiseSuppressor = NoiseSuppressor.create(
                audioRecord.audioSessionId
            )
            noiseSuppressor?.enabled = true
        }
    }

    @JvmStatic
    fun releaseVoiceEnhance() {
        automaticGainControl?.release()
        automaticGainControl = null
        acousticEchoCanceler?.release()
        acousticEchoCanceler = null
        noiseSuppressor?.release()
        noiseSuppressor = null
    }

    @JvmStatic
    fun isAvailable(): Boolean {
        return AutomaticGainControl.isAvailable() || NoiseSuppressor.isAvailable() || AcousticEchoCanceler.isAvailable()
    }
}
