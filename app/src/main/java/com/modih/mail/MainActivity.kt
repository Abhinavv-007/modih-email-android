package com.modih.mail

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.modih.mail.ui.navigation.ModihBottomBar
import com.modih.mail.ui.navigation.ModihNavHost
import com.modih.mail.ui.components.SyncSignedInUserPlanEffect
import com.modih.mail.ui.theme.BgPrimary
import com.modih.mail.ui.theme.ModihMailTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ModihMailTheme {
                SyncSignedInUserPlanEffect()
                val navController = rememberNavController()

                Scaffold(
                    containerColor = BgPrimary,
                    bottomBar = {
                        ModihBottomBar(navController = navController)
                    }
                ) { innerPadding ->
                    ModihNavHost(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}
