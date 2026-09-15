package br.edu.ucs.brickbreaker.ui

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import br.edu.ucs.brickbreaker.BuildConfig
import br.edu.ucs.brickbreaker.R
import br.edu.ucs.brickbreaker.audio.Sons
import br.edu.ucs.brickbreaker.dados.Configuracao
import br.edu.ucs.brickbreaker.dados.Preferencias
import br.edu.ucs.brickbreaker.jogo.JogoView
import br.edu.ucs.brickbreaker.jogo.Motor
import br.edu.ucs.brickbreaker.niveis.GeradorDeParede
import kotlinx.coroutines.launch

/**
 * WF-05: hospeda o SurfaceView e o HUD, e conduz o fluxo da partida:
 * transicao de nivel (WF-12), bola perdida (WF-11), pausa (WF-13) e fim de jogo (WF-14).
 */
class JogoActivity : TelaCheiaActivity(), Motor.Ouvinte {

    private lateinit var jogoView: JogoView
    private val motor get() = jogoView.motor

    private lateinit var preferencias: Preferencias
    private var config = Configuracao()
    private var configCarregada = false
    private var partidaIniciada = false
    private var encerrada = false
    private var topoSeguro = 0
    private var niveisConcluidos = 0

    /** So em build de depuracao, para gravacao: o paddle segue a bola ate o primeiro toque do nivel. */
    private var modoDemonstracao = false

    private val principal = Handler(Looper.getMainLooper())
    private var contagem: Runnable? = null

    private lateinit var textoNivel: TextView
    private lateinit var textoPontos: TextView
    private lateinit var textoVidas: TextView
    private lateinit var overlayTransicao: View
    private lateinit var overlayPausa: View
    private lateinit var overlayPerdida: View
    private lateinit var textoRetomar: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jogo)
        preferencias = Preferencias(this)

        jogoView = findViewById(R.id.jogoView)
        textoNivel = findViewById(R.id.hudNivel)
        textoPontos = findViewById(R.id.hudPontos)
        textoVidas = findViewById(R.id.hudVidas)
        overlayTransicao = findViewById(R.id.overlayTransicao)
        overlayPausa = findViewById(R.id.overlayPausa)
        overlayPerdida = findViewById(R.id.overlayPerdida)
        textoRetomar = findViewById(R.id.textoRetomar)

        motor.ouvinte = this
        modoDemonstracao = BuildConfig.DEBUG && intent.getBooleanExtra(EXTRA_DEMO, false)

        respeitarRecorte(findViewById(R.id.hud)) { topo ->
            topoSeguro = topo
            if (motor.telaConfigurada) synchronized(motor) { motor.configurarTela(motor.largura, motor.altura, topo.toFloat()) }
        }
        jogoView.aoMedir = { largura, altura ->
            synchronized(motor) { motor.configurarTela(largura.toFloat(), altura.toFloat(), topoSeguro.toFloat()) }
            tentarIniciarPartida()
        }

        findViewById<View>(R.id.botaoPausa).setOnClickListener { mostrarPausa() }
        overlayTransicao.setOnClickListener { comecarNivel() }

        findViewById<Button>(R.id.botaoContinuar).setOnClickListener { continuar() }
        findViewById<Button>(R.id.botaoReiniciarPausa).setOnClickListener {
            overlayPausa.isVisible = false
            iniciarNivel(motor.nivel)
        }
        findViewById<Button>(R.id.botaoConfiguracoesPausa).setOnClickListener {
            startActivity(Intent(this, ConfiguracoesActivity::class.java))
        }
        findViewById<Button>(R.id.botaoSairPausa).setOnClickListener { finish() }

        findViewById<Button>(R.id.botaoReiniciarNivel).setOnClickListener {
            overlayPerdida.isVisible = false
            iniciarNivel(motor.nivel)
        }
        findViewById<Button>(R.id.botaoProximoNivel).setOnClickListener {
            overlayPerdida.isVisible = false
            if (motor.nivel < GeradorDeParede.TOTAL_DE_NIVEIS) iniciarNivel(motor.nivel + 1) else encerrar(vitoria = false)
        }
        findViewById<Button>(R.id.botaoMenuPerdida).setOnClickListener { finish() }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    overlayPerdida.isVisible -> Unit          // escolha explicita obrigatoria (requisito g)
                    overlayPausa.isVisible -> continuar()
                    else -> mostrarPausa()
                }
            }
        })

        lifecycleScope.launch {
            config = preferencias.ler()
            jogoView.renderizador.paleta = config.paletaAtual
            configCarregada = true
            tentarIniciarPartida()
        }
    }

    override fun onResume() {
        super.onResume()
        // volta das Configuracoes: a cor vale na hora, o tamanho so no proximo nivel
        if (configCarregada) lifecycleScope.launch {
            config = preferencias.ler()
            jogoView.renderizador.paleta = config.paletaAtual
        }
    }

    override fun onPause() {
        super.onPause()
        if (partidaIniciada && !encerrada) mostrarPausa()
    }

    override fun onDestroy() {
        principal.removeCallbacksAndMessages(null)
        motor.ouvinte = null
        super.onDestroy()
    }

    // --- fluxo da partida ----------------------------------------------------------------------

    private fun tentarIniciarPartida() {
        if (partidaIniciada || !configCarregada || !motor.telaConfigurada) return
        partidaIniciada = true
        synchronized(motor) { motor.reiniciarPartida() }
        iniciarNivel(1)
    }

    private fun iniciarNivel(numero: Int) {
        cancelarContagem()
        synchronized(motor) {
            motor.iniciarNivel(numero, Motor.Ajustes(config.colunas, config.alturaTijolo))
            motor.pilotoAutomatico = modoDemonstracao
        }
        atualizarHud()
        mostrarTransicao()
    }

    /** WF-12: som de inicio de fase, parede esmaecida ao fundo e contagem de 3 s, pulavel com um toque. */
    private fun mostrarTransicao() {
        val info = GeradorDeParede.NIVEIS[motor.nivel - 1]
        findViewById<TextView>(R.id.transicaoNivel).text = getString(R.string.nivel_n, motor.nivel)
        findViewById<TextView>(R.id.transicaoNome).text = info.nome.uppercase()
        findViewById<TextView>(R.id.transicaoResumo).text =
            getString(R.string.resumo_transicao, motor.parede.totalDestrutiveis, motor.vidas)
        val textoContagem = findViewById<TextView>(R.id.transicaoContagem)

        jogoView.renderizador.esmaecerParede = true
        overlayTransicao.isVisible = true
        Sons.tocarInicioDeFase()

        var restante = SEGUNDOS_TRANSICAO
        contagem = object : Runnable {
            override fun run() {
                if (restante == 0) {
                    comecarNivel()
                } else {
                    textoContagem.text = restante.toString()
                    restante--
                    principal.postDelayed(this, 1000)
                }
            }
        }.also { principal.post(it) }
    }

    private fun comecarNivel() {
        cancelarContagem()
        overlayTransicao.isVisible = false
        jogoView.renderizador.esmaecerParede = false
        synchronized(motor) { motor.lancar() }
    }

    private fun mostrarPausa() {
        if (overlayPerdida.isVisible || overlayPausa.isVisible || encerrada) return
        cancelarContagem()
        overlayTransicao.isVisible = false
        textoRetomar.isVisible = false
        synchronized(motor) { motor.pausar() }
        findViewById<TextView>(R.id.pausaResumo).text =
            getString(R.string.resumo_pausa, motor.nivel, formatarPontos(motor.pontos))
        overlayPausa.isVisible = true
    }

    /** Retoma o estado congelado depois de uma contagem curta de 1 s. */
    private fun continuar() {
        overlayPausa.isVisible = false
        when (motor.estado) {
            Motor.Estado.AGUARDANDO -> mostrarTransicao()
            Motor.Estado.PAUSADO -> {
                textoRetomar.text = "1"
                textoRetomar.isVisible = true
                contagem = Runnable {
                    textoRetomar.isVisible = false
                    synchronized(motor) { motor.retomar() }
                }.also { principal.postDelayed(it, 1000) }
            }
            else -> Unit
        }
    }

    /** WF-11: nao cancelavel, pede uma escolha explicita (requisito g). */
    private fun mostrarBolaPerdida() {
        val n = motor.nivel
        findViewById<TextView>(R.id.perdidaNivel).text = getString(R.string.nivel_n_de_5, n)
        findViewById<Button>(R.id.botaoReiniciarNivel).text = getString(R.string.reiniciar_nivel_n, n)
        findViewById<Button>(R.id.botaoProximoNivel).text =
            if (n < GeradorDeParede.TOTAL_DE_NIVEIS) getString(R.string.ir_para_nivel_n, n + 1)
            else getString(R.string.finalizar_partida)
        overlayPerdida.isVisible = true
    }

    private fun encerrar(vitoria: Boolean) {
        if (encerrada) return
        encerrada = true
        cancelarContagem()
        val pontos = motor.pontos
        val destruidos = motor.tijolosDestruidos
        lifecycleScope.launch {
            val melhor = preferencias.registrarPontuacao(pontos)
            startActivity(Intent(this@JogoActivity, FimDeJogoActivity::class.java).apply {
                putExtra(FimDeJogoActivity.EXTRA_VITORIA, vitoria)
                putExtra(FimDeJogoActivity.EXTRA_PONTOS, pontos)
                putExtra(FimDeJogoActivity.EXTRA_NIVEIS, niveisConcluidos)
                putExtra(FimDeJogoActivity.EXTRA_TIJOLOS, destruidos)
                putExtra(FimDeJogoActivity.EXTRA_MELHOR, melhor)
            })
            finish()
        }
    }

    private fun cancelarContagem() {
        contagem?.let { principal.removeCallbacks(it) }
        contagem = null
    }

    private fun atualizarHud() {
        textoNivel.text = getString(R.string.hud_nivel, motor.nivel, GeradorDeParede.TOTAL_DE_NIVEIS)
        textoPontos.text = formatarPontos(motor.pontos)
        textoVidas.text = (1..Motor.VIDAS_INICIAIS).joinToString(" ") { if (it <= motor.vidas) "●" else "○" }
    }

    private fun formatarPontos(pontos: Int) = "%03d %03d".format(pontos / 1000, pontos % 1000)

    private fun vibrar() {
        val vibrador = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrador.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrador.vibrate(120)
        }
    }

    // --- eventos do motor (chegam pela thread do laco de jogo) ---------------------------------

    override fun aoRebaterNoPaddle() = Sons.tocarRebatida()

    override fun aoAtingirTijolo(destruido: Boolean) {
        principal.post { atualizarHud() }
    }

    override fun aoPerderBola() {
        principal.post {
            vibrar()
            atualizarHud()
            if (motor.vidas <= 0) encerrar(vitoria = false) else mostrarBolaPerdida()
        }
    }

    override fun aoConcluirNivel() {
        principal.post {
            niveisConcluidos++
            if (motor.nivel < GeradorDeParede.TOTAL_DE_NIVEIS) iniciarNivel(motor.nivel + 1) else encerrar(vitoria = true)
        }
    }

    companion object {
        const val EXTRA_DEMO = "demo"
        private const val SEGUNDOS_TRANSICAO = 3
    }
}
