package br.edu.ucs.brickbreaker.jogo

import android.graphics.Canvas
import android.os.Build
import android.view.SurfaceHolder

/**
 * Thread do laco de jogo: passo fixo de 16 ms com acumulador de tempo.
 * Atualiza a fisica quantas vezes forem necessarias e desenha um quadro.
 */
class LacoDeJogo(
    private val holder: SurfaceHolder,
    private val motor: Motor,
    private val renderizador: Renderizador,
) : Thread("LacoDeJogo") {

    @Volatile var rodando = true

    override fun run() {
        var anterior = System.nanoTime()
        var acumulado = 0f

        while (rodando) {
            val inicio = System.nanoTime()
            // limita o salto apos uma pausa longa (ex.: app em segundo plano)
            acumulado += ((inicio - anterior) / 1_000_000_000f).coerceAtMost(0.25f)
            anterior = inicio

            synchronized(motor) {
                while (acumulado >= PASSO) {
                    motor.atualizar(PASSO)
                    acumulado -= PASSO
                }
            }

            val canvas = try { travarCanvas() } catch (e: IllegalStateException) { null }
            if (canvas != null) {
                try {
                    synchronized(motor) { renderizador.desenhar(canvas, motor) }
                } finally {
                    try { holder.unlockCanvasAndPost(canvas) } catch (_: IllegalStateException) { }
                }
            }

            val gastoMs = (System.nanoTime() - inicio) / 1_000_000L
            val folga = PASSO_MS - gastoMs
            if (folga > 0) sleep(folga)
        }
    }

    /** A partir do Android 8 o Canvas do SurfaceView pode ser acelerado por GPU. */
    private fun travarCanvas(): Canvas? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) holder.lockHardwareCanvas() else holder.lockCanvas()

    companion object {
        const val PASSO = 0.016f
        private const val PASSO_MS = 16L
    }
}
