package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ChatMessage
import com.example.data.ZakatCalculation
import com.example.data.ZakatRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class ZakatViewModel(
    application: Application,
    private val repository: ZakatRepository
) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isTtsEnabled by mutableStateOf(false)
        private set

    var isVoiceFeedbackActive by mutableStateOf(false) // Toggle read out automatically
    private var hasGreeted = false

    init {
        // Disabled assistant voice as requested
    }

    override fun onInit(status: Int) {
        // Empty implementation as voice assistant is disabled
    }

    fun playWelcomeGreeting() {
        // Voice is disabled
    }

    fun speak(text: String) {
        // Voice is disabled
    }

    fun stopSpeaking() {
        // Voice is disabled
    }

    override fun onCleared() {
        super.onCleared()
    }

    // Reactive database streams
    val chatMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedCalculations: StateFlow<List<ZakatCalculation>> = repository.allCalculations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Chat Loading State
    var isChatLoading by mutableStateOf(false)
        private set

    // Send chat message
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            // 1. Save user message to database
            val userMsg = ChatMessage(text = text, sender = "user")
            repository.insertMessage(userMsg)

            isChatLoading = true

            // 2. Fetch all messages for conversational context
            val currentHistory = chatMessages.value

            // 3. Query ZAVIRA AI
            val botResponseText = repository.askZavira(text, currentHistory)

            // 4. Save bot message to database
            val botMsg = ChatMessage(text = botResponseText, sender = "bot")
            repository.insertMessage(botMsg)

            isChatLoading = false

            // Auto-speak response if enabled
            if (isVoiceFeedbackActive) {
                speak(botResponseText)
            }
        }
    }

    // Clear chat history
    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            // Add a friendly welcome message from ZAVIRA
            repository.insertMessage(
                ChatMessage(
                    text = "Assalamualaikum Wr. Wb. Selamat datang di ZAVIRA (Zakat Virtual Assistant BAZNAS Kabupaten Sampang). Saya siap membantu Anda berkonsultasi seputar zakat, infak, sedekah, dan membantu melakukan perhitungan zakat secara real-time. Ada yang bisa saya bantu hari ini?",
                    sender = "bot"
                )
            )
        }
    }

    // Check if chat is empty on startup, if so add welcome message
    fun checkAndAddWelcomeMessage() {
        viewModelScope.launch {
            // Need a brief check
            val currentList = chatMessages.value
            if (currentList.isEmpty()) {
                repository.insertMessage(
                    ChatMessage(
                        text = "Assalamualaikum Wr. Wb. Selamat datang di ZAVIRA (Zakat Virtual Assistant BAZNAS Kabupaten Sampang). Saya siap membantu Anda berkonsultasi seputar zakat, infak, sedekah, dan membantu melakukan perhitungan zakat secara real-time. Ada yang bisa saya bantu hari ini?",
                        sender = "bot"
                    )
                )
            }
        }
    }

    // Save Zakat Calculation
    fun saveCalculation(type: String, totalWealth: Double, calculatedZakat: Double, description: String) {
        viewModelScope.launch {
            val calc = ZakatCalculation(
                zakatType = type,
                totalWealth = totalWealth,
                calculatedZakat = calculatedZakat,
                description = description
            )
            repository.insertCalculation(calc)
        }
    }

    // Delete Zakat Calculation
    fun deleteCalculation(id: Long) {
        viewModelScope.launch {
            repository.deleteCalculationById(id)
        }
    }

    // --- ZAKAT CALCULATORS LOGIC ---

    // 1. Zakat Fitrah Calculator
    fun calculateFitrah(jiwa: Int, hargaBerasPerKg: Double): FitrahResult {
        val berasKg = jiwa * 2.5
        val berasLiter = jiwa * 3.5
        val uangRupiah = berasKg * hargaBerasPerKg
        return FitrahResult(jiwa, berasKg, berasLiter, uangRupiah)
    }

    // 2. Zakat Penghasilan Calculator
    // Nisab: 522 kg beras per tahun. Misal harga beras Rp 13.000/kg -> Nisab per bulan = (522 * 13.000) / 12 = Rp 565.500 per bulan?
    // Catatan: BAZNAS RI menentukan Nisab Penghasilan bulanan di kisaran Rp 6.828.333,- (setara harga beras premium lokal atau emas).
    // Kita gunakan standar BAZNAS RI Rp 6.828.333 atau input dinamis harga beras.
    fun calculatePenghasilan(
        gajiBulanan: Double,
        bonusLain: Double,
        hutangBulanan: Double,
        hargaBerasPerKg: Double = 13000.0
    ): PenghasilanResult {
        val totalPendapatan = gajiBulanan + bonusLain
        val pendapatanBersih = totalPendapatan - hutangBulanan
        
        // Nisab Penghasilan bulanan = 522 * harga beras / 12
        val nisabBulanan = (522.0 * hargaBerasPerKg) / 12.0
        val isWajibZakat = pendapatanBersih >= nisabBulanan
        val zakatAmount = if (isWajibZakat) pendapatanBersih * 0.025 else 0.0

        return PenghasilanResult(
            totalPendapatan = totalPendapatan,
            pendapatanBersih = pendapatanBersih,
            nisabBulanan = nisabBulanan,
            isWajibZakat = isWajibZakat,
            zakatAmount = zakatAmount
        )
    }

    // 3. Zakat Emas / Perak Calculator
    // Nisab Emas: 85 gram. Kadar: 2.5%.
    // Nisab Perak: 595 gram. Kadar: 2.5%.
    fun calculateEmasPerak(
        beratEmas: Double,
        hargaEmasPerGram: Double,
        beratPerak: Double,
        hargaPerakPerGram: Double,
        haulSatuTahun: Boolean
    ): EmasPerakResult {
        val nilaiEmas = beratEmas * hargaEmasPerGram
        val nisabEmasRupiah = 85.0 * hargaEmasPerGram
        val wajibEmas = beratEmas >= 85.0 && haulSatuTahun
        val zakatEmas = if (wajibEmas) nilaiEmas * 0.025 else 0.0

        val nilaiPerak = beratPerak * hargaPerakPerGram
        val nisabPerakRupiah = 595.0 * hargaPerakPerGram
        val wajibPerak = beratPerak >= 595.0 && haulSatuTahun
        val zakatPerak = if (wajibPerak) nilaiPerak * 0.025 else 0.0

        return EmasPerakResult(
            nilaiEmas = nilaiEmas,
            wajibEmas = wajibEmas,
            zakatEmas = zakatEmas,
            nilaiPerak = nilaiPerak,
            wajibPerak = wajibPerak,
            zakatPerak = zakatPerak,
            totalZakat = zakatEmas + zakatPerak
        )
    }

    // 4. Zakat Perdagangan Calculator
    // Nisab: Setara 85 gram emas. Kadar: 2.5%.
    fun calculatePerdagangan(
        modalDagang: Double,
        keuntungan: Double,
        piutangLancar: Double,
        hutangJatuhTempo: Double,
        hargaEmasPerGram: Double
    ): PerdaganganResult {
        val asetBersih = (modalDagang + keuntungan + piutangLancar) - hutangJatuhTempo
        val nisabRupiah = 85.0 * hargaEmasPerGram
        val wajibZakat = asetBersih >= nisabRupiah
        val zakatAmount = if (wajibZakat) asetBersih * 0.025 else 0.0

        return PerdaganganResult(
            asetBersih = asetBersih,
            nisabRupiah = nisabRupiah,
            wajibZakat = wajibZakat,
            zakatAmount = zakatAmount
        )
    }

    // 5. Zakat Pertanian Calculator
    // Nisab: 5 Wasaq = 653 kg beras/gabah.
    // Kadar: 5% (irigasi/buatan berbayar) atau 10% (tadah hujan/alami).
    fun calculatePertanian(
        hasilPanenKg: Double,
        hargaPanenPerKg: Double,
        jenisPengairan: String // "irigasi" atau "tadah_hujan"
    ): PertanianResult {
        val totalNilaiPanen = hasilPanenKg * hargaPanenPerKg
        val wajibZakat = hasilPanenKg >= 653.0
        val tarifZakat = if (jenisPengairan == "irigasi") 0.05 else 0.10
        val zakatAmount = if (wajibZakat) totalNilaiPanen * tarifZakat else 0.0

        return PertanianResult(
            totalNilaiPanen = totalNilaiPanen,
            wajibZakat = wajibZakat,
            tarifZakat = tarifZakat,
            zakatAmount = zakatAmount
        )
    }
}

// --- Results Data Classes ---

data class FitrahResult(
    val jiwa: Int,
    val berasKg: Double,
    val berasLiter: Double,
    val uangRupiah: Double
)

data class PenghasilanResult(
    val totalPendapatan: Double,
    val pendapatanBersih: Double,
    val nisabBulanan: Double,
    val isWajibZakat: Boolean,
    val zakatAmount: Double
)

data class EmasPerakResult(
    val nilaiEmas: Double,
    val wajibEmas: Boolean,
    val zakatEmas: Double,
    val nilaiPerak: Double,
    val wajibPerak: Boolean,
    val zakatPerak: Double,
    val totalZakat: Double
)

data class PerdaganganResult(
    val asetBersih: Double,
    val nisabRupiah: Double,
    val wajibZakat: Boolean,
    val zakatAmount: Double
)

data class PertanianResult(
    val totalNilaiPanen: Double,
    val wajibZakat: Boolean,
    val tarifZakat: Double,
    val zakatAmount: Double
)

// ViewModel Factory
class ZakatViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ZakatViewModel::class.java)) {
            val database = AppDatabase.getDatabase(application)
            val repository = ZakatRepository(database.zakatDao())
            @Suppress("UNCHECKED_CAST")
            return ZakatViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
