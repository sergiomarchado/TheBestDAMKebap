package com.sergiom.thebestdamkebap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sergiom.thebestdamkebap.navigation.AppNavHost
import com.sergiom.thebestdamkebap.ui.theme.TheBestDAMKebapTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TheBestDAMKebapTheme {
                AppNavHost()
            }
        }
    }
}
