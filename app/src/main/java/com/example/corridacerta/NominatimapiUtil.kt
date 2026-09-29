package com.example.corridacerta

import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.maplibre.android.geometry.LatLng
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Busca endereços reais usando o Nominatim, o serviço público de geocodificação
 * do OpenStreetMap — gratuito, sem chave de API.
 *
 * Política de uso (importante seguir, é o mesmo espírito do OSRM):
 *  - no máximo ~1 requisição por segundo (por isso o debounce ao digitar)
 *  - é obrigatório um User-Agent identificando o app
 *  - não usar para geocodificação em massa/automatizada
 *  - dar crédito a "© OpenStreetMap contributors" em algum lugar do app
 * Documentação: https://operations.osmfoundation.org/policies/nominatim/
 */
object NominatimApiUtil {

    data class Sugestao(val descricao: String, val latLng: LatLng)

    private const val BASE_URL = "https://nominatim.openstreetmap.org"

    // Troque pelo nome/contato reais do seu app — exigido pela política de uso.
    private const val USER_AGENT = "CorridaCertaApp/1.0"

    /** Sugestões de endereço enquanto o usuário digita (autocompletar). */
    fun buscarSugestoes(consulta: String, callback: (List<Sugestao>) -> Unit) {
        if (consulta.trim().length < 3) {
            callback(emptyList())
            return
        }
        Thread {
            val resultado = try {
                val query = URLEncoder.encode(consulta, "UTF-8")
                val url = "$BASE_URL/search?q=$query&format=json&addressdetails=0&limit=5" +
                        "&countrycodes=br&accept-language=pt-BR"
                val json = fazerRequisicao(url)
                parseSugestoes(json)
            } catch (e: Exception) {
                emptyList()
            }
            Handler(Looper.getMainLooper()).post { callback(resultado) }
        }.start()
    }

    /** Endereço de texto -> coordenadas (pega o primeiro resultado). */
    fun geocodificar(endereco: String, callback: (LatLng?) -> Unit) {
        buscarSugestoes(endereco) { sugestoes -> callback(sugestoes.firstOrNull()?.latLng) }
    }

    /** Coordenadas -> endereço de texto (geocodificação reversa). */
    fun geocodificarReverso(lat: Double, lon: Double, callback: (String?) -> Unit) {
        Thread {
            val resultado = try {
                val url = "$BASE_URL/reverse?lat=$lat&lon=$lon&format=json&accept-language=pt-BR"
                val json = fazerRequisicao(url)
                org.json.JSONObject(json).optString("display_name").ifBlank { null }
            } catch (e: Exception) {
                null
            }
            Handler(Looper.getMainLooper()).post { callback(resultado) }
        }.start()
    }

    private fun fazerRequisicao(urlStr: String): String {
        val conn = URL(urlStr).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        val resposta = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()
        return resposta
    }

    private fun parseSugestoes(json: String): List<Sugestao> {
        val array = JSONArray(json)
        val lista = mutableListOf<Sugestao>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            val nome = item.optString("display_name")
            val lat = item.optString("lat").toDoubleOrNull()
            val lon = item.optString("lon").toDoubleOrNull()
            if (nome.isNotBlank() && lat != null && lon != null) {
                lista.add(Sugestao(nome, LatLng(lat, lon)))
            }
        }
        return lista
    }
}