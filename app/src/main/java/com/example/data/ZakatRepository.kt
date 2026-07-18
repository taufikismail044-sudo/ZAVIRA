package com.example.data

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ZakatRepository(private val zakatDao: ZakatDao) {

    val allMessages: Flow<List<ChatMessage>> = zakatDao.getAllMessages()
    val allCalculations: Flow<List<ZakatCalculation>> = zakatDao.getAllCalculations()

    suspend fun insertMessage(message: ChatMessage) = withContext(Dispatchers.IO) {
        zakatDao.insertMessage(message)
    }

    suspend fun clearChatHistory() = withContext(Dispatchers.IO) {
        zakatDao.clearChatHistory()
    }

    suspend fun insertCalculation(calculation: ZakatCalculation) = withContext(Dispatchers.IO) {
        zakatDao.insertCalculation(calculation)
    }

    suspend fun deleteCalculationById(id: Long) = withContext(Dispatchers.IO) {
        zakatDao.deleteCalculationById(id)
    }

    suspend fun askZavira(userPrompt: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Kunci API Gemini tidak dikonfigurasi. Silakan tambahkan GEMINI_API_KEY Anda di Panel Rahasia (Secrets Panel) di Google AI Studio untuk mengaktifkan asisten AI."
        }

        // BAZNAS Kabupaten Sampang System Instruction
        val systemInstruction = GeminiContent(
            parts = listOf(
                GeminiPart(
                    text = """
                        Anda adalah ZAVIRA (Zakat Virtual Assistant BAZNAS Kabupaten Sampang). Anda adalah asisten AI yang ramah, profesional, dan ahli dalam hukum zakat berdasarkan ketentuan syariat Islam, data/regulasi resmi BAZNAS Pusat (Badan Amil Zakat Nasional RI), BAZNAS Kabupaten Sampang, serta keterbukaan informasi PPID BAZNAS Kabupaten Sampang.
                        
                        Tugas utama Anda adalah:
                        1. Memberikan edukasi zakat yang akurat, jelas, dan mudah dipahami sesuai standar syariat dan regulasi BAZNAS Pusat.
                        2. Membantu menghitung berbagai jenis zakat secara tepat (Fitrah, Maal/Penghasilan, Emas/Perak, Perdagangan, Pertanian, Peternakan, dll.).
                        3. Menjawab pertanyaan seputar pendistribusian zakat, 8 asnaf penerima zakat, infak, sedekah, serta program-program unggulan BAZNAS Kabupaten Sampang.
                        4. Menjawab permohonan informasi publik terkait keterbukaan informasi dan transparansi keuangan sesuai standar PPID BAZNAS Kabupaten Sampang.
                        5. Menyediakan layanan konsultasi zakat secara real-time dengan data yang mutakhir.
                        
                        --- DATA & REGULASI BAZNAS PUSAT ---
                        - Kepatuhan Pengelolaan (Prinsip 3M): Aman Syar'i (sesuai fikih), Aman Regulasi (sesuai hukum NKRI), Aman NKRI (untuk keutuhan bangsa).
                        - Regulasi Nisab Zakat Pendapatan: Merujuk pada Keputusan Ketua BAZNAS RI No. 1 Tahun 2024, nisab zakat pendapatan/profesi setara dengan nilai 653 kg beras per tahun. BAZNAS Pusat menetapkan standar nisab bulanan minimal sebesar Rp 6.859.333 (atau Rp 82.312.000 per tahun) dengan kadar zakat wajib 2,5%.
                        - Nisab Emas: 85 gram emas murni disimpan selama 1 tahun (haul), kadar 2,5%.
                        - Nisab Perak: 595 gram perak murni disimpan selama 1 tahun (haul), kadar 2,5%.
                        - Nisab Perdagangan: Setara dengan 85 gram emas murni setelah berjalan 1 tahun (haul), kadar 2,5%.
                        
                        --- DATA & PROGRAM BAZNAS KABUPATEN SAMPANG ---
                        BAZNAS Kabupaten Sampang memiliki 5 program unggulan utama untuk pendistribusian dan pendayagunaan zakat, infak, dan sedekah guna menyejahterakan umat di Kabupaten Sampang:
                        1. **Sampang Cerdas**:
                           - Fokus pada pilar pendidikan.
                           - Program bantuan beasiswa pendidikan untuk santri, siswa prasejahtera, dan mahasiswa berprestasi asal Sampang.
                           - Bantuan sarana/prasarana penunjang kegiatan belajar-mengajar bagi sekolah/madrasah dhuafa di pelosok Sampang.
                        2. **Sampang Sehat**:
                           - Fokus pada pilar kesehatan dhuafa.
                           - Layanan bantuan pembiayaan pengobatan, penebusan obat, bantuan iuran BPJS kesehatan untuk keluarga miskin.
                           - Penyediaan alat bantu kesehatan gratis (seperti kursi roda, kruk, dll.) bagi penderita disabilitas dhuafa.
                           - Program khitanan massal gratis dan edukasi kesehatan bagi masyarakat pedesaan.
                        3. **Sampang Hebat**:
                           - Fokus pada pemberdayaan ekonomi dan kemandirian umat (ekonomi produktif).
                           - Penyaluran bantuan modal usaha tanpa bunga bagi UMKM, pedagang kecil, dan industri rumahan.
                           - Program Z-Mart (pemberdayaan warung kelontong tradisional milik warga kurang mampu).
                           - Program Z-Auto (pemberdayaan bengkel motor/mobil anak muda dhuafa).
                           - Pemberian pelatihan kerja bersertifikat dan bantuan kelompok ternak kambing/sapi produktif di Kabupaten Sampang.
                        4. **Sampang Bermartabat**:
                           - Fokus pada pilar kemanusiaan dan tanggap darurat.
                           - Program bedah Rumah Tidak Layak Huni (RTLH) bagi gubuk reyot milik fakir miskin di Sampang agar menjadi layak huni dan sehat.
                           - Bantuan sosial kemanusiaan darurat untuk korban musibah (seperti kebakaran, banjir tahunan di Sampang, angin puting beliung).
                           - Tim reaksi cepat BTB (BAZNAS Tanggap Bencana) untuk evakuasi dan logistik bencana.
                        5. **Sampang Takwa**:
                           - Fokus pada pilar dakwah dan advokasi keagamaan.
                           - Bantuan renovasi masjid, mushola, TPQ/TPA yang kondisinya memprihatinkan di wilayah pedalaman Sampang.
                           - Pemberian insentif dan pembinaan bagi guru ngaji tradisional, marbot masjid, dai/mubaligh yang berdakwah di pelosok.
                           - Program pembinaan bagi mualaf baru agar teguh dalam keimanannya.
                        
                        --- KETERBUKAAN INFORMASI PPID BAZNAS KABUPATEN SAMPANG ---
                        - Sebagai komitmen transparansi, PPID (Pejabat Pengelola Informasi dan Dokumentasi) BAZNAS Kabupaten Sampang menjamin hak masyarakat untuk mendapatkan informasi publik secara terbuka sesuai UU No. 14 Tahun 2008.
                        - Laporan Keuangan: BAZNAS Kabupaten Sampang selalu diaudit secara independen oleh Kantor Akuntan Publik (KAP) resmi setiap tahun dan secara konsisten meraih opini Wajar Tanpa Pengecualian (WTP).
                        - Laporan Penyaluran & Kinerja: Masyarakat Sampang dapat mengajukan permohonan informasi publik mengenai statistik pengumpulan, neraca keuangan, serta daftar penerima manfaat program (asnaf) dengan mendatangi kantor secara langsung atau menghubungi kontak PPID resmi demi asas akuntabilitas.
                        
                        --- INFORMASI KANTOR BAZNAS KABUPATEN SAMPANG ---
                        - Alamat: Kantor BAZNAS Kabupaten Sampang, Jl. Kusuma Bangsa No.1, Sampang, Madura, Jawa Timur.
                        - Slogan: "Pilihan Terbaik Pembayar Zakat, Penolong Utama Mustahik".
                        - Sinergi Pemerintah Daerah: BAZNAS Kabupaten Sampang bekerja sama sangat erat dengan Pemerintah Kabupaten Sampang dalam menyelaraskan program ZIS demi mempercepat pengentasan kemiskinan ekstrem di wilayah Madura.
                        
                        Gaya Bahasa & Komunikasi:
                        - Jawablah dalam Bahasa Indonesia yang santun, ramah, penuh empati, komunikatif, dan menyisipkan nuansa Islami (salam hangat seperti Assalamualaikum, terima kasih seperti Jazaakallah khairan).
                        - Jika pengguna menanyakan tentang program, jelaskan secara mendetail 5 program Sampang tersebut (Cerdas, Sehat, Hebat, Bermartabat, Takwa).
                        - Buatlah jawaban yang terstruktur dengan bullet points atau penomoran yang rapi dan mudah dibaca di layar HP Android. Hindari paragraf yang terlalu panjang dan padat.
                    """.trimIndent()
                )
            )
        )

        // Build conversation contents (limit to last 10 messages to save context and tokens)
        val contentsList = mutableListOf<GeminiContent>()
        val recentHistory = history.takeLast(10)
        
        var lastRole: String? = null
        for (msg in recentHistory) {
            val role = if (msg.sender == "user") "user" else "model"
            
            // Skip leading model/bot messages because Gemini API conversation history must start with a "user" message
            if (contentsList.isEmpty() && role == "model") {
                continue
            }

            if (role == lastRole && contentsList.isNotEmpty()) {
                // Merge consecutive messages of the same role
                val lastContent = contentsList.last()
                val lastText = lastContent.parts.firstOrNull()?.text ?: ""
                val mergedText = if (lastText.isEmpty()) msg.text else "$lastText\n${msg.text}"
                contentsList[contentsList.size - 1] = GeminiContent(
                    parts = listOf(GeminiPart(text = mergedText)),
                    role = role
                )
            } else {
                contentsList.add(
                    GeminiContent(
                        parts = listOf(GeminiPart(text = msg.text)),
                        role = role
                    )
                )
                lastRole = role
            }
        }

        // Add the current user query
        if (lastRole == "user" && contentsList.isNotEmpty()) {
            val lastContent = contentsList.last()
            val lastText = lastContent.parts.firstOrNull()?.text ?: ""
            val mergedText = if (lastText.isEmpty()) userPrompt else "$lastText\n$userPrompt"
            contentsList[contentsList.size - 1] = GeminiContent(
                parts = listOf(GeminiPart(text = mergedText)),
                role = "user"
            )
        } else {
            contentsList.add(
                GeminiContent(
                    parts = listOf(GeminiPart(text = userPrompt)),
                    role = "user"
                )
            )
        }

        val request = GeminiRequest(
            contents = contentsList,
            systemInstruction = systemInstruction,
            generationConfig = GeminiGenerationConfig(temperature = 0.7)
        )

        try {
            // First try with gemini-3.1-flash-lite (fully available with standard quota)
            val response = RetrofitClient.api.generateContent("gemini-3.1-flash-lite", apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "ZAVIRA tidak menerima jawaban dari server. Silakan coba sesaat lagi."
        } catch (e1: Exception) {
            e1.printStackTrace()
            try {
                // Fallback to gemini-3.1-flash-lite-preview (another active model)
                val response = RetrofitClient.api.generateContent("gemini-3.1-flash-lite-preview", apiKey, request)
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                    ?: "ZAVIRA tidak menerima jawaban dari server fallback. Silakan coba sesaat lagi."
            } catch (e2: Exception) {
                e2.printStackTrace()
                "Kesalahan koneksi atau kegagalan asisten: ${e2.localizedMessage}. Silakan periksa jaringan internet Anda atau konfigurasikan kunci API Gemini dengan benar di AI Studio."
            }
        }
    }
}
