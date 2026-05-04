package com.modih.mail.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.modih.mail.data.local.PreferencesManager
import com.modih.mail.data.model.Plan
import com.modih.mail.data.repository.MailRepository
import kotlinx.coroutines.tasks.await

data class UserPlanState(
    val isLoggedIn: Boolean = false,
    val plan: Plan = Plan.FREE,
    val email: String? = null,
    val isLoading: Boolean = false
)

@Composable
fun rememberUserPlanState(): UserPlanState {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val repo = remember { MailRepository() }
    val prefs = remember { PreferencesManager(context.applicationContext) }
    val savedUser by prefs.userPlanFlow.collectAsState(initial = null)
    val savedPlan = Plan.fromString(savedUser?.plan ?: "free")

    var state by remember {
        mutableStateOf(
            UserPlanState(
                isLoggedIn = auth.currentUser != null,
                plan = savedPlan,
                email = savedUser?.email ?: auth.currentUser?.email,
                isLoading = auth.currentUser != null
            )
        )
    }

    LaunchedEffect(
        auth.currentUser?.uid,
        savedUser?.plan,
        savedUser?.email
    ) {
        state = UserPlanState(
            isLoggedIn = auth.currentUser != null,
            plan = savedPlan,
            email = savedUser?.email ?: auth.currentUser?.email,
            isLoading = auth.currentUser != null
        )

        val user = auth.currentUser
        if (user == null) {
            prefs.clearUser()
            state = UserPlanState()
            return@LaunchedEffect
        }

        state = UserPlanState(
            isLoggedIn = true,
            plan = savedPlan,
            email = user.email ?: savedUser?.email,
            isLoading = true
        )

        try {
            val authToken = user.getIdToken(false).await()?.token
            if (authToken.isNullOrBlank()) {
                state = state.copy(isLoading = false)
                return@LaunchedEffect
            }

            val result = repo.getUserProfile(authToken)
            val profile = result.getOrNull()
            if (profile != null) {
                val resolvedEmail = profile.email.ifBlank { user.email.orEmpty() }
                state = UserPlanState(
                    isLoggedIn = true,
                    plan = profile.plan,
                    email = resolvedEmail,
                    isLoading = false
                )
                prefs.saveUserPlan(profile.uid, resolvedEmail, profile.plan.name.lowercase())
            } else {
                state = UserPlanState(
                    isLoggedIn = true,
                    plan = savedPlan,
                    email = user.email ?: savedUser?.email,
                    isLoading = false
                )
            }
        } catch (_: Exception) {
            state = UserPlanState(
                isLoggedIn = true,
                plan = savedPlan,
                email = user.email ?: savedUser?.email,
                isLoading = false
            )
        }
    }

    return state
}
