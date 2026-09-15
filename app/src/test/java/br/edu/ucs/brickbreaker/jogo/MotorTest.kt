package br.edu.ucs.brickbreaker.jogo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MotorTest {

    private lateinit var motor: Motor
    private var rebatidas = 0
    private var perdas = 0

    @Before
    fun preparar() {
        motor = Motor(densidade = 2f)
        motor.configurarTela(largura = 720f, altura = 1600f, topoSeguro = 0f)
        motor.ouvinte = object : Motor.Ouvinte {
            override fun aoRebaterNoPaddle() { rebatidas++ }
            override fun aoAtingirTijolo(destruido: Boolean) = Unit
            override fun aoPerderBola() { perdas++ }
            override fun aoConcluirNivel() = Unit
        }
        motor.iniciarNivel(1, Motor.Ajustes())
    }

    /** Requisito (d): mesmo a 10x a velocidade normal a bola para no primeiro tijolo do caminho. */
    @Test
    fun bolaRapidaNaoAtravessaAParede() {
        motor.lancar()
        val v = motor.velocidadeDoNivel() * 10f
        motor.bola.x = motor.parede.tijolos.first { it.coluna == 3 }.caixa.centroX
        motor.bola.y = motor.paddle.topo - 40f
        motor.bola.vx = 0f
        motor.bola.vy = -v

        val antes = motor.parede.restantes
        repeat(10) { motor.atualizar(0.016f) }

        val destruidos = antes - motor.parede.restantes
        assertTrue("destruiu $destruidos tijolos", destruidos in 1..2)
        // a fileira de baixo da coluna foi atingida; as de cima continuam de pe
        val coluna = motor.parede.tijolos.filter { it.coluna == 3 }
        assertTrue(coluna.first { it.linha == 0 }.vivo)
    }

    @Test
    fun umSubpassoDestroiNoMaximoDoisTijolos() {
        motor.lancar()
        // bola encaixada entre dois tijolos, andando na diagonal
        val alvo = motor.parede.tijolos.first { it.linha == 4 && it.coluna == 2 }
        motor.bola.x = alvo.caixa.direita + 1f
        motor.bola.y = alvo.caixa.base + motor.bola.raio + 1f
        motor.bola.vx = -3000f
        motor.bola.vy = -3000f

        val antes = motor.parede.restantes
        val colisoes = motor.avancarUmSubpasso(0.05f)

        assertTrue(colisoes <= Motor.MAX_COLISOES_POR_SUBPASSO)
        assertTrue(antes - motor.parede.restantes <= Motor.MAX_COLISOES_POR_SUBPASSO)
    }

    @Test
    fun rebatidaNoPaddleDependeDoPontoDeContato() {
        motor.lancar()
        motor.bola.x = motor.paddle.x + motor.paddle.largura * 0.45f   // quase na ponta direita
        motor.bola.y = motor.paddle.topo - motor.bola.raio - 20f
        motor.bola.vx = 0f
        motor.bola.vy = 600f

        repeat(5) { motor.atualizar(0.016f) }

        assertEquals(1, rebatidas)
        assertTrue(motor.bola.vy < 0f)
        assertTrue("sai para a direita", motor.bola.vx > 0f)
    }

    @Test
    fun bolaQuePassaDoPaddleEPerdida() {
        motor.lancar()
        motor.bola.x = 20f
        motor.bola.y = motor.paddle.topo
        motor.bola.vx = 0f
        motor.bola.vy = 800f
        motor.paddle.x = 600f
        motor.paddle.alvoX = 600f

        repeat(30) { motor.atualizar(0.016f) }

        assertEquals(1, perdas)
        assertEquals(Motor.Estado.BOLA_PERDIDA, motor.estado)
        assertEquals(Motor.VIDAS_INICIAIS - 1, motor.vidas)
    }

    @Test
    fun velocidadeCresceOitoPorCentoPorNivelComTeto() {
        assertEquals(motor.velocidadeDoNivel(1) * 1.08f, motor.velocidadeDoNivel(2), 0.01f)
        assertTrue(motor.velocidadeDoNivel(50) <= motor.dp(Motor.VELOCIDADE_MAXIMA_DP))
    }
}
