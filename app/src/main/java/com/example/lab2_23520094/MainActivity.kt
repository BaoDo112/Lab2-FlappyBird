package com.example.lab2_23520094

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.lab2_23520094.ui.theme.Lab2_23520094Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab2_23520094Theme {
                AndroidView(
                    factory = { context ->
                        GameView(context)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
