package com.dxtr.routini

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import androidx.navigation.compose.rememberNavController
import com.dxtr.routini.ui.navigation.RoutiniNavHost
import com.dxtr.routini.ui.theme.RoutiniTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start the Keep-Alive Service
        val serviceIntent = android.content.Intent(this, com.dxtr.routini.service.RoutiniService::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        enableEdgeToEdge()

        setContent {
            RoutiniTheme {
                val navController = rememberNavController()
                Scaffold { innerPadding ->
                     RoutiniNavHost(navController = navController)
                }
            }
        }
    }
}