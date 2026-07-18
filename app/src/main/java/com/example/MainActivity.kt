package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.ZaviraApp
import com.example.ui.ZakatViewModel
import com.example.ui.ZakatViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the ViewModel with our custom factory passing Application context
        val viewModel: ZakatViewModel by viewModels {
            ZakatViewModelFactory(application)
        }

        setContent {
            MyApplicationTheme {
                ZaviraApp(viewModel = viewModel)
            }
        }
    }
}
