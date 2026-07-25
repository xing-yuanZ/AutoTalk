package com.autotalk.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.autotalk.app.ui.navigation.AppNavigation
import com.autotalk.app.ui.theme.AutoTalkTheme

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer 未提供")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as AutoTalkApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AutoTalkTheme {
                    AppNavigation()
                }
            }
        }
    }
}
