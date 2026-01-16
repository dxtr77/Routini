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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Service Removed: "Keep-Alive" service is no longer needed as AlarmManager handles wakeups.

        enableEdgeToEdge()

        setContent {
            RoutiniTheme {
                val navController = rememberNavController()
                Scaffold { innerPadding ->
                     RoutiniNavHost(
                         navController = navController,
                         modifier = Modifier.padding(innerPadding)
                     )
                }
            }
        }
    }
}