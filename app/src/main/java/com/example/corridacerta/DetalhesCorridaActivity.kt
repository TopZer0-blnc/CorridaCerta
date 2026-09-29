package com.example.corridacerta

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class DetalhesCorridaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_detalhes_corrida)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val destino = intent.getStringExtra(MapaConfirmacaoActivity.EXTRA_DESTINO).orEmpty()
        val paradas = intent.getStringArrayListExtra(MapaConfirmacaoActivity.EXTRA_PARADAS) ?: arrayListOf()
        val tipoViagem = intent.getStringExtra(MapaConfirmacaoActivity.EXTRA_TIPO_VIAGEM).orEmpty()
        val quando = intent.getStringExtra(MapaConfirmacaoActivity.EXTRA_QUANDO).orEmpty()
        val espera = intent.getStringExtra(MapaConfirmacaoActivity.EXTRA_ESPERA).orEmpty()
        val distanciaKm = intent.getDoubleExtra(MapaConfirmacaoActivity.EXTRA_DISTANCIA_KM, 0.0)
        val tempoMin = intent.getIntExtra(MapaConfirmacaoActivity.EXTRA_TEMPO_MIN, 0)
        val precisaEsperar = espera == "Sim"

        val calculo = CalculoCorridaUtil.calcular(this, distanciaKm, tempoMin, precisaEsperar)

        montarPontos(destino, paradas)
        montarCustos(calculo)

        findViewById<TextView>(R.id.txtTipoViagem).text = tipoViagem
        findViewById<TextView>(R.id.txtQuando).text = quando
        findViewById<TextView>(R.id.txtEspera).text = if (precisaEsperar) "Sim (30 min)" else "Não"

        findViewById<TextView>(R.id.txtCustoTotal).text = CalculoCorridaUtil.formatarReais(calculo.custoTotal)
        findViewById<TextView>(R.id.txtLucroEstimado).text = CalculoCorridaUtil.formatarReais(calculo.lucroEstimado)
        findViewById<TextView>(R.id.txtPrecoMinimo).text = CalculoCorridaUtil.formatarReais(calculo.precoMinimo)
        findViewById<TextView>(R.id.txtPrecoJusto).text = CalculoCorridaUtil.formatarReais(calculo.precoJusto)
        findViewById<TextView>(R.id.txtPrecoRecomendado).text = CalculoCorridaUtil.formatarReais(calculo.precoRecomendado)

        findViewById<TextView>(R.id.btnVoltar).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnCompartilhar).setOnClickListener {
            compartilharCorrida(destino, distanciaKm, tempoMin, calculo.precoRecomendado)
        }
    }

    /** Monta a lista numerada: Origem, Parada(s) e Destino, como no mockup. */
    private fun montarPontos(destino: String, paradas: List<String>) {
        val container = findViewById<LinearLayout>(R.id.containerPontos)
        container.removeAllViews()

        val pontos = mutableListOf("Minha localização atual")
        pontos.addAll(paradas)
        pontos.add(destino)

        pontos.forEachIndexed { index, texto ->
            val label = when (index) {
                0 -> "Origem"
                pontos.lastIndex -> "Destino"
                else -> "Parada $index"
            }
            container.addView(criarLinhaPonto(index + 1, label, texto))
            if (index != pontos.lastIndex) {
                container.addView(criarEspacoPequeno())
            }
        }
    }

    private fun criarLinhaPonto(numero: Int, label: String, texto: String): LinearLayout {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val badge = TextView(this).apply {
            text = numero.toString()
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(56, 56)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(getColor(R.color.surface_card_light))
            }
        }

        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            params.marginStart = 24
            layoutParams = params
        }

        val txtLabel = TextView(this).apply {
            text = label
            setTextColor(getColor(R.color.text_secondary))
            textSize = 12f
        }

        val txtValor = TextView(this).apply {
            text = texto
            setTextColor(getColor(R.color.text_primary))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        textos.addView(txtLabel)
        textos.addView(txtValor)
        linha.addView(badge)
        linha.addView(textos)
        return linha
    }

    private fun criarEspacoPequeno(): View {
        return View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 20)
        }
    }

    /** Monta a lista de custos do cálculo detalhado. */
    private fun montarCustos(calculo: CalculoCorridaUtil.DetalhamentoCusto) {
        val container = findViewById<LinearLayout>(R.id.containerCustos)
        container.removeAllViews()

        val itens = listOf(
            "Combustível" to calculo.combustivel,
            "Pedágios" to calculo.pedagios,
            "Manutenção" to calculo.manutencao,
            "Desgaste" to calculo.desgaste,
            "Depreciação" to calculo.depreciacao,
            "Custo de tempo" to calculo.custoTempo,
            "Outros custos" to calculo.outrosCustos
        )

        itens.forEachIndexed { index, (label, valor) ->
            container.addView(criarLinhaCusto(label, valor))
            if (index != itens.lastIndex) {
                val divisor = View(this).apply {
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 2).apply {
                        topMargin = 20
                        bottomMargin = 20
                    }
                    setBackgroundColor(getColor(R.color.divider))
                }
                container.addView(divisor)
            }
        }
    }

    private fun criarLinhaCusto(label: String, valor: Double): LinearLayout {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val txtLabel = TextView(this).apply {
            text = label
            setTextColor(getColor(R.color.text_secondary))
            textSize = 14f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val txtValor = TextView(this).apply {
            text = CalculoCorridaUtil.formatarReais(valor)
            setTextColor(getColor(R.color.text_primary))
            textSize = 14f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        linha.addView(txtLabel)
        linha.addView(txtValor)
        return linha
    }

    private fun compartilharCorrida(destino: String, distanciaKm: Double, tempoMin: Int, valor: Double) {
        val texto = buildString {
            append("Corrida Certa\n")
            append("Destino: $destino\n")
            append("Distância: %.1f km\n".format(distanciaKm))
            append("Tempo estimado: $tempoMin min\n")
            append("Valor: ${CalculoCorridaUtil.formatarReais(valor)}")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }
        startActivity(Intent.createChooser(intent, "Compartilhar corrida"))
    }
}