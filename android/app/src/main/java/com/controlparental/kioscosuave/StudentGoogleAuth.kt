package com.controlparental.kioscosuave

import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Identifica al alumno con el correo autorizado por el padre en el panel.
 * El documento del hijo lo crea el padre; una tablet nunca inventa perfiles.
 */
object StudentGoogleAuth {
    fun connect(activity: ComponentActivity, onResult: (String?) -> Unit) {
        val clientId = BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        if (clientId.isBlank()) {
            onResult("Falta GOOGLE_WEB_CLIENT_ID en local.properties. Descarga la configuración Android actualizada de Firebase.")
            return
        }
        activity.lifecycleScope.launch {
            try {
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()
                val result = CredentialManager.create(activity).getCredential(
                    activity, GetCredentialRequest.Builder().addCredentialOption(option).build()
                )
                val credential = result.credential as? CustomCredential
                if (credential?.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    onResult("No se recibió una cuenta Google válida.")
                    return@launch
                }
                val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
                FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .addOnSuccessListener { authResult ->
                        val email = authResult.user?.email?.trim()?.lowercase()
                        if (email.isNullOrBlank()) {
                            onResult("La cuenta Google no tiene correo disponible.")
                            return@addOnSuccessListener
                        }
                        FirebaseFirestore.getInstance().collection("children")
                            .whereEqualTo("authorizedEmail", email).limit(2).get()
                            .addOnSuccessListener { profiles ->
                                when (profiles.size()) {
                                    1 -> {
                                        val profile = profiles.documents.first()
                                        if (profile.getBoolean("active") == false || profile.getBoolean("archived") == true) {
                                            onResult("Este perfil fue eliminado o está desactivado desde el Panel de Padres.")
                                            return@addOnSuccessListener
                                        }
                                        ProfileStore.setCloudChildId(activity, profile.id)
                                        ProgressSync.registerChild(activity)
                                        onResult(null)
                                    }
                                    0 -> onResult("Esta cuenta no está autorizada. Agrégala primero en el Panel de Padres.")
                                    else -> onResult("Ese correo está asociado a más de un perfil. Corrígelo en el Panel de Padres.")
                                }
                            }
                            .addOnFailureListener { onResult("No se pudo validar la cuenta: ${it.message}") }
                    }
                    .addOnFailureListener { onResult("No se pudo iniciar sesión: ${it.message}") }
            } catch (e: Exception) {
                onResult("No se pudo abrir el selector de Google: ${e.message}")
            }
        }
    }
}
