package com.controlparental.kioscosuave

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.controlparental.kioscosuave.ui.HomeScreen
import com.controlparental.kioscosuave.ui.KioskScreen
import com.controlparental.kioscosuave.ui.KioscoTheme

/**
 * Actividad HOME/Launcher. Decide qué mostrar:
 *  - Sin configurar  -> pantalla que invita a la configuración parental.
 *  - Bloqueado        -> kiosco de desafíos.
 *  - Desbloqueado     -> launcher simple con las apps del dispositivo.
 */
class LauncherActivity : ComponentActivity() {

    // Se incrementa en onResume para forzar re-lectura del estado tras volver.
    private val refreshTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KioskForegroundService.start(this)

        if (SessionStateMachine.isLocked(this)) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Bloquear "Atrás" mientras el día esté bloqueado.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Consumir siempre: como somos HOME no hay "atrás" útil.
            }
        })

        setContent { KioscoTheme { Root() } }
    }

    override fun onResume() {
        super.onResume()
        refreshTick.intValue++
    }

    @Composable
    private fun Root() {
        val tick = refreshTick.intValue
        val configured = remember(tick) { ProfileStore.isConfigured(this) }
        var locked by remember(tick) { mutableStateOf(SessionStateMachine.isLocked(this)) }

        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                !configured -> NeedsSetupScreen()
                locked -> KioskScreen(
                    profile = ProfileStore.getProfile(this),
                    onAllComplete = {
                        SessionStateMachine.setUnlocked(this)
                        locked = false
                    },
                    onParentAccess = { openParentSetup() }
                )
                else -> HomeScreen(
                    childName = ProfileStore.getProfile(this).name,
                    onLockAgain = {
                        SessionStateMachine.setLocked(this)
                        locked = true
                    },
                    onParentAccess = { openParentSetup() }
                )
            }
        }
    }

    @Composable
    private fun NeedsSetupScreen() {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Kiosco Suave", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Configura el perfil del niño para comenzar.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            Button(onClick = { openParentSetup() }) {
                Text("Configuración parental")
            }
        }
    }

    private fun openParentSetup() {
        startActivity(Intent(this, ParentSetupActivity::class.java))
    }
}
