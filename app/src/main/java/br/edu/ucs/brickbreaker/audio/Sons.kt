package br.edu.ucs.brickbreaker.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import br.edu.ucs.brickbreaker.R

/**
 * Os dois sons exigidos pelo requisito (e). SoundPool mantem os efeitos descomprimidos em memoria,
 * entao o disparo e imediato. Carregado uma vez na Splash.
 */
object Sons {

    private var pool: SoundPool? = null
    private var inicioFase = 0
    private var rebatida = 0

    fun carregar(context: Context) {
        if (pool != null) return
        val atributos = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val novo = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(atributos).build()
        val app = context.applicationContext
        inicioFase = novo.load(app, R.raw.inicio_fase, 1)
        rebatida = novo.load(app, R.raw.rebatida, 1)
        pool = novo
    }

    fun tocarInicioDeFase() {
        pool?.play(inicioFase, 1f, 1f, 1, 0, 1f)
    }

    fun tocarRebatida() {
        pool?.play(rebatida, 0.9f, 0.9f, 2, 0, 1f)
    }
}
