package com.example.corridacerta

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Tela de configuração dos custos do veículo. É a primeira tela do app:
 * se o motorista ainda não configurou nada, ela aparece antes da splash.
 * Se já tiver configurado, o app pula direto para a MainActivity — a não
 * ser que seja aberta em modo de edição (a partir de "Configurações").
 */
class VeiculoCustosActivity : AppCompatActivity() {

    private var modoEdicao = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        modoEdicao = intent.getBooleanExtra(EXTRA_MODO_EDICAO, false)

        // Se já está tudo configurado e não é edição explícita, pula direto pro app.
        if (!modoEdicao && CustosVeiculoPrefs.estaConfigurado(this)) {
            irParaTelaInicial()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_veiculo_custos)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        preencherComDadosSalvos()
        configurarToggleModoTarifa()

        findViewById<Button>(R.id.btnSalvarContinuar).setOnClickListener {
            salvarESeguir()
        }
    }

    private fun preencherComDadosSalvos() {
        val dados = CustosVeiculoPrefs.carregar(this)
        if (!CustosVeiculoPrefs.estaConfigurado(this)) return

        findViewById<EditText>(R.id.edtValorGastoCombustivel).setText(formatarInput(dados.valorGastoCombustivel))
        findViewById<EditText>(R.id.edtQuilometragemTanque).setText(formatarInput(dados.quilometragemTanque))
        findViewById<EditText>(R.id.edtConsumoKmLitro).setText(formatarInput(dados.consumoKmPorLitro))
        findViewById<EditText>(R.id.edtManutencaoKm).setText(formatarInput(dados.custoManutencaoKm))
        findViewById<EditText>(R.id.edtDepreciacaoKm).setText(formatarInput(dados.custoDepreciacaoKm))
        findViewById<EditText>(R.id.edtDesgasteKm).setText(formatarInput(dados.custoDesgasteKm))
        findViewById<EditText>(R.id.edtPedagiosKm).setText(formatarInput(dados.custoPedagiosKm))
        findViewById<EditText>(R.id.edtCustoMinuto).setText(formatarInput(dados.custoPorMinuto))
        findViewById<EditText>(R.id.edtValorPorKm).setText(formatarInput(dados.valorPorKmRodado))
        findViewById<EditText>(R.id.edtTaxaInicio).setText(formatarInput(dados.taxaInicioCorrida))

        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupModoTarifa)
        if (dados.modoTarifa == DadosVeiculo.MODO_MINUTO) {
            radioGroup.check(R.id.radioModoMinuto)
        } else {
            radioGroup.check(R.id.radioModoKm)
        }
        atualizarCamposHabilitados(dados.modoTarifa)
    }

    private fun formatarInput(valor: Double): String =
        if (valor == 0.0) "" else valor.toString().replace(".", ",")

    /** Só o campo do modo escolhido (km OU minuto) fica habilitado para edição. */
    private fun configurarToggleModoTarifa() {
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupModoTarifa)
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val modo = if (checkedId == R.id.radioModoMinuto) DadosVeiculo.MODO_MINUTO else DadosVeiculo.MODO_KM
            atualizarCamposHabilitados(modo)
        }
    }

    private fun atualizarCamposHabilitados(modoTarifa: String) {
        val edtValorPorKm = findViewById<EditText>(R.id.edtValorPorKm)
        val edtCustoMinuto = findViewById<EditText>(R.id.edtCustoMinuto)

        val usaKm = modoTarifa == DadosVeiculo.MODO_KM
        edtValorPorKm.isEnabled = usaKm
        edtValorPorKm.alpha = if (usaKm) 1f else 0.4f
        edtCustoMinuto.isEnabled = !usaKm
        edtCustoMinuto.alpha = if (usaKm) 0.4f else 1f
    }

    private fun salvarESeguir() {
        val valorGastoCombustivel = lerDouble(R.id.edtValorGastoCombustivel)
        val quilometragemTanque = lerDouble(R.id.edtQuilometragemTanque)
        val consumoKmPorLitro = lerDouble(R.id.edtConsumoKmLitro)
        val manutencaoKm = lerDouble(R.id.edtManutencaoKm)
        val depreciacaoKm = lerDouble(R.id.edtDepreciacaoKm)
        val desgasteKm = lerDouble(R.id.edtDesgasteKm)
        val pedagiosKm = lerDouble(R.id.edtPedagiosKm)
        val custoMinuto = lerDouble(R.id.edtCustoMinuto)
        val valorPorKm = lerDouble(R.id.edtValorPorKm)
        val taxaInicio = lerDouble(R.id.edtTaxaInicio)

        val modoMinuto = findViewById<RadioGroup>(R.id.radioGroupModoTarifa).checkedRadioButtonId == R.id.radioModoMinuto
        val modoTarifa = if (modoMinuto) DadosVeiculo.MODO_MINUTO else DadosVeiculo.MODO_KM

        // Validação: os custos por km e o combustível são sempre obrigatórios,
        // e só o campo do modo de precificação escolhido precisa ser > 0.
        val erro = when {
            valorGastoCombustivel <= 0 || quilometragemTanque <= 0 ->
                "Informe o valor gasto e a quilometragem do combustível."
            manutencaoKm < 0 || depreciacaoKm < 0 || desgasteKm < 0 || pedagiosKm < 0 ->
                "Os custos por km não podem ser negativos."
            taxaInicio < 0 ->
                "A taxa de início não pode ser negativa."
            modoTarifa == DadosVeiculo.MODO_KM && valorPorKm <= 0 ->
                "Você escolheu precificar por km: informe o valor por km rodado."
            modoTarifa == DadosVeiculo.MODO_MINUTO && custoMinuto <= 0 ->
                "Você escolheu precificar por minuto: informe o custo por minuto."
            else -> null
        }

        val txtErro = findViewById<TextView>(R.id.txtErro)
        if (erro != null) {
            txtErro.text = erro
            txtErro.visibility = android.view.View.VISIBLE
            return
        }
        txtErro.visibility = android.view.View.GONE

        val dados = DadosVeiculo(
            valorGastoCombustivel = valorGastoCombustivel,
            quilometragemTanque = quilometragemTanque,
            consumoKmPorLitro = consumoKmPorLitro,
            custoManutencaoKm = manutencaoKm,
            custoDepreciacaoKm = depreciacaoKm,
            custoDesgasteKm = desgasteKm,
            custoPedagiosKm = pedagiosKm,
            custoPorMinuto = custoMinuto,
            valorPorKmRodado = valorPorKm,
            taxaInicioCorrida = taxaInicio,
            outrosCustosPorCorrida = 0.0,
            modoTarifa = modoTarifa
        )
        CustosVeiculoPrefs.salvar(this, dados)

        if (modoEdicao) {
            finish()
        } else {
            irParaTelaInicial()
        }
    }

    private fun lerDouble(id: Int): Double {
        val texto = findViewById<EditText>(id).text.toString().trim().replace(",", ".")
        return texto.toDoubleOrNull() ?: 0.0
    }

    private fun irParaTelaInicial() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val EXTRA_MODO_EDICAO = "extra_modo_edicao"
    }
}