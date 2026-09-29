package com.example.corridacerta

import android.content.Context

/**
 * Todos os dados de custo do veículo que alimentam o CalculoCorridaUtil.
 */
data class DadosVeiculo(
    val valorGastoCombustivel: Double,   // R$ gastos no último abastecimento
    val quilometragemTanque: Double,     // km rodados com esse valor
    val consumoKmPorLitro: Double,       // km/L (rendimento informativo do veículo)
    val custoManutencaoKm: Double,       // R$/km
    val custoDepreciacaoKm: Double,      // R$/km
    val custoDesgasteKm: Double,         // R$/km
    val custoPedagiosKm: Double,         // R$/km (média estimada)
    val custoPorMinuto: Double,          // R$/min (também usado como tarifa de tempo)
    val valorPorKmRodado: Double,        // R$/km (tarifa cobrada, estilo Uber)
    val taxaInicioCorrida: Double,       // R$ fixo por corrida (bandeirada)
    val outrosCustosPorCorrida: Double,  // R$ fixo por corrida (opcional)
    val modoTarifa: String = MODO_KM     // "KM" ou "MINUTO" — como a corrida é precificada
) {
    /** Custo de combustível por km, calculado a partir do abastecimento informado. */
    val custoCombustivelPorKm: Double
        get() = if (quilometragemTanque > 0) valorGastoCombustivel / quilometragemTanque else 0.0

    /** Preço aproximado pago por litro, só para exibição/conferência. */
    val precoPorLitro: Double
        get() = if (consumoKmPorLitro > 0) custoCombustivelPorKm * consumoKmPorLitro else 0.0

    companion object {
        const val MODO_KM = "KM"
        const val MODO_MINUTO = "MINUTO"
    }
}

/**
 * Guarda os dados de custo do veículo localmente (SharedPreferences).
 *
 * Para migrar para Firebase (Firestore) depois:
 * 1. Adicione as dependências do Firebase no build.gradle e o google-services.json
 *    gerado no console do Firebase para o seu app.
 * 2. Troque o corpo de `salvar` por algo como:
 *      Firebase.firestore.collection("motoristas").document(uid)
 *          .collection("config").document("custos_veiculo")
 *          .set(dados)
 *    e o corpo de `carregar`/`estaConfigurado` por uma leitura equivalente
 *    (idealmente em corrotina/callback, já que Firestore é assíncrono).
 * O resto do app (CalculoCorridaUtil, telas) não precisa mudar — só chama
 * CustosVeiculoPrefs.carregar(context).
 */
object CustosVeiculoPrefs {

    private const val PREFS_NAME = "custos_veiculo_prefs"

    private const val KEY_VALOR_GASTO_COMBUSTIVEL = "valor_gasto_combustivel"
    private const val KEY_QUILOMETRAGEM_TANQUE = "quilometragem_tanque"
    private const val KEY_CONSUMO_KM_LITRO = "consumo_km_litro"
    private const val KEY_MANUTENCAO_KM = "manutencao_km"
    private const val KEY_DEPRECIACAO_KM = "depreciacao_km"
    private const val KEY_DESGASTE_KM = "desgaste_km"
    private const val KEY_PEDAGIOS_KM = "pedagios_km"
    private const val KEY_CUSTO_MINUTO = "custo_minuto"
    private const val KEY_VALOR_POR_KM = "valor_por_km"
    private const val KEY_TAXA_INICIO = "taxa_inicio"
    private const val KEY_OUTROS_CUSTOS = "outros_custos"
    private const val KEY_MODO_TARIFA = "modo_tarifa"

    fun salvar(context: Context, dados: DadosVeiculo) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_VALOR_GASTO_COMBUSTIVEL, dados.valorGastoCombustivel.toFloat())
            putFloat(KEY_QUILOMETRAGEM_TANQUE, dados.quilometragemTanque.toFloat())
            putFloat(KEY_CONSUMO_KM_LITRO, dados.consumoKmPorLitro.toFloat())
            putFloat(KEY_MANUTENCAO_KM, dados.custoManutencaoKm.toFloat())
            putFloat(KEY_DEPRECIACAO_KM, dados.custoDepreciacaoKm.toFloat())
            putFloat(KEY_DESGASTE_KM, dados.custoDesgasteKm.toFloat())
            putFloat(KEY_PEDAGIOS_KM, dados.custoPedagiosKm.toFloat())
            putFloat(KEY_CUSTO_MINUTO, dados.custoPorMinuto.toFloat())
            putFloat(KEY_VALOR_POR_KM, dados.valorPorKmRodado.toFloat())
            putFloat(KEY_TAXA_INICIO, dados.taxaInicioCorrida.toFloat())
            putFloat(KEY_OUTROS_CUSTOS, dados.outrosCustosPorCorrida.toFloat())
            putString(KEY_MODO_TARIFA, dados.modoTarifa)
            apply()
        }
    }

    fun carregar(context: Context): DadosVeiculo {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DadosVeiculo(
            valorGastoCombustivel = prefs.getFloat(KEY_VALOR_GASTO_COMBUSTIVEL, 0f).toDouble(),
            quilometragemTanque = prefs.getFloat(KEY_QUILOMETRAGEM_TANQUE, 0f).toDouble(),
            consumoKmPorLitro = prefs.getFloat(KEY_CONSUMO_KM_LITRO, 0f).toDouble(),
            custoManutencaoKm = prefs.getFloat(KEY_MANUTENCAO_KM, 0f).toDouble(),
            custoDepreciacaoKm = prefs.getFloat(KEY_DEPRECIACAO_KM, 0f).toDouble(),
            custoDesgasteKm = prefs.getFloat(KEY_DESGASTE_KM, 0f).toDouble(),
            custoPedagiosKm = prefs.getFloat(KEY_PEDAGIOS_KM, 0f).toDouble(),
            custoPorMinuto = prefs.getFloat(KEY_CUSTO_MINUTO, 0f).toDouble(),
            valorPorKmRodado = prefs.getFloat(KEY_VALOR_POR_KM, 0f).toDouble(),
            taxaInicioCorrida = prefs.getFloat(KEY_TAXA_INICIO, 0f).toDouble(),
            outrosCustosPorCorrida = prefs.getFloat(KEY_OUTROS_CUSTOS, 0f).toDouble(),
            modoTarifa = prefs.getString(KEY_MODO_TARIFA, DadosVeiculo.MODO_KM) ?: DadosVeiculo.MODO_KM
        )
    }

    /** true assim que o motorista já preencheu ao menos a quilometragem do tanque. */
    fun estaConfigurado(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(KEY_QUILOMETRAGEM_TANQUE, 0f) > 0f
    }
}