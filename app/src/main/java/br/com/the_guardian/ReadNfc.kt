package br.com.the_guardian

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NdefMessage
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.IOException
import java.nio.charset.Charset

class ReadNfc : AppCompatActivity() {

    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_read_nfc)

        // Inicializa o NfcAdapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Verifica se o NFC está habilitado no dispositivo
        if (!nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "Por favor, ative o NFC nas configurações do seu aparelho", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()

        // Configura um PendingIntent para que esta atividade seja acionada quando uma tag NFC for detectada
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Configura os filtros de intenção para processar somente ações relacionadas a NFC
        val intentFilters = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))

        // Registra os filtros de intenção e o PendingIntent
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()

        // Desabilita o envio em primeiro plano para evitar o consumo de energia quando a atividade não está em foco
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Verifica se a intent contém uma tag NFC
        if (NfcAdapter.ACTION_TAG_DISCOVERED == intent.action) {
            // Extrai as mensagens NDEF da intent
            val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
            if (rawMessages != null) {
                val ndefMessages = rawMessages.mapNotNull { it as? NdefMessage } // Flatten the list of messages
                if (ndefMessages.isNotEmpty()) {
                    // Percorre todas as mensagens NDEF
                    for (message in ndefMessages) {
                        // Percorre todos os registros de cada mensagem NDEF
                        for (record in message.records) {
                            // Extrai a tag NFC de cada registro
                            val tag = record.toByteArray()
                            // Ler os dados na tag:
                            readNfcTag(tag)
                        }
                    }
                } else {
                    Toast.makeText(this, "Nenhuma mensagem NDEF encontrada", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Nenhuma mensagem NDEF encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readNfcTag(tag: ByteArray) {
        try {
            // Converte o array de bytes em uma instância de Tag
            val nfcTag: Tag = tag as Tag
            // Obtém uma instância de Ndef para ler na tag NFC
            val ndef = Ndef.get(nfcTag)
            if (ndef != null) {
                // Habilita a conexão com a tag NFC
                ndef.connect()
                // Lê o NdefMessage na tag NFC
                val ndefMessage = ndef.ndefMessage
                // Fecha a conexão com a tag NFC
                ndef.close()

                // Percorre todos os registros de cada mensagem NDEF
                for (record in ndefMessage.records) {
                    // Extrai a tag NFC de cada registro
                    val payload = record.payload
                    val qrCodeContent = String(payload, Charset.defaultCharset())
                    Toast.makeText(this, "Dados do QR Code lidos na tag NFC: $qrCodeContent", Toast.LENGTH_SHORT).show()

                    // Se a tag NFC não estiver vazia, redireciona o usuário para a tela AbrirArmario
                    if (qrCodeContent.isNotEmpty()) {
                        val intent = Intent(this, ConfirmarUsuario::class.java)
                        startActivity(intent)
                    }
                    else{
                        Toast.makeText(this, "Erro ao ler pulseira, tente novamente!", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "A tag NFC não suporta NDEF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao ler na tag NFC", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro desconhecido ao ler na tag NFC", Toast.LENGTH_SHORT).show()
        }
    }
}