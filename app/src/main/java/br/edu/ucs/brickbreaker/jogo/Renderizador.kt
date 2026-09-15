package br.edu.ucs.brickbreaker.jogo

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import br.edu.ucs.brickbreaker.dados.Paleta
import br.edu.ucs.brickbreaker.dados.Paletas
import br.edu.ucs.brickbreaker.niveis.Tijolo

/** Desenho de um tijolo, compartilhado entre o jogo e a pre-visualizacao das Configuracoes. */
class PintorDeTijolo(private val densidade: Float) {

    private val preenchimento = Paint(Paint.ANTI_ALIAS_FLAG)
    private val trama = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(70, 0, 0, 0)
        strokeWidth = 1.5f * densidade
    }
    private val contorno = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f * densidade
    }
    private val rect = RectF()

    /**
     * A cor vem da paleta; a resistencia tambem aparece na trama (lisa, diagonal, xadrez, X),
     * para o tijolo continuar legivel em qualquer paleta.
     */
    fun desenhar(canvas: Canvas, esquerda: Float, topo: Float, direita: Float, base: Float, resistencia: Int, paleta: Paleta, alpha: Int = 255) {
        val raio = 3f * densidade
        rect.set(esquerda, topo, direita, base)
        preenchimento.color = paleta.corDe(resistencia)
        preenchimento.alpha = alpha
        canvas.drawRoundRect(rect, raio, raio, preenchimento)

        trama.alpha = alpha * 70 / 255
        val passo = 7f * densidade
        canvas.save()
        canvas.clipRect(rect)
        when (resistencia) {
            2 -> diagonais(canvas, passo)
            3 -> {
                diagonais(canvas, passo)
                var x = esquerda + passo / 2
                while (x < direita) { canvas.drawLine(x, topo, x, base, trama); x += passo }
            }
            Tijolo.INDESTRUTIVEL -> {
                canvas.drawLine(esquerda, topo, direita, base, trama)
                canvas.drawLine(esquerda, base, direita, topo, trama)
            }
        }
        canvas.restore()

        contorno.color = Color.argb(alpha * 90 / 255, 0, 0, 0)
        canvas.drawRoundRect(rect, raio, raio, contorno)
    }

    private fun diagonais(canvas: Canvas, passo: Float) {
        var x = rect.left - rect.height()
        while (x < rect.right) {
            canvas.drawLine(x, rect.bottom, x + rect.height(), rect.top, trama)
            x += passo
        }
    }
}

/** Desenha um quadro do jogo no Canvas do SurfaceView. */
class Renderizador(private val densidade: Float) {

    @Volatile var paleta: Paleta = Paletas.CLASSICO
    @Volatile var esmaecerParede = false

    private val pintor = PintorDeTijolo(densidade)
    private val fundo = Color.rgb(14, 17, 22)
    private val zona = Paint().apply { color = Color.rgb(20, 24, 31) }
    private val linhaPerda = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(90, 239, 83, 80)
        strokeWidth = 1.5f * densidade
        pathEffect = DashPathEffect(floatArrayOf(8f * densidade, 6f * densidade), 0f)
    }
    private val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(110, 200, 210, 220)
        textSize = 12f * densidade
        textAlign = Paint.Align.CENTER
    }
    private val tintaPaddle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(232, 236, 241) }
    private val tintaBola = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val brilhoBola = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 79, 195, 247) }
    private val rect = RectF()

    fun desenhar(canvas: Canvas, motor: Motor) {
        canvas.drawColor(fundo)
        if (!motor.telaConfigurada) return

        // zona de controle e linha de perda
        canvas.drawRect(0f, motor.zonaControleTopo, motor.largura, motor.altura, zona)
        canvas.drawText(
            "arraste aqui para mover o paddle",
            motor.largura / 2f, motor.zonaControleTopo + (motor.altura - motor.zonaControleTopo) / 2f, texto,
        )
        canvas.drawLine(0f, motor.linhaDePerda, motor.largura, motor.linhaDePerda, linhaPerda)

        if (motor.paredeMontada) {
            val alpha = if (esmaecerParede) 90 else 255
            val paletaAtual = paleta
            for (t in motor.parede.tijolos) {
                if (!t.vivo) continue
                val c = t.caixa
                pintor.desenhar(canvas, c.esquerda, c.topo, c.direita, c.base, t.resistencia, paletaAtual, alpha)
            }
        }

        val p = motor.paddle.caixa
        rect.set(p.esquerda, p.topo, p.direita, p.base)
        canvas.drawRoundRect(rect, p.altura / 2f, p.altura / 2f, tintaPaddle)

        val b = motor.bola
        canvas.drawCircle(b.x, b.y, b.raio * 2.2f, brilhoBola)
        canvas.drawCircle(b.x, b.y, b.raio, tintaBola)
    }
}
