package com.lucafilippi.universebible

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lucafilippi.universebible.ui.navigation.UniverseBibleScreen
import com.lucafilippi.universebible.ui.theme.UniverseBibleTheme
import com.lucafilippi.universebible.ui.viewmodel.UniversesViewModel

class MainActivity : ComponentActivity() {
    private val universesViewModel by viewModels<UniversesViewModel> {
        viewModelFactory {
            initializer { UniversesViewModel((application as UniverseBibleApplication).universeRepository) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        setContent {
            UniverseBibleTheme { UniverseBibleScreen(universesViewModel) }
        }
    }
}
