package br.edu.ucs.brickbreaker.niveis

import kotlin.math.roundToInt

/**
 * Os cinco metodos de construcao da parede (doc 03, secao 4).
 *
 * Todos devolvem a parede como matriz de caracteres:
 * `.` vazio, `1`/`2`/`3` resistencia, `X` indestrutivel.
 */
object GeradorDeParede {

    const val TOTAL_DE_NIVEIS = 5
    const val COLUNAS_CANONICAS = 8

    data class InfoNivel(val numero: Int, val nome: String, val metodo: String)

    val NIVEIS = listOf(
        InfoNivel(1, "Muralha", "Mapa fixo literal"),
        InfoNivel(2, "Pirâmide", "Meia matriz + espelhamento"),
        InfoNivel(3, "Xadrez", "Regra matemática sobre (linha, coluna)"),
        InfoNivel(4, "Campo minado", "Procedural com semente fixa"),
        InfoNivel(5, "Fortaleza", "Híbrido: moldura fixa + miolo procedural"),
    )

    // --- Nivel 1: mapa fixo literal -------------------------------------------------------------

    val NIVEL_1 = listOf(
        "11111111",
        "11111111",
        "11111111",
        "11111111",
        "11111111",
    )

    // --- Nivel 2: meia matriz + espelhamento ----------------------------------------------------

    fun espelhar(metade: List<String>): List<String> =
        metade.map { it + it.reversed() }

    val METADE_NIVEL_2 = listOf(
        "...1",
        "..11",
        ".222",
        "2222",
        "3333",
    )

    // --- Nivel 3: regra matematica sobre (linha, coluna) ----------------------------------------

    fun porRegra(linhas: Int, colunas: Int, regra: (Int, Int) -> Char): List<String> =
        (0 until linhas).map { i ->
            (0 until colunas).map { j -> regra(i, j) }.joinToString("")
        }

    fun regraXadrez(i: Int, j: Int): Char =
        if ((i + j) % 2 == 0) when {
            i < 2 -> '1'
            i < 4 -> '2'
            else -> '3'
        } else '.'

    // --- Nivel 4: procedural com semente fixa ---------------------------------------------------

    const val SEMENTE_NIVEL_4 = 20260821
    val PESOS_NIVEL_4 = listOf('1' to 0.50, '2' to 0.32, '3' to 0.18)
    private const val MAX_TENTATIVAS = 50

    fun procedural(
        linhas: Int,
        colunas: Int,
        semente: Int,
        densidade: Double,
        pesos: List<Pair<Char, Double>>,
    ): List<String> {
        val rnd = Aleatorio(semente)
        repeat(MAX_TENTATIVAS) {
            val matriz = (0 until linhas).map {
                (0 until colunas).map {
                    if (rnd.proximo() < densidade) sorteiaResistencia(rnd, pesos) else '.'
                }.joinToString("")
            }
            if (valida(matriz)) return matriz
        }
        // caso extremo: nenhuma tentativa passou na validacao
        return List(linhas) { "1".repeat(colunas) }
    }

    private fun sorteiaResistencia(rnd: Aleatorio, pesos: List<Pair<Char, Double>>): Char {
        val r = rnd.proximo()
        var acumulado = 0.0
        for ((caractere, peso) in pesos) {
            acumulado += peso
            if (r <= acumulado) return caractere
        }
        return pesos.last().first
    }

    /** Massa minima de tijolos destrutiveis e nenhuma fileira vazia (doc 03, secao 4.4). */
    fun valida(matriz: List<String>): Boolean {
        val destrutiveis = matriz.sumOf { linha -> linha.count { it in "123" } }
        val temLinhaVazia = matriz.any { linha -> linha.none { it in "123X" } }
        val minimo = (matriz.size * matriz[0].length * 0.45).toInt()
        return destrutiveis >= minimo && !temLinhaVazia
    }

    // --- Nivel 5: hibrido ------------------------------------------------------------------------

    const val SEMENTE_NIVEL_5 = 5150

    fun hibrido(linhas: Int, colunas: Int, semente: Int): List<String> {
        val rnd = Aleatorio(semente)
        return (0 until linhas).map { i ->
            (0 until colunas).map { j ->
                when {
                    i == 0 -> '3'                                   // teto blindado
                    i == linhas - 1 -> '2'                          // base reforcada
                    j == 0 || j == colunas - 1 -> 'X'               // colunas indestrutiveis
                    rnd.proximo() < 0.72 ->
                        if (rnd.proximo() < 0.35) '2' else '1'      // miolo sorteado
                    else -> '.'
                }
            }.joinToString("")
        }
    }

    // --- Adaptacao ao tamanho escolhido (doc 03, secao 5) ----------------------------------------

    /** Reamostragem horizontal por vizinho mais proximo: preserva silhueta e simetria. */
    fun reamostrar(matriz: List<String>, colunasDestino: Int): List<String> {
        val colunasOrigem = matriz[0].length
        if (colunasOrigem == colunasDestino) return matriz
        return matriz.map { linha ->
            (0 until colunasDestino).map { j ->
                val origem = ((j * (colunasOrigem - 1).toDouble()) / (colunasDestino - 1))
                    .roundToInt().coerceAtMost(colunasOrigem - 1)
                linha[origem]
            }.joinToString("")
        }
    }

    /** Matriz do nivel ja no numero de colunas pedido. Mapas fixos sao reamostrados; os gerados por funcao nascem no tamanho certo. */
    fun matrizDoNivel(numero: Int, colunas: Int = COLUNAS_CANONICAS): List<String> = when (numero) {
        1 -> reamostrar(NIVEL_1, colunas)
        2 -> reamostrar(espelhar(METADE_NIVEL_2), colunas)
        3 -> porRegra(6, colunas, ::regraXadrez)
        4 -> procedural(6, colunas, SEMENTE_NIVEL_4, 0.70, PESOS_NIVEL_4)
        5 -> hibrido(6, colunas, SEMENTE_NIVEL_5)
        else -> throw IllegalArgumentException("nivel inexistente: $numero")
    }

    /** Quantas fileiras cabem na area da parede (doc 03, secao 2). */
    fun linhasQueCabem(alturaArea: Float, alturaTijolo: Float, espaco: Float): Int =
        ((alturaArea + espaco) / (alturaTijolo + espaco)).toInt().coerceAtLeast(1)
}
