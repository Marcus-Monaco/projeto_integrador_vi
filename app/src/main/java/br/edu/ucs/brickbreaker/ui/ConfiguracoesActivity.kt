package br.edu.ucs.brickbreaker.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import br.edu.ucs.brickbreaker.R
import br.edu.ucs.brickbreaker.dados.Configuracao
import br.edu.ucs.brickbreaker.dados.Paleta
import br.edu.ucs.brickbreaker.dados.Paletas
import br.edu.ucs.brickbreaker.dados.Preferencias
import br.edu.ucs.brickbreaker.jogo.PintorDeTijolo
import br.edu.ucs.brickbreaker.niveis.GeradorDeParede
import br.edu.ucs.brickbreaker.niveis.Tijolo
import kotlinx.coroutines.launch

/** WF-04: opcao 3 do menu, padrao de cores e tamanho dos tijolos usados na geracao da parede. */
class ConfiguracoesActivity : TelaCheiaActivity() {

    private lateinit var preferencias: Preferencias
    private var config = Configuracao()

    private lateinit var grupoPaleta: RadioGroup
    private lateinit var grupoTamanho: RadioGroup
    private lateinit var sliderAltura: SeekBar
    private lateinit var valorAltura: TextView
    private lateinit var amostras: List<View>
    private lateinit var previa: PreviaParedeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes)
        respeitarRecorte(findViewById(R.id.raiz))
        preferencias = Preferencias(this)
        findViewById<TextView>(R.id.tituloBarra).setText(R.string.configuracoes)

        grupoPaleta = findViewById(R.id.grupoPaleta)
        grupoTamanho = findViewById(R.id.grupoTamanho)
        sliderAltura = findViewById(R.id.sliderAltura)
        valorAltura = findViewById(R.id.valorAltura)
        previa = findViewById(R.id.previa)
        amostras = listOf(R.id.amostra1, R.id.amostra2, R.id.amostra3, R.id.amostraX).map { findViewById(it) }

        findViewById<View>(R.id.botaoVoltarTopo).setOnClickListener { finish() }
        findViewById<View>(R.id.editarCores).setOnClickListener { editarCor(0) }
        amostras.forEachIndexed { indice, amostra -> amostra.setOnClickListener { editarCor(indice) } }

        findViewById<Button>(R.id.botaoRestaurar).setOnClickListener {
            config = Configuracao(coresLivres = config.coresLivres)
            mostrar()
        }
        findViewById<Button>(R.id.botaoSalvar).setOnClickListener {
            lifecycleScope.launch {
                preferencias.salvar(config)
                finish()
            }
        }

        lifecycleScope.launch {
            config = preferencias.ler()
            mostrar()
            ligarControles()
        }
    }

    private fun ligarControles() {
        grupoPaleta.setOnCheckedChangeListener { _, id ->
            val paleta = when (id) {
                R.id.paletaNeon -> Paletas.NEON.id
                R.id.paletaMono -> Paletas.MONO.id
                R.id.paletaLivre -> Paletas.LIVRE
                else -> Paletas.CLASSICO.id
            }
            config = config.copy(paleta = paleta)
            atualizarPrevia()
        }
        grupoTamanho.setOnCheckedChangeListener { _, id ->
            val colunas = when (id) {
                R.id.tamanhoPequeno -> 10
                R.id.tamanhoGrande -> 6
                else -> 8
            }
            config = config.copy(colunas = colunas)
            atualizarPrevia()
        }
        sliderAltura.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                config = config.copy(alturaTijolo = ALTURA_MINIMA + progress)
                atualizarPrevia()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
        })
    }

    /** Coloca os controles no estado de [config]. */
    private fun mostrar() {
        grupoPaleta.check(
            when (config.paleta) {
                Paletas.NEON.id -> R.id.paletaNeon
                Paletas.MONO.id -> R.id.paletaMono
                Paletas.LIVRE -> R.id.paletaLivre
                else -> R.id.paletaClassico
            }
        )
        grupoTamanho.check(
            when (config.colunas) {
                10 -> R.id.tamanhoPequeno
                6 -> R.id.tamanhoGrande
                else -> R.id.tamanhoMedio
            }
        )
        sliderAltura.max = ALTURA_MAXIMA - ALTURA_MINIMA
        sliderAltura.progress = config.alturaTijolo - ALTURA_MINIMA
        atualizarPrevia()
    }

    private fun atualizarPrevia() {
        val paleta = config.paletaAtual
        amostras.forEachIndexed { i, amostra ->
            (amostra.background.mutate() as? GradientDrawable)?.setColor(paleta.cores[i])
        }
        valorAltura.text = getString(R.string.valor_dp, config.alturaTijolo)
        previa.configurar(paleta, config.colunas, config.alturaTijolo)
    }

    /** Modo "Livre": seletor HSV para a cor de uma resistencia. */
    private fun editarCor(indice: Int) {
        val coresBase = config.paletaAtual.cores.toMutableList()
        val hsv = FloatArray(3).also { Color.colorToHSV(coresBase[indice], it) }
        val rotulos = resources.getStringArray(R.array.resistencias)
        val dp = resources.displayMetrics.density

        val amostra = View(this).apply {
            background = GradientDrawable().apply { cornerRadius = 6 * dp; setColor(coresBase[indice]) }
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (40 * dp).toInt())
        }
        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0)
            addView(amostra)
        }
        fun atualizar() {
            coresBase[indice] = Color.HSVToColor(hsv)
            (amostra.background as GradientDrawable).setColor(coresBase[indice])
        }
        listOf(getString(R.string.matiz) to 360, getString(R.string.saturacao) to 100, getString(R.string.brilho) to 100)
            .forEachIndexed { canal, (nome, maximo) ->
                conteudo.addView(TextView(this).apply { text = nome; setPadding(0, (12 * dp).toInt(), 0, 0) })
                conteudo.addView(SeekBar(this).apply {
                    max = maximo
                    progress = if (canal == 0) hsv[0].toInt() else (hsv[canal] * 100).toInt()
                    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(s: SeekBar, p: Int, fromUser: Boolean) {
                            hsv[canal] = if (canal == 0) p.toFloat() else p / 100f
                            atualizar()
                        }
                        override fun onStartTrackingTouch(s: SeekBar) = Unit
                        override fun onStopTrackingTouch(s: SeekBar) = Unit
                    })
                })
            }

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.cor_da_resistencia, rotulos[indice]))
            .setView(conteudo)
            .setPositiveButton(R.string.aplicar) { _, _ ->
                config = config.copy(paleta = Paletas.LIVRE, coresLivres = coresBase)
                grupoPaleta.check(R.id.paletaLivre)
                atualizarPrevia()
            }
            .setNeutralButton(R.string.proxima_cor) { _, _ ->
                config = config.copy(paleta = Paletas.LIVRE, coresLivres = coresBase)
                grupoPaleta.check(R.id.paletaLivre)
                atualizarPrevia()
                editarCor((indice + 1) % 4)
            }
            .setNegativeButton(R.string.cancelar, null)
            .show()
    }

    private companion object {
        const val ALTURA_MINIMA = 12
        const val ALTURA_MAXIMA = 26
    }
}

/** Pre-visualizacao viva: um recorte real da parede do nivel 5 (que tem os quatro tipos de tijolo) com as opcoes atuais. */
class PreviaParedeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private val densidade = resources.displayMetrics.density
    private val pintor = PintorDeTijolo(densidade)
    private var paleta: Paleta = Paletas.CLASSICO
    private var colunas = 8
    private var alturaTijoloDp = 18

    fun configurar(paleta: Paleta, colunas: Int, alturaTijoloDp: Int) {
        this.paleta = paleta
        this.colunas = colunas
        this.alturaTijoloDp = alturaTijoloDp
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val espaco = 3 * densidade
        val margem = 8 * densidade
        val alturaTijolo = alturaTijoloDp * densidade
        val larguraTijolo = (width - 2 * margem - espaco * (colunas - 1)) / colunas
        val matriz = GeradorDeParede.matrizDoNivel(5, colunas)
        val cabem = GeradorDeParede.linhasQueCabem(height - 2 * margem, alturaTijolo, espaco)

        matriz.take(cabem).forEachIndexed { i, linha ->
            linha.forEachIndexed { j, c ->
                val resistencia = Tijolo.resistenciaDe(c) ?: return@forEachIndexed
                val x = margem + j * (larguraTijolo + espaco)
                val y = margem + i * (alturaTijolo + espaco)
                pintor.desenhar(canvas, x, y, x + larguraTijolo, y + alturaTijolo, resistencia, paleta)
            }
        }
    }
}
