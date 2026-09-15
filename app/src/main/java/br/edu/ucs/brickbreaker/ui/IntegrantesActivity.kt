package br.edu.ucs.brickbreaker.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.edu.ucs.brickbreaker.R

/** Lista estatica, versionada junto do codigo. */
data class Integrante(val nome: String, val sobrenome: String, val frente: String) {
    val iniciais get() = "${nome.first()}${sobrenome.first()}".uppercase()
}

val INTEGRANTES = listOf(
    Integrante("Marcus", "Sena", "Motor do jogo (game loop, física, colisão)"),
    Integrante("Lucas", "Hoffman", "Telas e navegação"),
    Integrante("Henrique", "Bin Estramar", "Níveis e geração de paredes"),
    Integrante("Luan", "Bossardi", "Áudio e assets"),
    Integrante("Mauricio", "Porgeri", "Testes e geração do APK"),
)

/** WF-03: opcao 1 do menu, nome e sobrenome de todos os integrantes. */
class IntegrantesActivity : TelaCheiaActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_integrantes)
        respeitarRecorte(findViewById(R.id.raiz))
        findViewById<TextView>(R.id.tituloBarra).setText(R.string.integrantes_titulo)

        findViewById<RecyclerView>(R.id.lista).apply {
            layoutManager = LinearLayoutManager(this@IntegrantesActivity)
            adapter = IntegrantesAdapter(INTEGRANTES)
        }
        findViewById<View>(R.id.botaoVoltarTopo).setOnClickListener { finish() }
        findViewById<View>(R.id.botaoVoltar).setOnClickListener { finish() }
    }
}

private class IntegrantesAdapter(private val itens: List<Integrante>) :
    RecyclerView.Adapter<IntegrantesAdapter.Item>() {

    class Item(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: TextView = view.findViewById(R.id.avatar)
        val nome: TextView = view.findViewById(R.id.nome)
        val frente: TextView = view.findViewById(R.id.frente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Item(LayoutInflater.from(parent.context).inflate(R.layout.item_integrante, parent, false))

    override fun onBindViewHolder(holder: Item, position: Int) {
        val integrante = itens[position]
        holder.avatar.text = integrante.iniciais
        holder.nome.text = "${integrante.nome} ${integrante.sobrenome}"
        holder.frente.text = integrante.frente
    }

    override fun getItemCount() = itens.size
}
