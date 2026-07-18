package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.data.ChatMessage
import com.example.data.ZakatCalculation
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.speech.RecognizerIntent
import android.content.Intent

// Formatter for Indonesian Rupiah
fun formatRupiah(value: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"))
    // Remove decimal points for clean look if it has no fraction
    val formatted = formatter.format(value)
    return if (formatted.endsWith(",00")) formatted.substring(0, formatted.length - 3) else formatted
}

// Formatter for Timestamp
fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"))
    return sdf.format(Date(timestamp))
}

sealed class Screen(val route: String, val title: String, val iconSelected: ImageVector, val iconUnselected: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home, Icons.Filled.Home)
    object Chat : Screen("chat", "Chat ZAVIRA", Icons.Filled.Chat, Icons.Filled.Chat)
    object Calculator : Screen("calculator", "Kalkulator", Icons.Filled.Calculate, Icons.Filled.Calculate)
    object History : Screen("history", "Riwayat", Icons.Filled.History, Icons.Filled.History)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZaviraApp(viewModel: ZakatViewModel) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }

    // Check if welcome message exists on launch and play greeting
    LaunchedEffect(Unit) {
        viewModel.checkAndAddWelcomeMessage()
        viewModel.playWelcomeGreeting()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_zavira_mascot),
                            contentDescription = "ZAVIRA Mascot",
                            modifier = Modifier
                                .size(36.dp)
                        )
                        Column {
                            Text(
                                text = "ZAVIRA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color(0xFFFFD700)
                            )
                            Text(
                                text = "BAZNAS Kabupaten Sampang",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (currentScreen == Screen.Chat) {
                        IconButton(
                            onClick = { viewModel.clearChat() },
                            modifier = Modifier.testTag("clear_chat_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Chat",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                val screens = listOf(Screen.Home, Screen.Chat, Screen.Calculator, Screen.History)
                screens.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.iconSelected else screen.iconUnselected,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                Screen.Home -> HomeScreen(
                    onConsultationClick = { currentScreen = Screen.Chat },
                    onCalculatorClick = { currentScreen = Screen.Calculator },
                    onProgramConsultClick = { programName ->
                        currentScreen = Screen.Chat
                        viewModel.sendChatMessage("Saya ingin berkonsultasi mengenai program $programName dari BAZNAS Sampang.")
                    }
                )
                Screen.Chat -> ChatScreen(viewModel)
                Screen.Calculator -> CalculatorScreen(viewModel)
                Screen.History -> HistoryScreen(viewModel)
            }
        }
    }
}

data class ProgramData(
    val title: String,
    val desc: String,
    val fullDesc: String,
    val target: String,
    val icon: ImageVector,
    val color: Color
)

// ==================== SCREEN 1: HOME SCREEN ====================
@Composable
fun HomeScreen(
    onConsultationClick: () -> Unit,
    onCalculatorClick: () -> Unit,
    onProgramConsultClick: (String) -> Unit = {}
) {
    var selectedProgram by remember { mutableStateOf<ProgramData?>(null) }
    
    val programs = listOf(
        ProgramData(
            title = "Sampang Cerdas",
            desc = "Beasiswa pendidikan bagi anak-anak kurang mampu.",
            fullDesc = "Program bantuan beasiswa pendidikan dan biaya penunjang sekolah untuk anak-anak yatim, piatu, serta dhuafa berprestasi agar terus dapat melanjutkan pendidikan hingga tingkat tinggi.",
            target = "Siswa SD/MI, SMP/MTs, SMA/MA, dan Mahasiswa dari keluarga prasejahtera di Sampang.",
            icon = Icons.Default.School,
            color = Color(0xFF1E88E5)
        ),
        ProgramData(
            title = "Sampang Sehat",
            desc = "Layanan kesehatan gratis & bantuan biaya pengobatan.",
            fullDesc = "Layanan pembiayaan kesehatan darurat, obat-obatan, rujukan rumah sakit, serta bantuan alat bantu (kursi roda, tongkat) bagi pasien dhuafa yang tidak ditanggung oleh jaminan kesehatan umum.",
            target = "Pasien dhuafa sakit menahun, penderita disabilitas, dan warga miskin non-BPJS di Sampang.",
            icon = Icons.Default.Favorite,
            color = Color(0xFFE53935)
        ),
        ProgramData(
            title = "Sampang Hebat",
            desc = "Pemberdayaan ekonomi, permodalan usaha kecil.",
            fullDesc = "Program pembinaan keterampilan usaha, pendampingan wirausaha mikro, bantuan alat produksi, serta penyaluran modal usaha tanpa bunga (Zakat Produktif) demi memandirikan mustahik.",
            target = "Pelaku usaha ultra-mikro, pedagang asongan, dan kepala keluarga prasejahtera yang produktif.",
            icon = Icons.Default.TrendingUp,
            color = Color(0xFF43A047)
        ),
        ProgramData(
            title = "Sampang Bermartabat",
            desc = "Bantuan logistik kebencanaan, santunan dhuafa.",
            fullDesc = "Program respon cepat tanggap darurat bencana alam, santunan pangan darurat (Sembako bulanan) untuk dhuafa lansia sebatangkara, serta bedah rumah tidak layak huni (RTLH).",
            target = "Korban bencana alam/sosial, lansia dhuafa non-produktif, dan keluarga di hunian tidak layak.",
            icon = Icons.Default.VolunteerActivism,
            color = Color(0xFFFB8C00)
        ),
        ProgramData(
            title = "Sampang Takwa",
            desc = "Bantuan insentif guru ngaji, pembinaan keagamaan & sarana ibadah.",
            fullDesc = "Program peningkatan kesejahteraan guru ngaji pelosok, bantuan operasional madrasah diniyah, renovasi ringan sarana ibadah (musholla/masjid), dan syiar dakwah Islam di daerah terpencil.",
            target = "Guru ngaji tradisional, marbot masjid, musholla pelosok, dan dai/daiah di Sampang.",
            icon = Icons.Default.MenuBook,
            color = Color(0xFF8E24AA)
        )
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ZAVIRA",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFD700)
                                )
                                Text(
                                    text = "Zakat Virtual Assistant",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            
                            Image(
                                painter = painterResource(id = R.drawable.img_zavira_mascot),
                                contentDescription = "Mascot ZAVIRA",
                                modifier = Modifier
                                    .size(80.dp)
                                    .padding(start = 8.dp)
                            )
                        }

                        Text(
                            text = "Layanan otomatisasi edukasi zakat dan tanya jawab syariah seputar zakat secara real-time dari BAZNAS Kabupaten Sampang.",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Button(
                                onClick = onConsultationClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("home_consultation_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                    Text("Tanya", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                            
                            OutlinedButton(
                                onClick = onCalculatorClick,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("home_calculator_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Text("Hitung Zakat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // BAZNAS Sampang Programs
        item {
            Text(
                text = "Program Utama BAZNAS Sampang",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProgramCard(
                    program = programs[0],
                    onClick = { selectedProgram = programs[0] },
                    modifier = Modifier.weight(1f)
                )
                ProgramCard(
                    program = programs[1],
                    onClick = { selectedProgram = programs[1] },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProgramCard(
                    program = programs[2],
                    onClick = { selectedProgram = programs[2] },
                    modifier = Modifier.weight(1f)
                )
                ProgramCard(
                    program = programs[3],
                    onClick = { selectedProgram = programs[3] },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProgramCard(
                    program = programs[4],
                    onClick = { selectedProgram = programs[4] },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Edukasi FAQ Section
        item {
            Text(
                text = "Edukasi Zakat (FAQ)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
        }

        items(faqData) { faq ->
            FaqItem(faq = faq)
        }
    }

    // Detail Program Dialog
    if (selectedProgram != null) {
        val program = selectedProgram!!
        AlertDialog(
            onDismissRequest = { selectedProgram = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(program.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = program.icon,
                        contentDescription = null,
                        tint = program.color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = program.title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = program.fullDesc,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Sasaran Penerima (Mustahik):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = program.color,
                                modifier = Modifier.size(14.dp).padding(top = 2.dp)
                            )
                            Text(
                                text = program.target,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onProgramConsultClick(program.title)
                        selectedProgram = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = program.color),
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("Tanya ZAVIRA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedProgram = null },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Tutup", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun ProgramCard(
    program: ProgramData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() }
            .testTag("program_card_${program.title.replace(" ", "_").lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left color accent bar (modern highlight indicator)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(program.color)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Circular icon container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(program.color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = program.icon,
                        contentDescription = null,
                        tint = program.color,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                // Text section
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = program.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = program.desc,
                        fontSize = 8.5.sp,
                        lineHeight = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Clean interactive end icon
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = program.color,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

data class FaqData(val question: String, val answer: String)

val faqData = listOf(
    FaqData(
        "Apa itu Zakat Fitrah?",
        "Zakat Fitrah adalah zakat yang wajib dikeluarkan oleh setiap jiwa Muslim baik laki-laki maupun perempuan di bulan Ramadan sampai sebelum shalat Idul Fitri. Besarannya adalah 2,5 kg atau 3,5 liter beras/makanan pokok, atau setara dengan uang tunai senilai harga beras tersebut."
    ),
    FaqData(
        "Berapa Nisab untuk Zakat Penghasilan?",
        "Nisab zakat penghasilan merujuk pada nisab emas senilai 85 gram per tahun (atau setara 522 kg beras per tahun). BAZNAS RI saat ini menetapkan nisab zakat penghasilan bulanan sebesar Rp 6.828.333,-. Jika penghasilan bersih Anda mencapai angka ini, Anda wajib mengeluarkan zakat 2.5%."
    ),
    FaqData(
        "Bagaimana cara menghitung Zakat Emas?",
        "Jika Anda memiliki emas murni minimal 85 gram yang telah disimpan selama 1 tahun penuh (haul), maka wajib dizakati. Cara menghitungnya: Total Berat Emas (gram) x Harga Emas Per Gram Saat Ini x 2.5%."
    ),
    FaqData(
        "Siapa sajakah golongan penerima Zakat (Asnaf)?",
        "Menurut QS. At-Taubah ayat 60, ada 8 golongan yang berhak menerima zakat: 1. Fakir, 2. Miskin, 3. Amil (pengelola zakat), 4. Muallaf, 5. Riqab (hamba sahaya), 6. Gharimin (yang berutang), 7. Fisabilillah (pejuang di jalan Allah), dan 8. Ibnu Sabil (musafir)."
    )
)

@Composable
fun FaqItem(faq: FaqData) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = if (expanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(0.9f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Sembunyikan" else "Tampilkan",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(0.1f)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = faq.answer,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }
    }
}


// ==================== SCREEN 2: CHAT SCREEN (ZAVIRA ASSISTANT) ====================
@Composable
fun ChatScreen(viewModel: ZakatViewModel) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    val lazyListState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current

    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.getOrNull(0) ?: ""
                if (spokenText.isNotBlank()) {
                    inputText = spokenText
                    viewModel.sendChatMessage(spokenText)
                }
            }
        }
    )

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, viewModel.isChatLoading) {
        if (messages.isNotEmpty()) {
            lazyListState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestionChips = listOf(
        "Berapa Nisab Emas?",
        "Syarat Zakat Maal?",
        "Hukum Zakat Profesi?",
        "Zakat Fitrah di Sampang?"
    )

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Chat History List
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message = message, viewModel = viewModel)
            }

            if (viewModel.isChatLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "ZAVIRA sedang berpikir...",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Suggestions Area
        if (messages.size <= 1 && !viewModel.isChatLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                suggestionChips.forEach { chipText ->
                    SuggestionChip(
                        onClick = {
                            inputText = chipText
                        },
                        label = {
                            Text(
                                text = chipText,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier.testTag("suggestion_chip_$chipText")
                    )
                }
            }
        }

        // Input Field Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Microphone/Voice Command Button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Silakan bicara sekarang...")
                            }
                            try {
                                speechRecognizerLauncher.launch(intent)
                            } catch (e: Exception) {
                                // speech recognition not supported
                            }
                        }
                        .testTag("chat_voice_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Command",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Tanyakan seputar zakat...", fontSize = 13.sp) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !viewModel.isChatLoading) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        .clickable(enabled = inputText.isNotBlank() && !viewModel.isChatLoading) {
                            viewModel.sendChatMessage(inputText)
                            inputText = ""
                            keyboardController?.hide()
                        }
                        .testTag("chat_send_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Kirim",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, viewModel: ZakatViewModel) {
    val isUser = message.sender == "user"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            // Zavira Avatar Indicator using the Mascot image
            Image(
                painter = painterResource(id = R.drawable.img_zavira_mascot),
                contentDescription = "Zavira Avatar",
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.Top)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Card(
            shape = if (isUser) {
                RoundedCornerShape(topStart = 16.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            } else {
                RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            },
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primary 
                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ),
            modifier = Modifier
                .widthIn(max = 280.dp)
                .testTag(if (isUser) "user_message" else "bot_message")
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = message.text,
                    color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = formatTimestamp(message.timestamp),
                        color = if (isUser) Color.White.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}


// ==================== SCREEN 3: INTERACTIVE CALCULATOR SCREEN ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(viewModel: ZakatViewModel) {
    val calculatorTypes = listOf("Zakat Fitrah", "Zakat Penghasilan", "Zakat Emas / Perak", "Zakat Perdagangan", "Zakat Pertanian")
    var selectedType by remember { mutableStateOf(calculatorTypes[0]) }
    var expandedDropdown by remember { mutableStateOf(false) }

    // Unified inputs
    // Fitrah
    var numFamilyMembers by remember { mutableStateOf("1") }
    var ricePriceStr by remember { mutableStateOf("15000") } // default Rp 15k/kg

    // Penghasilan
    var monthlySalaryStr by remember { mutableStateOf("") }
    var bonusIncomeStr by remember { mutableStateOf("") }
    var monthlyDebtStr by remember { mutableStateOf("") }

    // Emas/Perak
    var goldWeightStr by remember { mutableStateOf("") }
    var goldPriceStr by remember { mutableStateOf("1400000") } // default Rp 1.4jt/gram
    var silverWeightStr by remember { mutableStateOf("") }
    var silverPriceStr by remember { mutableStateOf("20000") } // default Rp 20rb/gram
    var hasHaulReached by remember { mutableStateOf(true) }

    // Perdagangan
    var tradeCapitalStr by remember { mutableStateOf("") }
    var tradeProfitStr by remember { mutableStateOf("") }
    var tradeReceivablesStr by remember { mutableStateOf("") }
    var tradeDebtsStr by remember { mutableStateOf("") }

    // Pertanian
    var cropWeightStr by remember { mutableStateOf("") }
    var cropPriceStr by remember { mutableStateOf("10000") }
    var irrigationType by remember { mutableStateOf("irigasi") } // "irigasi" (5%) or "tadah_hujan" (10%)

    // Results states
    var showResult by remember { mutableStateOf(false) }
    var resultTitle by remember { mutableStateOf("") }
    var resultDescription by remember { mutableStateOf("") }
    var resultZakatAmount by remember { mutableStateOf(0.0) }
    var resultAssetsValue by remember { mutableStateOf(0.0) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Dropdown Selector
        Text(
            text = "Pilih Jenis Zakat",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        ExposedDropdownMenuBox(
            expanded = expandedDropdown,
            onExpandedChange = { expandedDropdown = !expandedDropdown },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .testTag("zakat_type_dropdown"),
                shape = RoundedCornerShape(8.dp)
            )

            ExposedDropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false }
            ) {
                calculatorTypes.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection, fontSize = 13.sp) },
                        onClick = {
                            selectedType = selection
                            expandedDropdown = false
                            showResult = false // reset result on change
                        }
                    )
                }
            }
        }

        // Calculator Inputs container
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Formulir $selectedType",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Dynamic forms based on selection
                when (selectedType) {
                    "Zakat Fitrah" -> {
                        item {
                            OutlinedTextField(
                                value = numFamilyMembers,
                                onValueChange = { numFamilyMembers = it },
                                label = { Text("Jumlah Anggota Keluarga (Jiwa)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_fitrah_jiwa"),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = ricePriceStr,
                                onValueChange = { ricePriceStr = it },
                                label = { Text("Harga Beras per Kg (Rupiah)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_fitrah_beras_price"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                    }

                    "Zakat Penghasilan" -> {
                        item {
                            OutlinedTextField(
                                value = monthlySalaryStr,
                                onValueChange = { monthlySalaryStr = it },
                                label = { Text("Penghasilan Bulanan (Gaji Pokok)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_gaji"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = bonusIncomeStr,
                                onValueChange = { bonusIncomeStr = it },
                                label = { Text("Bonus / Pendapatan Lain Bulanan") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_bonus"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = monthlyDebtStr,
                                onValueChange = { monthlyDebtStr = it },
                                label = { Text("Hutang / Cicilan Kebutuhan Pokok") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_hutang"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                    }

                    "Zakat Emas / Perak" -> {
                        item {
                            OutlinedTextField(
                                value = goldWeightStr,
                                onValueChange = { goldWeightStr = it },
                                label = { Text("Berat Emas yang Dimiliki (Gram)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_berat_emas"),
                                shape = RoundedCornerShape(8.dp),
                                suffix = { Text("gr") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = goldPriceStr,
                                onValueChange = { goldPriceStr = it },
                                label = { Text("Harga Emas per Gram (Rupiah)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_harga_emas"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = silverWeightStr,
                                onValueChange = { silverWeightStr = it },
                                label = { Text("Berat Perak yang Dimiliki (Gram)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_berat_perak"),
                                shape = RoundedCornerShape(8.dp),
                                suffix = { Text("gr") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = silverPriceStr,
                                onValueChange = { silverPriceStr = it },
                                label = { Text("Harga Perak per Gram (Rupiah)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_harga_perak"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Aset telah disimpan selama 1 tahun (Haul)?", fontSize = 12.sp, modifier = Modifier.weight(0.7f))
                                Switch(
                                    checked = hasHaulReached,
                                    onCheckedChange = { hasHaulReached = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.testTag("switch_haul")
                                )
                            }
                        }
                    }

                    "Zakat Perdagangan" -> {
                        item {
                            OutlinedTextField(
                                value = tradeCapitalStr,
                                onValueChange = { tradeCapitalStr = it },
                                label = { Text("Modal Dagang / Stok Barang") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_modal_dagang"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = tradeProfitStr,
                                onValueChange = { tradeProfitStr = it },
                                label = { Text("Keuntungan Dagang") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_keuntungan"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = tradeReceivablesStr,
                                onValueChange = { tradeReceivablesStr = it },
                                label = { Text("Piutang Lancar") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_piutang"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = tradeDebtsStr,
                                onValueChange = { tradeDebtsStr = it },
                                label = { Text("Hutang yang Jatuh Tempo") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_hutang_dagang"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = goldPriceStr,
                                onValueChange = { goldPriceStr = it },
                                label = { Text("Harga Emas Acuan per Gram") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                    }

                    "Zakat Pertanian" -> {
                        item {
                            OutlinedTextField(
                                value = cropWeightStr,
                                onValueChange = { cropWeightStr = it },
                                label = { Text("Hasil Panen Pokok (Kg / Gabah)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_hasil_panen"),
                                shape = RoundedCornerShape(8.dp),
                                suffix = { Text("Kg") }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = cropPriceStr,
                                onValueChange = { cropPriceStr = it },
                                label = { Text("Harga Hasil Panen per Kg") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth().testTag("input_harga_panen"),
                                shape = RoundedCornerShape(8.dp),
                                prefix = { Text("Rp ") }
                            )
                        }
                        item {
                            Text("Sistem Pengairan Sawah / Kebun", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = irrigationType == "irigasi",
                                        onClick = { irrigationType = "irigasi" },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("radio_irigasi")
                                    )
                                    Text("Irigasi / Berbayar (Zakat 5%)", fontSize = 12.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = irrigationType == "tadah_hujan",
                                        onClick = { irrigationType = "tadah_hujan" },
                                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.testTag("radio_tadah_hujan")
                                    )
                                    Text("Tadah Hujan / Alami (Zakat 10%)", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Calculate Action Row
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            when (selectedType) {
                                "Zakat Fitrah" -> {
                                    val j = numFamilyMembers.toIntOrNull() ?: 0
                                    val hp = ricePriceStr.toDoubleOrNull() ?: 0.0
                                    val r = viewModel.calculateFitrah(j, hp)
                                    resultTitle = "Zakat Fitrah"
                                    resultAssetsValue = r.uangRupiah
                                    resultZakatAmount = r.uangRupiah
                                    resultDescription = "Untuk $j Jiwa anggota keluarga.\n" +
                                            "Besaran Beras: ${r.berasKg} Kg (atau ${r.berasLiter} Liter).\n" +
                                            "Jika diuangkan: ${formatRupiah(r.uangRupiah)} (asumsi ${formatRupiah(hp)}/Kg)."
                                    showResult = true
                                }

                                "Zakat Penghasilan" -> {
                                    val sal = monthlySalaryStr.toDoubleOrNull() ?: 0.0
                                    val bon = bonusIncomeStr.toDoubleOrNull() ?: 0.0
                                    val debt = monthlyDebtStr.toDoubleOrNull() ?: 0.0
                                    val hp = ricePriceStr.toDoubleOrNull() ?: 13000.0
                                    val r = viewModel.calculatePenghasilan(sal, bon, debt, hp)
                                    resultTitle = "Zakat Penghasilan"
                                    resultAssetsValue = r.pendapatanBersih
                                    resultZakatAmount = r.zakatAmount
                                    resultDescription = "Total Pendapatan: ${formatRupiah(r.totalPendapatan)}\n" +
                                            "Bersih setelah cicilan: ${formatRupiah(r.pendapatanBersih)}\n" +
                                            "Nisab bulanan acuan: ${formatRupiah(r.nisabBulanan)} (setara 522 kg beras).\n" +
                                            "Status: ${if (r.isWajibZakat) "Wajib mengeluarkan Zakat Penghasilan sebesar 2.5%." else "Belum mencapai Nisab. Sunnah bersedekah."}"
                                    showResult = true
                                }

                                "Zakat Emas / Perak" -> {
                                    val ge = goldWeightStr.toDoubleOrNull() ?: 0.0
                                    val gp = goldPriceStr.toDoubleOrNull() ?: 0.0
                                    val se = silverWeightStr.toDoubleOrNull() ?: 0.0
                                    val sp = silverPriceStr.toDoubleOrNull() ?: 0.0
                                    val r = viewModel.calculateEmasPerak(ge, gp, se, sp, hasHaulReached)
                                    resultTitle = "Zakat Emas / Perak"
                                    resultAssetsValue = r.nilaiEmas + r.nilaiPerak
                                    resultZakatAmount = r.totalZakat
                                    resultDescription = "Detail Kepemilikan Aset:\n" +
                                            "- Emas: $ge gram (${formatRupiah(r.nilaiEmas)}), Nisab: 85g. Wajib Zakat? ${if (r.wajibEmas) "Ya" else "Tidak"}.\n" +
                                            "- Perak: $se gram (${formatRupiah(r.nilaiPerak)}), Nisab: 595g. Wajib Zakat? ${if (r.wajibPerak) "Ya" else "Tidak"}.\n" +
                                            "Status Haul: ${if (hasHaulReached) "Telah mencapai haul 1 tahun." else "Belum haul 1 tahun."}"
                                    showResult = true
                                }

                                "Zakat Perdagangan" -> {
                                    val cap = tradeCapitalStr.toDoubleOrNull() ?: 0.0
                                    val prof = tradeProfitStr.toDoubleOrNull() ?: 0.0
                                    val rec = tradeReceivablesStr.toDoubleOrNull() ?: 0.0
                                    val debt = tradeDebtsStr.toDoubleOrNull() ?: 0.0
                                    val gp = goldPriceStr.toDoubleOrNull() ?: 1400000.0
                                    val r = viewModel.calculatePerdagangan(cap, prof, rec, debt, gp)
                                    resultTitle = "Zakat Perdagangan"
                                    resultAssetsValue = r.asetBersih
                                    resultZakatAmount = r.zakatAmount
                                    resultDescription = "Bersih Kekayaan Perdagangan: ${formatRupiah(r.asetBersih)}\n" +
                                            "Nisab acuan (85g emas): ${formatRupiah(r.nisabRupiah)}\n" +
                                            "Status: ${if (r.wajibZakat) "Wajib mengeluarkan zakat perdagangan sebesar 2.5%." else "Belum mencapai Nisab perdagangan."}"
                                    showResult = true
                                }

                                "Zakat Pertanian" -> {
                                    val crop = cropWeightStr.toDoubleOrNull() ?: 0.0
                                    val cp = cropPriceStr.toDoubleOrNull() ?: 0.0
                                    val r = viewModel.calculatePertanian(crop, cp, irrigationType)
                                    resultTitle = "Zakat Pertanian"
                                    resultAssetsValue = r.totalNilaiPanen
                                    resultZakatAmount = r.zakatAmount
                                    val pct = if (r.tarifZakat == 0.05) "5%" else "10%"
                                    resultDescription = "Total Hasil Panen: $crop Kg (Setara ${formatRupiah(r.totalNilaiPanen)})\n" +
                                            "Nisab panen (653 Kg): ${if (r.wajibZakat) "Terlampaui" else "Belum tercapai"}.\n" +
                                            "Sistem Pengairan: $irrigationType (Tarif Zakat $pct).\n" +
                                            "Status: ${if (r.wajibZakat) "Wajib zakat $pct hasil panen." else "Belum wajib zakat pertanian."}"
                                    showResult = true
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("calculate_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Calculate, contentDescription = null)
                            Text("Hitung Zakat Sekarang", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                // Result Block inside the scroll
                if (showResult) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("result_card"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (resultZakatAmount > 0) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (resultZakatAmount > 0) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (resultZakatAmount > 0) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(
                                        text = "Hasil Perhitungan: $resultTitle",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Divider()

                                Text(
                                    text = resultDescription,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("Kewajiban Zakat Anda:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = if (resultZakatAmount > 0) formatRupiah(resultZakatAmount) else "Rp 0",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (resultZakatAmount > 0) MaterialTheme.colorScheme.primary else Color.Gray
                                        )
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.saveCalculation(
                                                type = resultTitle,
                                                totalWealth = resultAssetsValue,
                                                calculatedZakat = resultZakatAmount,
                                                description = resultDescription
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("save_calculation_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                            Text("Simpan", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==================== SCREEN 4: HISTORY SCREEN ====================
@Composable
fun HistoryScreen(viewModel: ZakatViewModel) {
    val calculations by viewModel.savedCalculations.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Riwayat Perhitungan Zakat",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${calculations.size} Tersimpan",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (calculations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Belum Ada Riwayat Perhitungan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Hitung zakat Anda di tab Kalkulator lalu ketuk tombol 'Simpan' untuk mencatat di sini.",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(calculations) { calculation ->
                    HistoryCard(calculation = calculation, onDelete = { viewModel.deleteCalculation(calculation.id) })
                }
            }
        }
    }
}

@Composable
fun HistoryCard(calculation: ZakatCalculation, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_${calculation.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = calculation.zakatType,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("delete_calculation_button_${calculation.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Divider()

            Text(
                text = calculation.description,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Zakat yang Wajib Dikeluarkan:",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = if (calculation.calculatedZakat > 0) formatRupiah(calculation.calculatedZakat) else "Rp 0 (Belum Nisab)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (calculation.calculatedZakat > 0) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }

                Text(
                    text = formatTimestamp(calculation.timestamp),
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}
