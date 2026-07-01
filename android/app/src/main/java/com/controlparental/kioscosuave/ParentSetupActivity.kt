package com.controlparental.kioscosuave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.controlparental.kioscosuave.ui.KioscoTheme

/**
 * Configuración parental protegida por PIN. Primera vez: crea el perfil y el PIN.
 * Después: exige el PIN para volver a editar (así el niño no cambia su nivel).
 */
class ParentSetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KioscoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var authenticated by remember { mutableStateOf(!ProfileStore.hasPin(this)) }
                    if (authenticated) {
                        SetupForm()
                    } else {
                        PinGate(onSuccess = { authenticated = true })
                    }
                }
            }
        }
    }

    @Composable
    private fun PinGate(onSuccess: () -> Unit) {
        var pin by remember { mutableStateOf("") }
        var error by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Ingresa el PIN de padres", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it; error = false },
                label = { Text("PIN") },
                isError = error,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            if (error) Text("PIN incorrecto", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (ProfileStore.verifyPin(this@ParentSetupActivity, pin)) onSuccess()
                    else error = true
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Entrar") }
        }
    }

    @Composable
    private fun SetupForm() {
        val ctx = this
        val existing = remember { if (ProfileStore.isConfigured(ctx)) ProfileStore.getProfile(ctx) else null }
        var name by remember { mutableStateOf(existing?.name ?: "") }
        var grade by remember { mutableStateOf(existing?.grade ?: GradeLevel.PRIMARIA) }
        var pin by remember { mutableStateOf("") }
        var blockSettings by remember { mutableStateOf(ProfileStore.blockSettings(ctx)) }
        var emergencyCalls by remember { mutableStateOf(ProfileStore.emergencyCalls(ctx)) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Text("Configuración del niño", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del niño") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Text("Nivel escolar", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GradeLevel.entries.forEach { g ->
                    if (grade == g) {
                        Button(onClick = { grade = g }, modifier = Modifier.weight(1f)) { Text(g.label) }
                    } else {
                        OutlinedButton(onClick = { grade = g }, modifier = Modifier.weight(1f)) { Text(g.label) }
                    }
                }
            }
            Text(
                "La dificultad se ajusta sola: Primaria = fácil, Secundaria = difícil.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text(if (existing != null) "Nuevo PIN (4+ dígitos)" else "Crear PIN (4+ dígitos)") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            SwitchRow("Bloquear Ajustes del sistema", blockSettings) { blockSettings = it }
            SwitchRow("Permitir llamadas de emergencia", emergencyCalls) { emergencyCalls = it }

            errorMsg?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    when {
                        name.isBlank() -> errorMsg = "Escribe el nombre del niño."
                        existing == null && pin.length < 4 ->
                            errorMsg = "El PIN debe tener al menos 4 dígitos."
                        pin.isNotBlank() && pin.length < 4 ->
                            errorMsg = "El nuevo PIN debe tener al menos 4 dígitos."
                        else -> {
                            if (pin.isBlank() && existing != null) {
                                // Edición sin cambiar el PIN existente.
                                ProfileStore.updateProfileKeepPin(
                                    ctx, name, grade, blockSettings, emergencyCalls
                                )
                            } else {
                                ProfileStore.saveProfile(
                                    ctx, name, grade, pin, blockSettings, emergencyCalls
                                )
                            }
                            KioskForegroundService.start(ctx)
                            finish()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar y activar kiosco") }
        }
    }

    @Composable
    private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
