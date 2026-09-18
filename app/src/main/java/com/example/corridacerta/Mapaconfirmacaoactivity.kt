package com.example.corridacerta

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

class MapaConfirmacaoActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var maplibreMap: MapLibreMap? = null

    private var origem = ""
    private var destino = ""
    private var paradas: ArrayList<String> = arrayListOf()
    private var tipoViagem = ""
    private var quando = ""
    private var espera = ""

    // Resultado real vindo do OSRM. Se ficar null, usamos o placeholder ao confirmar.
    private var rotaReal: RotaResultado? = null

    private val launcherPermissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) montarMapa()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapLibre.getInstance(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapaconfirmacaoactivity)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        origem = intent.getStringExtra(EXTRA_ORIGEM).orEmpty().ifBlank { "Minha localização atual" }
        destino = intent.getStringExtra(EXTRA_DESTINO).orEmpty()
        paradas = intent.getStringArrayListExtra(EXTRA_PARADAS) ?: arrayListOf()
        tipoViagem = intent.getStringExtra(EXTRA_TIPO_VIAGEM).orEmpty()
        quando = intent.getStringExtra(EXTRA_QUANDO).orEmpty()
        espera = intent.getStringExtra(EXTRA_ESPERA).orEmpty()

        findViewById<TextView>(R.id.txtResumoOrigem).text = origem
        findViewById<TextView>(R.id.txtResumoDestino).text = destino

        findViewById<TextView>(R.id.btnVoltar).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnConfirmar).setOnClickListener { confirmarESeguir() }

        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { map ->
            maplibreMap = map
            map.setStyle(ESTILO_MAPA) {
                verificarPermissaoEMontarMapa()
            }
        }
    }

    private fun verificarPermissaoEMontarMapa() {
        val jaTemPermissao = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (jaTemPermissao) {
            montarMapa()
        } else {
            launcherPermissaoLocalizacao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /** Geocodifica origem/paradas/destino, desenha os marcadores e busca a rota real. */
    private fun montarMapa() {
        val map = maplibreMap ?: return
        map.uiSettings.isZoomGesturesEnabled = true

        LocalizacaoUtil.obterLocalizacaoAtual(this) { location ->
            if (location == null) {
                atualizarStatusRota("Não foi possível obter sua localização atual.")
                return@obterLocalizacaoAtual
            }
            val origemLatLng = LatLng(location.latitude, location.longitude)

            map.addMarker(
                MarkerOptions()
                    .position(origemLatLng)
                    .title("Origem")
                    .icon(IconFactory.getInstance(this).fromResource(R.drawable.dot_origem))
            )
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(origemLatLng, 14.0))

            if (destino.isBlank()) return@obterLocalizacaoAtual

            LocalizacaoUtil.coordenadasAPartirDoEndereco(this, destino) { destinoLatLng ->
                if (destinoLatLng == null) {
                    atualizarStatusRota("Não encontrei esse destino no mapa. A distância será estimada.")
                    return@coordenadasAPartirDoEndereco
                }

                map.addMarker(
                    MarkerOptions()
                        .position(destinoLatLng)
                        .title("Destino")
                        .icon(IconFactory.getInstance(this).fromResource(R.drawable.dot_destino))
                )

                geocodificarParadasEBuscarRota(origemLatLng, destinoLatLng, map)
            }
        }
    }

    private fun geocodificarParadasEBuscarRota(origemLatLng: LatLng, destinoLatLng: LatLng, map: MapLibreMap) {
        val paradasLatLng = mutableListOf<LatLng>()
        var restantes = paradas.size

        fun continuarComParadas() {
            OsrmApiUtil.calcularRota(origemLatLng, destinoLatLng, paradasLatLng) { resultado ->
                if (resultado == null) {
                    atualizarStatusRota("Não foi possível calcular a rota real agora — usaremos uma estimativa.")
                    ajustarCamera(map, listOf(origemLatLng, destinoLatLng) + paradasLatLng)
                    return@calcularRota
                }
                rotaReal = resultado
                map.addPolyline(
                    PolylineOptions()
                        .addAll(resultado.pontosRota)
                        .color(Color.parseColor("#12C48B"))
                        .width(4f)
                )
                ajustarCamera(map, resultado.pontosRota)
                atualizarStatusRota("%.1f km • %d min (rota real via OSRM)".format(resultado.distanciaKm, resultado.tempoMin))
            }
        }

        if (restantes == 0) {
            continuarComParadas()
            return
        }

        paradas.forEach { textoParada ->
            LocalizacaoUtil.coordenadasAPartirDoEndereco(this, textoParada) { latLng ->
                if (latLng != null) paradasLatLng.add(latLng)
                restantes--
                if (restantes == 0) continuarComParadas()
            }
        }
    }

    private fun ajustarCamera(map: MapLibreMap, pontos: List<LatLng>) {
        if (pontos.size < 2) return
        val bounds = LatLngBounds.Builder().apply { pontos.forEach { include(it) } }.build()
        map.easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, 80))
    }

    private fun atualizarStatusRota(texto: String) {
        findViewById<TextView>(R.id.txtStatusRota).text = texto
    }

    private fun confirmarESeguir() {
        val rota = rotaReal
        val (distanciaKm, tempoMin) = if (rota != null) {
            Pair(rota.distanciaKm, rota.tempoMin)
        } else {
            CalculoCorridaUtil.estimarDistanciaETempo(destino, paradas.size)
        }

        val intent = Intent(this, ResultadoCorridaActivity::class.java).apply {
            putExtra(EXTRA_ORIGEM, origem)
            putExtra(EXTRA_DESTINO, destino)
            putStringArrayListExtra(EXTRA_PARADAS, paradas)
            putExtra(EXTRA_TIPO_VIAGEM, tipoViagem)
            putExtra(EXTRA_QUANDO, quando)
            putExtra(EXTRA_ESPERA, espera)
            putExtra(EXTRA_DISTANCIA_KM, distanciaKm)
            putExtra(EXTRA_TEMPO_MIN, tempoMin)
        }
        startActivity(intent)
    }

    // --- Ciclo de vida do MapView clássico do MapLibre precisa ser repassado manualmente ---
    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { mapView.onPause(); super.onPause() }
    override fun onStop() { mapView.onStop(); super.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onLowMemory() { super.onLowMemory(); mapView.onLowMemory() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    companion object {
        private const val ESTILO_MAPA = "https://tiles.openfreemap.org/styles/liberty"

        const val EXTRA_ORIGEM = "extra_origem"
        const val EXTRA_DESTINO = "extra_destino"
        const val EXTRA_PARADAS = "extra_paradas"
        const val EXTRA_TIPO_VIAGEM = "extra_tipo_viagem"
        const val EXTRA_QUANDO = "extra_quando"
        const val EXTRA_ESPERA = "extra_espera"
        const val EXTRA_DISTANCIA_KM = "extra_distancia_km"
        const val EXTRA_TEMPO_MIN = "extra_tempo_min"
    }
}