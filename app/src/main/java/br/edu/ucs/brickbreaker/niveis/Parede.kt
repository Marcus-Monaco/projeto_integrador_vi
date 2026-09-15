package br.edu.ucs.brickbreaker.niveis

import br.edu.ucs.brickbreaker.jogo.Caixa
import kotlin.math.max
import kotlin.math.min

/** Posicao da malha na tela, em pixels (doc 03, secao 2). */
data class GeometriaParede(
    val margemLateral: Float,
    val topo: Float,
    val larguraTijolo: Float,
    val alturaTijolo: Float,
    val espaco: Float,
) {
    fun x(coluna: Int) = margemLateral + coluna * (larguraTijolo + espaco)
    fun y(linha: Int) = topo + linha * (alturaTijolo + espaco)

    companion object {
        /** A largura do tijolo e sempre calculada, nunca fixada. */
        fun calcular(
            larguraTela: Float,
            margemLateral: Float,
            topo: Float,
            colunas: Int,
            alturaTijolo: Float,
            espaco: Float,
        ): GeometriaParede {
            val larguraUtil = larguraTela - 2 * margemLateral
            val larguraTijolo = (larguraUtil - espaco * (colunas - 1)) / colunas
            return GeometriaParede(margemLateral, topo, larguraTijolo, alturaTijolo, espaco)
        }
    }
}

/** A parede como malha regular: a matriz vira tijolos posicionados uma unica vez, na montagem. */
class Parede(val matriz: List<String>, val geometria: GeometriaParede) {

    val linhas = matriz.size
    val colunas = matriz.firstOrNull()?.length ?: 0

    private val grade = arrayOfNulls<Tijolo>(linhas * colunas)
    val tijolos: List<Tijolo>

    init {
        val lista = ArrayList<Tijolo>()
        matriz.forEachIndexed { i, linha ->
            linha.forEachIndexed { j, caractere ->
                val resistencia = Tijolo.resistenciaDe(caractere) ?: return@forEachIndexed
                val x = geometria.x(j)
                val y = geometria.y(i)
                val tijolo = Tijolo(i, j, resistencia, Caixa(x, y, x + geometria.larguraTijolo, y + geometria.alturaTijolo))
                grade[i * colunas + j] = tijolo
                lista += tijolo
            }
        }
        tijolos = lista
    }

    /** Tijolos destrutiveis ainda de pe. Os indestrutiveis nao contam. */
    val restantes get() = tijolos.count { it.destrutivel && it.vivo }
    val concluida get() = restantes == 0
    val totalDestrutiveis = tijolos.count { it.destrutivel }

    /**
     * Candidatos a colisao dentro de uma regiao (doc 03, secao 7.4).
     * Como a parede e uma malha, as celulas cruzadas saem de uma conta direta, sem percorrer todos os tijolos.
     */
    fun candidatos(regiao: Caixa): List<Tijolo> {
        if (linhas == 0) return emptyList()
        val passoX = geometria.larguraTijolo + geometria.espaco
        val passoY = geometria.alturaTijolo + geometria.espaco
        val colInicial = max(0, ((regiao.esquerda - geometria.margemLateral) / passoX).toInt() - 1)
        val colFinal = min(colunas - 1, ((regiao.direita - geometria.margemLateral) / passoX).toInt() + 1)
        val linInicial = max(0, ((regiao.topo - geometria.topo) / passoY).toInt() - 1)
        val linFinal = min(linhas - 1, ((regiao.base - geometria.topo) / passoY).toInt() + 1)
        if (colInicial > colFinal || linInicial > linFinal) return emptyList()

        val resultado = ArrayList<Tijolo>(8)
        for (i in linInicial..linFinal) {
            for (j in colInicial..colFinal) {
                val tijolo = grade[i * colunas + j] ?: continue
                if (tijolo.vivo) resultado += tijolo
            }
        }
        return resultado
    }
}
