package br.edu.ucs.brickbreaker.jogo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColisaoTest {

    private val alvo = Caixa(100f, 100f, 140f, 118f)

    @Test
    fun faceAtingidaDefineOEixoDaReflexao() {
        // vindo de baixo, subindo: face inferior -> inverte Y
        val deBaixo = Colisao.tempoDeImpacto(Caixa(110f, 130f, 124f, 144f), 0f, -100f, alvo, 1f)
        assertEquals(Eixo.Y, deBaixo!!.eixo)
        assertEquals(0.12f, deBaixo.tempo, 1e-4f)

        // vindo da esquerda, andando para a direita: face lateral -> inverte X
        val daEsquerda = Colisao.tempoDeImpacto(Caixa(70f, 102f, 84f, 116f), 100f, 0f, alvo, 1f)
        assertEquals(Eixo.X, daEsquerda!!.eixo)
        assertEquals(0.16f, daEsquerda.tempo, 1e-4f)
    }

    @Test
    fun semImpactoQuandoAfastaOuPassaAoLado() {
        assertNull(Colisao.tempoDeImpacto(Caixa(110f, 130f, 124f, 144f), 0f, 100f, alvo, 1f))   // se afastando
        assertNull(Colisao.tempoDeImpacto(Caixa(200f, 130f, 214f, 144f), 0f, -100f, alvo, 1f))  // passa ao lado
        assertNull(Colisao.tempoDeImpacto(Caixa(110f, 130f, 124f, 144f), 0f, -100f, alvo, 0.05f)) // nao chega no dt
    }

    @Test
    fun deteccaoPorVarreduraNaoDependeDaPosicaoFinal() {
        // em um unico passo a bola iria de antes a depois do tijolo: um teste de sobreposicao nao veria nada
        val impacto = Colisao.tempoDeImpacto(Caixa(110f, 300f, 124f, 314f), 0f, -2000f, alvo, 0.5f)
        assertNotNull(impacto)
        assertTrue(impacto!!.tempo < 0.5f)
    }
}
