package br.com.the_guardian

// importações
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.google.firebase.auth.FirebaseAuth
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.database
import java.util.Calendar
import com.google.firebase.firestore.FirebaseFirestore

data class Locacao(
    val userId: String,
    val userLoc: LatLng,
    val actualLocker: homeScreen.Place?,
    val priceSelected: Double
)

class DataScreen : AppCompatActivity() {

    private var places: MutableList<homeScreen.Place> = mutableListOf(
        homeScreen.Place(
            "Armário 1",
            -22.833953,
            -47.052900,
            "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900",
            "Em frente ao prédio h15",
            false
        ),
        homeScreen.Place(
            "Armário 2",
            -22.833877,
            -47.052470,
            "Av. Reitor Benedito José Barreto Fonseca - Parque dos Jacarandás, Campinas - SP, 13086-900",
            "Em frente ao prédio h15",
            true
        ),
        homeScreen.Place(
            "Armário 3",
            -22.834040,
            -47.051999,
            "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP",
            "Em frente ao prédio h13",
            false
        ),
        homeScreen.Place(
            "Armário 4",
            -22.834028,
            -47.051889,
            "Av. Reitor Benedito José Barreto Fonseca, H13 - Parque dos Jacarandás, Campinas - SP",
            "Em frente ao prédio h13",
            true
        ),
        homeScreen.Place(
            "Armário 5",
            -22.833963,
            -47.051539,
            "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900",
            "Em frente ao prédio h11",
            false
        ),
        homeScreen.Place(
            "Armário 6",
            -22.833928,
            -47.051418,
            "Av. Reitor Benedito José Barreto Fonseca - Parque das Universidades, Campinas - SP, 13086-900",
            "Em frente ao prédio h11",
            true
        )
    )

    // variáveis para os botões
    private lateinit var btnConsultar: AppCompatButton
    private lateinit var btnVoltar: AppCompatButton
    private lateinit var actualLocker: homeScreen.Place
    private lateinit var db: FirebaseFirestore
    private lateinit var database: DatabaseReference

    private lateinit var auth: FirebaseAuth
    private var locacaoAtual: Locacao? = null
    private lateinit var userId: String // Movendo a declaração para o escopo adequado

    private fun confirmacao(locacao: Locacao) {
        Toast.makeText(this, "Locação confirmada: $locacao", Toast.LENGTH_SHORT).show()
    }

    companion object {
        var locacoesConfirmadas = mutableListOf<Locacao>()
        // Variável global para indicar se uma locação foi confirmada, usa-la para mandar o usuario direto para tela do qrcode
        var locacaoConfirmada: Boolean = false
    }

    private var checkedRadioButtonId: Int = -1
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_screen)

        // Inicialização da FirebaseAuth
        auth = FirebaseAuth.getInstance()
        database = Firebase.database.reference
        db = FirebaseFirestore.getInstance()

        // Obtendo o usuário atual
        userId = auth.currentUser?.uid ?: ""

        verificarLocacaoUsuario()
        if(locacaoConfirmada) {
            enviarParaTelaQRCode()
        }

        // recuperar os dados do lugar referênciado e os preços do intent
        val name = intent.getStringExtra("name")
        val reference = intent.getStringExtra("reference")
        val disponibility = intent.getBooleanExtra("disponibility", false)
        val prices = intent.getIntArrayExtra("prices")
        val userLocLatitude = intent.getDoubleExtra("userLocLatitude", 0.0)
        val userLocLongitude = intent.getDoubleExtra("userLocLongitude", 0.0)
        val userLoc = LatLng(userLocLatitude, userLocLongitude)

        // atualizar a interface do usuário com os dados recuperados
        findViewById<TextView>(R.id.marker_title).text = "Alugar $name"
        findViewById<TextView>(R.id.marker_reference).text = reference
        findViewById<TextView>(R.id.marker_disponibility).text = if (disponibility) "Está disponível: Sim" else "Está disponível: Não"


        // configuração dos botões de preço
        val radioButtons = listOf(
            findViewById<RadioButton>(R.id.radiobutton1),
            findViewById(R.id.radiobutton2),
            findViewById(R.id.radiobutton3),
            findViewById(R.id.radiobutton4),
            findViewById(R.id.radiobutton5)
        )

        prices?.forEachIndexed { index, price ->
            if (index < radioButtons.size) {
                radioButtons[index].text = "R$ $price"
            }
        }

        // configuração da disponibilidade e cor dos botões de preço com base na disponibilidade
        radioButtons.forEach { radioButton ->
            radioButton.isEnabled = disponibility
            radioButton.setTextColor(if (disponibility) Color.rgb(160,228,24) else Color.rgb(217,217,217))
        }

        // verifica a hora atual para habilitar ou desabilitar o botão do dia inteiro
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        if (hour in 6..8) {
            radioButtons.last().isEnabled = true
        } else {
            radioButtons.last().isEnabled = false
            radioButtons.last().setTextColor(Color.GRAY)
        }

        // Configurar o botão Voltar para fechar a tela
        btnVoltar = findViewById(R.id.btn_voltar)
        btnVoltar.setOnClickListener {
            finish()
        }

        // Configurar o botão Consultar para iniciar a tela de QrCode
        btnConsultar = findViewById(R.id.btn_consultar)
        btnConsultar.isEnabled = disponibility
        btnConsultar.setOnClickListener {
            var isAnyRadioButtonChecked = false


            // verificação se algum botão de preço foi selecionado
            for (radioButton in radioButtons) {
                if (radioButton.isChecked) {
                    isAnyRadioButtonChecked = true
                    checkedRadioButtonId = radioButton.id
                    break
                }
            }

            fun calcularDistancia(segunda: LatLng):Double {
                val localizacaoAtual = Location("")
                localizacaoAtual.latitude = userLocLatitude
                localizacaoAtual.longitude = userLocLongitude

                val localizacaoSegunda = Location("")
                localizacaoSegunda.latitude = segunda.latitude
                localizacaoSegunda.longitude = segunda.longitude

                val distancia = localizacaoAtual.distanceTo(localizacaoSegunda) / 100.0

                return distancia
            }
            fun checkLocation(): Boolean {
                for (place in places) {
                    val locPlace = LatLng(place.latitude, place.longitude)
                    val distancia = calcularDistancia(locPlace)
                    if (distancia <= 1.0) {
                        actualLocker = place
                        return true
                    }
                }
                return false
            }
            // se algum botão foi selecionado, inicia a tela de QrCode com o preço selecionado
            if (isAnyRadioButtonChecked) {
                if(usuarioEstaLogado()) {
                    if(checkLocation()) {
                        if(!locacaoConfirmada) {
                            if (locacaoAtual == null) {
                                val precoSelecionadoText = findViewById<RadioButton>(checkedRadioButtonId).text.toString()
                                val precoNumerico = precoSelecionadoText.substringAfter("R$ ").toDoubleOrNull()
                                if (precoNumerico != null) {
                                    val locker = actualLocker
                                    val priceSelected = precoNumerico
                                    atualizarStatusLocacaoUsuario()
                                    locacaoAtual = Locacao(userId,userLoc, locker,  priceSelected)
                                    locacoesConfirmadas.add(locacaoAtual!!)
                                    confirmacao(locacaoAtual!!)
                                    val intent = Intent(this, QrCodeScreen::class.java).apply {
                                        putExtra("checkedRadioButtonText", precoSelecionadoText)
                                    }
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "Preço selecionado inválido", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "Você já possui uma locação confirmada.", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, "Você já possui uma locação confirmada.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(baseContext, "Para realizar a locação, você devera estar entre 1 km", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(baseContext, "Para acessar essa funcionalidade, faça o login", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Escolha uma opção", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun verificarLocacaoUsuario() {
        val currentUser = auth.currentUser?.uid
        Log.d("debugg", "entrou na funcao")
        if (currentUser != null) {
            db.collection("Users").whereEqualTo("id", currentUser)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val document = querySnapshot.documents[0]
                        Log.d("debugg", document.toString())
                        val hasLocker = document["hasLocker"]
                        Log.d("debugg", hasLocker.toString())
                        if (hasLocker.toString() == "true") {
                            // O usuário já possui um armário locado
                            locacaoConfirmada = true

                        }
                    } else {
                        Log.d(TAG, "Documento do usuário não encontrado")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(TAG, "Falha ao obter o documento do usuário:", exception)
                }
        }
    }

    private fun atualizarStatusLocacaoUsuario() {
        val currentUser = auth.currentUser?.uid
        if (currentUser != null) {
            db.collection("Users")
                .whereEqualTo("id", currentUser)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        db.collection("Users")
                            .document(document.id)
                            .update("hasLocker", true)
                            .addOnSuccessListener {
                                Log.d(TAG, "Status de locação atualizado com sucesso!")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Erro ao atualizar o status de locação", e)
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Erro ao obter documentos", e)
                }
        }
    }

    private fun enviarParaTelaQRCode() {
        val intent = Intent(this, QrCodeScreen::class.java).apply {
        }
        startActivity(intent)
    }

    private fun usuarioEstaLogado(): Boolean {
        val usuarioAtual = auth.currentUser
        return usuarioAtual != null
    }

}
