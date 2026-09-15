package br.edu.ucs.brickbreaker.niveis

import br.edu.ucs.brickbreaker.jogo.Caixa
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeradorDeParedeTest {

    @Test
    fun paredeDoNivelTemAsDimensoesEsperadas() {
        val esperado = mapOf(1 to (5 to 8), 2 to (5 to 8), 3 to (6 to 8), 4 to (6 to 8), 5 to (6 to 8))
        for ((nivel, dimensoes) in esperado) {
            val matriz = GeradorDeParede.matrizDoNivel(nivel)
            assertEquals("linhas do nivel $nivel", dimensoes.first, matriz.size)
            assertTrue("colunas do nivel $nivel", matriz.all { it.length == dimensoes.second })
        }
    }

    /** As matrizes batem com as documentadas no doc 03 e desenhadas nos wireframes WF-06 a WF-10. */
    @Test
    fun matrizesIguaisAsDaDocumentacao() {
        assertEquals(List(5) { "11111111" }, GeradorDeParede.matrizDoNivel(1))
        assertEquals(
            listOf("...11...", "..1111..", ".222222.", "22222222", "33333333"),
            GeradorDeParede.matrizDoNivel(2),
        )
        assertEquals(
            listOf("1.1.1.1.", ".1.1.1.1", "2.2.2.2.", ".2.2.2.2", "3.3.3.3.", ".3.3.3.3"),
            GeradorDeParede.matrizDoNivel(3),
        )
        assertEquals(
            listOf("..221.1.", ".11.1212", "121..131", "112.2.12", "1.1223.2", ".1.1.23."),
            GeradorDeParede.matrizDoNivel(4),
        )
        assertEquals(
            listOf("33333333", "X111.2.X", "X1.1.12X", "X1.1..1X", "X11112.X", "22222222"),
            GeradorDeParede.matrizDoNivel(5),
        )
    }

    @Test
    fun contagemDeTijolosEAcertosConfereComOResumo() {
        // (destrutiveis, indestrutiveis, acertos) do doc 03, secao 9
        val esperado = mapOf(1 to Triple(40, 0, 40), 2 to Triple(28, 0, 58), 3 to Triple(24, 0, 48), 4 to Triple(32, 0, 50), 5 to Triple(32, 8, 59))
        for ((nivel, contagem) in esperado) {
            val celulas = GeradorDeParede.matrizDoNivel(nivel).joinToString("")
            assertEquals(contagem.first, celulas.count { it in "123" })
            assertEquals(contagem.second, celulas.count { it == 'X' })
            assertEquals(contagem.third, celulas.sumOf { if (it in "123") it.digitToInt() else 0 })
        }
    }

    @Test
    fun geradorProceduralEReprodutivel() {
        val primeira = GeradorDeParede.matrizDoNivel(4)
        repeat(100) { assertEquals(primeira, GeradorDeParede.matrizDoNivel(4)) }
    }

    @Test
    fun paredeGeradaPassaNaValidacao() {
        for (nivel in 1..5) for (colunas in listOf(6, 8, 10)) {
            assertTrue("nivel $nivel com $colunas colunas", GeradorDeParede.valida(GeradorDeParede.matrizDoNivel(nivel, colunas)))
        }
        assertFalse(GeradorDeParede.valida(listOf("11111111", "........")))
    }

    @Test
    fun reamostragemPreservaSimetria() {
        for (colunas in listOf(6, 8, 10)) {
            val matriz = GeradorDeParede.matrizDoNivel(2, colunas)
            assertTrue(matriz.all { it.length == colunas && it == it.reversed() })
        }
        assertEquals(
            listOf("..11..", "..11..", ".2222.", "222222", "333333"),
            GeradorDeParede.matrizDoNivel(2, 6),
        )
    }

    @Test
    fun nivelConcluiIgnorandoIndestrutiveis() {
        val geometria = GeometriaParede(0f, 0f, 10f, 10f, 1f)
        val parede = Parede(GeradorDeParede.matrizDoNivel(5), geometria)
        assertEquals(8, parede.tijolos.count { !it.destrutivel })

        parede.tijolos.filter { it.destrutivel }.forEach { tijolo -> while (tijolo.vivo) tijolo.atingir() }

        assertTrue(parede.concluida)
        assertEquals(8, parede.tijolos.count { it.vivo })
    }

    @Test
    fun candidatosDaMalhaFicamPertoDaRegiao() {
        val geometria = GeometriaParede(16f, 100f, 38f, 18f, 3f)
        val parede = Parede(GeradorDeParede.matrizDoNivel(1), geometria)
        val regiao = Caixa(60f, 120f, 74f, 134f)
        val candidatos = parede.candidatos(regiao)
        assertTrue(candidatos.size in 1..16)
        assertTrue(candidatos.size < parede.tijolos.size)
    }
}
