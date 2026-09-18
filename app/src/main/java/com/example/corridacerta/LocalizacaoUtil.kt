package com.example.corridacerta

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Build
import com.google.android.gms.location.LocationServices
import org.maplibre.android.geometry.LatLng
import java.util.Locale

/**
 * Reúne localização do dispositivo e geocodificação (endereço <-> coordenadas)
 * usadas tanto na tela Nova Corrida (origem) quanto no Mapa/Confirmação.
 *
 * Chame sempre depois de confirmar a permissão ACCESS_FINE_LOCATION,
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
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocation(location.latitude, location.longitude, 1) { enderecos ->
                    aoObter(enderecos.firstOrNull()?.getAddressLine(0))
                }
            } else {
                @Suppress("DEPRECATION")
                val enderecos = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                aoObter(enderecos?.firstOrNull()?.getAddressLine(0))
            }
        } catch (e: Exception) {
            aoObter(null)
        }
    }

    /** Transforma um texto de endereço/destino digitado em coordenadas. */
    fun coordenadasAPartirDoEndereco(context: Context, endereco: String, aoObter: (LatLng?) -> Unit) {
        val geocoder = Geocoder(context, Locale("pt", "BR"))
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                geocoder.getFromLocationName(endereco, 1) { enderecos ->
                    val resultado = enderecos.firstOrNull()
                    aoObter(resultado?.let { LatLng(it.latitude, it.longitude) })
                }
            } else {
                @Suppress("DEPRECATION")
                val enderecos = geocoder.getFromLocationName(endereco, 1)
                val resultado = enderecos?.firstOrNull()
                aoObter(resultado?.let { LatLng(it.latitude, it.longitude) })
            }
        } catch (e: Exception) {
            aoObter(null)
        }
    }
}