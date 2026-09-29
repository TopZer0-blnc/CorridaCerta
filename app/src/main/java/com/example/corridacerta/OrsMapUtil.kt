package com.example.corridacerta

import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import org.maplibre.android.geometry.LatLng
import java.net.HttpURLConnection
import java.net.URL

data class RotaResultado(
    val distanciaKm: Double,
    val tempoMin: Int,
    val pontosRota: List<LatLng>
)

/**
 * Calcula a rota real (distância, tempo e traçado) usando o servidor público
 * do OSRM (Open Source Routing Machine) sobre dados do OpenStreetMap.
 *
 * Não precisa de chave de API nem de conta de faturamento. Em troca, é um
 * serviço de demonstração mantido pela comunidade: uso "razoável" (a
 * documentação pede no máximo ~1 requisição por segundo), sem garantia de
 * disponibilidade e não recomendado para tráfego comercial pesado. Se o app
 * crescer bastante, o próximo passo é hospedar sua própria instância do OSRM
 * (o software continua gratuito, você só paga o servidor).
 *
 * Documentação: https://github.com/Project-OSRM/osrm-backend/wiki/Api-usage-policy
 */
object OsrmApiUtil {

    private const val BASE_URL = "https://router.project-osrm.org/route/v1/driving/"

    // Troque pelo nome/contato reais do seu app — é exigido pela política de uso do OSRM.
    private const val USER_AGENT = "CorridaCertaApp/1.0"

    fun calcularRota(
        origem: LatLng,
        destino: LatLng,
        paradas: List<LatLng>,
        callback: (RotaResultado?) -> Unit
    ) {
        Thread {
            val resultado = try {
                buscarRota(origem, destino, paradas)
            } catch (e: Exception) {
                null
            }
            Handler(Looper.getMainLooper()).post { callback(resultado) }
        }.start()
    }

    private fun buscarRota(origem: LatLng, destino: LatLng, paradas: List<LatLng>): RotaResultado? {
        val pontos = mutableListOf(origem)
        pontos.addAll(paradas)
        pontos.add(destino)

        val coordenadas = pontos.joinToString(";") { "${it.longitude},${it.latitude}" }
        val urlStr = "$BASE_URL$coordenadas?overview=full&geometries=geojson"

        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 10000
        conn.readTimeout = 10000

        val resposta = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val json = JSONObject(resposta)
        if (json.optString("code") != "Ok") return null

        val rotas = json.getJSONArray("routes")
        if (rotas.length() == 0) return null
        val rota = rotas.getJSONObject(0)

        val distanciaMetros = rota.getDouble("distance")
        val duracaoSegundos = rota.getDouble("duration")

        val coordenadasRota = rota.getJSONObject("geometry").getJSONArray("coordinates")
        val pontosRota = mutableListOf<LatLng>()
        for (i in 0 until coordenadasRota.length()) {
            val par = coordenadasRota.getJSONArray(i)
            val lon = par.getDouble(0)
            val lat = par.getDouble(1)
            pontosRota.add(LatLng(lat, lon))
        }

        return RotaResultado(
            distanciaKm = distanciaMetros / 1000.0,
            tempoMin = (duracaoSegundos / 60.0).toInt(),
            pontosRota = pontosRota
        )
    }
}