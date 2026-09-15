package br.edu.ucs.brickbreaker.jogo

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

/** Superficie de desenho do jogo. Cria o laco quando a superficie existe e o encerra quando ela e destruida. */
class JogoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : SurfaceView(context, attrs), SurfaceHolder.Callback {

    val densidade = resources.displayMetrics.density
    val motor = Motor(densidade)
    val renderizador = Renderizador(densidade)

    /** Chamado na thread de interface quando a superficie ganha tamanho. */
    var aoMedir: ((largura: Int, altura: Int) -> Unit)? = null

    private var laco: LacoDeJogo? = null

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        laco = LacoDeJogo(holder, motor, renderizador).also { it.start() }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        aoMedir?.invoke(width, height)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        laco?.let {
            it.rodando = false
            try { it.join(500) } catch (_: InterruptedException) { }
        }
        laco = null
    }

    /** O paddle so responde a toques na zona de controle (WF-05, anotacao 5). */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> synchronized(motor) {
                if (event.y >= motor.zonaControleTopo) {
                    motor.pilotoAutomatico = false
                    motor.moverPaddlePara(event.x)
                }
            }
        }
        return true
    }
}
