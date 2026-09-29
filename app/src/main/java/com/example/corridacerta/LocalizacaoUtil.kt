package com.example.corridacerta

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import org.maplibre.android.geometry.LatLng

/**
 * Localização do dispositivo (GPS, via FusedLocationProviderClient) e
 * geocodificação de endereços (texto <-> coordenadas), usadas tanto na tela
 * Nova Corrida (origem) quanto no Mapa/Confirmação.
 *
 * A geocodificação usa o Nominatim (OpenStreetMap) em vez do Geocoder nativo
 * do Android — assim o endereço que o app resolve é da mesma fonte de dados
 * do mapa e da rota, evitando divergências entre um serviço e outro.
 *
 * Chame sempre depois de confirmar a permissão ACCESS_FINE_LOCATION
 * (ver ActivityResultContracts.RequestPermission nas Activities).
 */
object LocalizacaoUtil {

    @SuppressLint("MissingPermission") // a permissão é checada antes de chamar isto
    fun obterLocalizacaoAtual(context: Context, aoObter: (Location?) -> Unit) {
        val cliente = LocationServices.getFusedLocationProviderClient(context)
        cliente.lastLocation
            .addOnSuccessListener { location -> aoObter(location) }
            .addOnFailureListener { aoObter(null) }
    }

    /** Transforma uma coordenada em um endereço legível (rua, bairro etc.). */
    fun enderecoAPartirDaLocalizacao(context: Context, location: Location, aoObter: (String?) -> Unit) {
        NominatimApiUtil.geocodificarReverso(location.latitude, location.longitude, aoObter)
    }

    /** Transforma um texto de endereço/destino digitado em coordenadas. */
    fun coordenadasAPartirDoEndereco(context: Context, endereco: String, aoObter: (LatLng?) -> Unit) {
        NominatimApiUtil.geocodificar(endereco, aoObter)
    }
}