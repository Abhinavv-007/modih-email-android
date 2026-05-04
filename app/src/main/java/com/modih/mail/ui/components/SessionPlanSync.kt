package com.modih.mail.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.repository.MailRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

suspend fun syncSignedInUserPlan(
    context: Context,
    auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val appContext = context.applicationContext
    val prefs = PreferencesManager(appContext)
    val user = auth.currentUser

    if (user == null) {
        prefs.clearUser()
        return
    }

    val fallbackEmail = user.email.orEmpty()
    val existingUser = prefs.userPlanFlow.first()
    val fallbackPlan = existingUser
        ?.takeIf { it.uid == user.uid }
        ?.plan
        ?: "free"
    val authToken = user.getIdToken(false).await()?.token
    if (authToken.isNullOrBlank()) {
        prefs.saveUserPlan(user.uid, fallbackEmail, fallbackPlan)
        return
    }

    val profile = MailRepository().getUserProfile(authToken).getOrNull()
    if (profile != null) {
        prefs.saveUserPlan(
            profile.uid.ifBlank { user.uid },
            profile.email.ifBlank { fallbackEmail },
            profile.plan.name.lowercase()
        )
    } else {
        prefs.saveUserPlan(user.uid, fallbackEmail, fallbackPlan)
    }
}

@Composable
fun SyncSignedInUserPlanEffect() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUserUid by remember { mutableStateOf(auth.currentUser?.uid) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUserUid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(currentUserUid) {
        syncSignedInUserPlan(context, auth)
    }
}
