package com.arda.cineverse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.arda.cineverse.navigation.CineVerseNavGraph
import com.arda.cineverse.ui.theme.CineVerseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CineVerseTheme {
                CineVerseNavGraph()
            }
        }
    }
}
