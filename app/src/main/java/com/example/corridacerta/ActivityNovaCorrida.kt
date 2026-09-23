package com.example.corridacerta

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class NovaCorridaActivity : AppCompatActivity() {

    private val launcherPermissaoLocalizacao = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedida ->
        if (concedida) preencherOrigemComLocalizacaoAtual()
        // Se negar, o campo simplesmente continua editável manualmente.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_nova_corrida)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        solicitarLocalizacaoOuUsarPermissaoJaConcedida()
        configurarAutocompleteDestino()

        val radioGroupQuando = findViewById<RadioGroup>(R.id.radioGroupQuando)
        val txtDataAgendada = findViewById<TextView>(R.id.txtDataAgendada)

        // Quando o usuário escolhe "Agendar", abre o calendário automaticamente
        radioGroupQuando.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.radioAgendar) {
                abrirCalendario(txtDataAgendada)
            } else {
                txtDataAgendada.text = ""
            }
        }

        // Adicionar parada(s) entre a origem e o destino
        val containerParadas = findViewById<LinearLayout>(R.id.containerParadas)
        val btnAdicionarParada = findViewById<TextView>(R.id.btnAdicionarParada)
        btnAdicionarParada.setOnClickListener {
            adicionarCampoParada(containerParadas)
        }

        // Botão Calcular Corrida
        val btnCalcularCorrida = findViewById<Button>(R.id.btnCalcularCorrida)
        btnCalcularCorrida.setOnClickListener {
            calcularCorrida(containerParadas)
        }

        // Barra de navegação inferior
        findViewById<TextView>(R.id.navInicio).setOnClickListener {
            finish() // volta para a tela inicial (MainActivity)
        }
        findViewById<TextView>(R.id.navHistorico).setOnClickListener {
            Toast.makeText(this, "Histórico em construção", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.navAgendados).setOnClickListener {
            Toast.makeText(this, "Agendados em construção", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.navConfiguracoes).setOnClickListener {
            val intent = Intent(this, VeiculoCustosActivity::class.java).apply {
                putExtra(VeiculoCustosActivity.EXTRA_MODO_EDICAO, true)
            }
            startActivity(intent)
        }
    }

    /** Verifica a permissão de localização; pede se necessário. */
    private fun solicitarLocalizacaoOuUsarPermissaoJaConcedida() {
        val jaTemPermissao = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (jaTemPermissao) {
            preencherOrigemComLocalizacaoAtual()
        } else {
            launcherPermissaoLocalizacao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Busca a localização atual do dispositivo e preenche o campo Origem
     * com o endereço correspondente. O campo continua um EditText normal,
     * então o motorista pode apagar e digitar outro ponto de partida se quiser.
     */
    private fun preencherOrigemComLocalizacaoAtual() {
        val edtOrigem = findViewById<EditText>(R.id.edtOrigem)
        LocalizacaoUtil.obterLocalizacaoAtual(this) { location ->
            if (location == null) return@obterLocalizacaoAtual
            LocalizacaoUtil.enderecoAPartirDaLocalizacao(this, location) { endereco ->
                if (!endereco.isNullOrBlank() && edtOrigem.text.isBlank()) {
                    edtOrigem.setText(endereco)
                }
            }
        }
    }

    /**
     * Sugere endereços reais (via Nominatim/OpenStreetMap) enquanto o usuário
     * digita o destino. Espera 500ms sem digitar novas letras antes de buscar,
     * pra não estourar o limite de ~1 requisição/segundo do serviço gratuito.
     */
    private fun configurarAutocompleteDestino() {
        val edtDestino = findViewById<AutoCompleteTextView>(R.id.edtDestino)
        var sugestoesAtuais: List<NominatimApiUtil.Sugestao> = emptyList()
        val handler = Handler(Looper.getMainLooper())
        var buscaAgendada: Runnable? = null

        edtDestino.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                buscaAgendada?.let { handler.removeCallbacks(it) }
                val texto = s?.toString().orEmpty()
                val novaBusca = Runnable {
                    NominatimApiUtil.buscarSugestoes(texto) { sugestoes ->
                        sugestoesAtuais = sugestoes
                        val adapter = ArrayAdapter(
                            this@NovaCorridaActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            sugestoes.map { it.descricao }
                        )
                        edtDestino.setAdapter(adapter)
                        if (sugestoes.isNotEmpty() && edtDestino.isFocused) edtDestino.showDropDown()
                    }
                }
                buscaAgendada = novaBusca
                handler.postDelayed(novaBusca, 500)
            }
        })
    }

    /**
     * Cria dinamicamente um campo de texto para uma parada extra,
     * com um botão "x" ao lado para removê-la.
     */
    private fun adicionarCampoParada(containerParadas: LinearLayout) {
        val numeroParada = containerParadas.childCount + 1

        val linhaParada = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }

        val edtParada = EditText(this).apply {
            hint = "Parada $numeroParada"
            setTextColor(getColor(R.color.text_primary))
            setHintTextColor(getColor(R.color.text_secondary))
            background = getDrawable(R.drawable.bg_edit_field)
            setPadding(24, 20, 24, 20)
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val btnRemover = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            background = null
            imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.text_secondary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                containerParadas.removeView(linhaParada)
            }
        }

        linhaParada.addView(edtParada)
        linhaParada.addView(btnRemover)
        containerParadas.addView(linhaParada)
    }

    /**
     * Recolhe os dados preenchidos e segue para a tela de mapa/confirmação.
     */
    private fun calcularCorrida(containerParadas: LinearLayout) {
        val edtOrigem = findViewById<EditText>(R.id.edtOrigem)
        val origem = edtOrigem.text.toString().ifBlank { edtOrigem.hint.toString() }

        val destino = findViewById<EditText>(R.id.edtDestino).text.toString()
        if (destino.isBlank()) {
            Toast.makeText(this, "Informe o destino para calcular a corrida", Toast.LENGTH_SHORT).show()
            return
        }

        val paradas = arrayListOf<String>()
        for (i in 0 until containerParadas.childCount) {
            val linha = containerParadas.getChildAt(i) as LinearLayout
            val edt = linha.getChildAt(0) as EditText
            if (edt.text.isNotBlank()) paradas.add(edt.text.toString())
        }

        val idaEVolta = findViewById<RadioGroup>(R.id.radioGroupTipoViagem).checkedRadioButtonId == R.id.radioIdaVolta
        val tipoViagem = if (idaEVolta) "Ida e volta" else "Só ida"

        val vaiAgendar = findViewById<RadioGroup>(R.id.radioGroupQuando).checkedRadioButtonId == R.id.radioAgendar
        val txtDataAgendada = findViewById<TextView>(R.id.txtDataAgendada).text.toString()
        val quando = if (vaiAgendar && txtDataAgendada.isNotBlank()) txtDataAgendada else "Agora"

        val vaiEsperar = findViewById<RadioGroup>(R.id.radioGroupEspera).checkedRadioButtonId == R.id.radioEsperaSim
        val espera = if (vaiEsperar) "Sim" else "Não"

        val intent = Intent(this, MapaConfirmacaoActivity::class.java).apply {
            putExtra(MapaConfirmacaoActivity.EXTRA_ORIGEM, origem)
            putExtra(MapaConfirmacaoActivity.EXTRA_DESTINO, destino)
            putStringArrayListExtra(MapaConfirmacaoActivity.EXTRA_PARADAS, paradas)
            putExtra(MapaConfirmacaoActivity.EXTRA_TIPO_VIAGEM, tipoViagem)
            putExtra(MapaConfirmacaoActivity.EXTRA_QUANDO, quando)
            putExtra(MapaConfirmacaoActivity.EXTRA_ESPERA, espera)
        }
        startActivity(intent)
    }

    private fun abrirCalendario(txtDataAgendada: TextView) {
        val calendario = Calendar.getInstance()
        val ano = calendario.get(Calendar.YEAR)
        val mes = calendario.get(Calendar.MONTH)
        val dia = calendario.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, anoSelecionado, mesSelecionado, diaSelecionado ->
                val dataFormatada = String.format(
                    "%02d/%02d/%04d",
                    diaSelecionado,
                    mesSelecionado + 1,
                    anoSelecionado
                )
                txtDataAgendada.text = "Data agendada: $dataFormatada"
            },
            ano, mes, dia
        )
        // Não deixa selecionar uma data anterior a hoje
        datePickerDialog.datePicker.minDate = calendario.timeInMillis
        datePickerDialog.show()
    }
}