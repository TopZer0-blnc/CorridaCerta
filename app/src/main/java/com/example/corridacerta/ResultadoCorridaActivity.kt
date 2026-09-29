package com.example.corridacerta

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ResultadoCorridaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_resultado_corrida)
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

        findViewById<TextView>(R.id.txtValorRecomendado).text = CalculoCorridaUtil.formatarReais(calculo.precoRecomendado)
        findViewById<TextView>(R.id.txtDistanciaResumo).text = "%.1f km".format(distanciaKm)
        findViewById<TextView>(R.id.txtTempoResumo).text = formatarTempo(tempoMin)
        findViewById<TextView>(R.id.txtDistancia).text = "%.1f km".format(distanciaKm)
        findViewById<TextView>(R.id.txtTempoDirecao).text = formatarTempo(tempoMin)
        findViewById<TextView>(R.id.txtPedagios).text = CalculoCorridaUtil.formatarReais(calculo.pedagios)

        if (precisaEsperar) {
            findViewById<TextView>(R.id.txtChipEspera).visibility = android.view.View.VISIBLE
        }

        findViewById<TextView>(R.id.btnVoltar).setOnClickListener {
            finish()
        }

        findViewById<Button>(R.id.btnVerDetalhes).setOnClickListener {
            val intent = Intent(this, DetalhesCorridaActivity::class.java).apply {
                putExtra(MapaConfirmacaoActivity.EXTRA_DESTINO, destino)
                putStringArrayListExtra(MapaConfirmacaoActivity.EXTRA_PARADAS, paradas)
                putExtra(MapaConfirmacaoActivity.EXTRA_TIPO_VIAGEM, tipoViagem)
                putExtra(MapaConfirmacaoActivity.EXTRA_QUANDO, quando)
                putExtra(MapaConfirmacaoActivity.EXTRA_ESPERA, espera)
                putExtra(MapaConfirmacaoActivity.EXTRA_DISTANCIA_KM, distanciaKm)
                putExtra(MapaConfirmacaoActivity.EXTRA_TEMPO_MIN, tempoMin)
            }
            startActivity(intent)
        }
    }

    private fun formatarTempo(minutos: Int): String {
        val horas = minutos / 60
        val min = minutos % 60
        return if (horas > 0) "${horas}h ${min}min" else "${min}min"
    }
}