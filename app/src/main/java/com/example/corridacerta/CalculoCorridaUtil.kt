package com.example.corridacerta

import android.content.Context

/**
 * Cálculos de custo e preço da corrida, agora baseados nos dados reais
 * configurados pelo motorista na tela de Informações do Veículo
 * (CustosVeiculoPrefs), em vez de constantes fixas.
 */
object CalculoCorridaUtil {

    private const val MARGEM_MINIMA_SE_TARIFA_BAIXA = 0.15 // usada só se a tarifa ficar abaixo do custo

    data class DetalhamentoCusto(
        val combustivel: Double,
        val pedagios: Double,
        val manutencao: Double,
        val desgaste: Double,
        val depreciacao: Double,
        val custoTempo: Double,
        val outrosCustos: Double,
        val custoTotal: Double,
        val lucroEstimado: Double,
        val precoMinimo: Double,
        val precoJusto: Double,
        val precoRecomendado: Double
    )

    /**
     * Estima uma distância/tempo de forma determinística a partir do texto do
     * destino, só para a demonstração ter números plausíveis e consistentes
     * entre as telas. Substitua por uma chamada real de rota assim que tiver
     * a integração com mapas.
     */
    fun estimarDistanciaETempo(destino: String, numeroDeParadas: Int): Pair<Double, Int> {
        val base = 6.0 + (destino.trim().length % 25) * 1.4
        val distanciaKm = base + (numeroDeParadas * 3.5)
        val tempoMin = (distanciaKm * 1.4).toInt() + (numeroDeParadas * 6)
        return Pair(distanciaKm, tempoMin)
    }

    /**
     * @param context usado para ler os custos do veículo salvos em CustosVeiculoPrefs
     */
    fun calcular(context: Context, distanciaKm: Double, tempoMin: Int, precisaEsperar: Boolean): DetalhamentoCusto {
        val dados = CustosVeiculoPrefs.carregar(context)
        val tempoTotalMin = tempoMin + if (precisaEsperar) 30 else 0

        // Custos reais do motorista (o que ele gasta de fato para rodar a corrida)
        val combustivel = distanciaKm * dados.custoCombustivelPorKm
        val manutencao = distanciaKm * dados.custoManutencaoKm
        val desgaste = distanciaKm * dados.custoDesgasteKm
        val depreciacao = distanciaKm * dados.custoDepreciacaoKm
        val pedagios = distanciaKm * dados.custoPedagiosKm
        val outrosCustos = dados.outrosCustosPorCorrida
        // Custo de tempo entra como despesa também (seu tempo tem valor,
        // mesmo que você tenha escolhido cobrar por km — ver explicação no chat)
        val custoTempo = tempoTotalMin * dados.custoPorMinuto

        val custoTotal = combustivel + manutencao + desgaste + depreciacao + pedagios + custoTempo + outrosCustos

        // Tarifa cobrada: só usa o modo escolhido pelo motorista (km OU minuto), + taxa de início
        val tarifaVariavel = if (dados.modoTarifa == DadosVeiculo.MODO_MINUTO) {
            tempoTotalMin * dados.custoPorMinuto
        } else {
            distanciaKm * dados.valorPorKmRodado
        }
        val precoTarifa = dados.taxaInicioCorrida + tarifaVariavel

        val precoMinimo = custoTotal
        val precoJusto = precoTarifa
        val precoRecomendado = maxOf(precoTarifa, custoTotal * (1 + MARGEM_MINIMA_SE_TARIFA_BAIXA))
        val lucroEstimado = precoRecomendado - custoTotal

        return DetalhamentoCusto(
            combustivel = combustivel,
            pedagios = pedagios,
            manutencao = manutencao,
            desgaste = desgaste,
            depreciacao = depreciacao,
            custoTempo = custoTempo,
            outrosCustos = outrosCustos,
            custoTotal = custoTotal,
            lucroEstimado = lucroEstimado,
            precoMinimo = precoMinimo,
            precoJusto = precoJusto,
            precoRecomendado = precoRecomendado
        )
    }

    fun formatarReais(valor: Double): String = "R$ %.2f".format(valor).replace(".", ",")
}