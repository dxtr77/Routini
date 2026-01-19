package com.dxtr.routini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dxtr.routini.ui.navigation.RoutiniNavHost
import com.dxtr.routini.ui.theme.RoutiniTheme
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        handleIntent(intent)

        enableEdgeToEdge()

        setContent {
            RoutiniTheme {
                val navController = rememberNavController()
                RoutiniNavHost(
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: android.content.Intent?) {
        if (intent?.action == "com.dxtr.routini.ACTION_ADD_TASK") {
            viewModel.triggerAddTaskDialog(true)
        }
    }
}