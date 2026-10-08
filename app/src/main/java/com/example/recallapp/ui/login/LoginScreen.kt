package com.example.recallapp.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.recallapp.ui.theme.Lab
import com.example.recallapp.ui.theme.RecallTheme

/**
 * Landing / login screen. UI only: the caller decides what the sign in button does.
 *
 * @param loading shows a spinner instead of the button while signing in
 * @param error message shown under the button, or null
 * @param onSignInClick called when the user taps "Sign in with Google"
 */
@Composable
fun LoginScreen(
    loading: Boolean,
    error: String?,
    onSignInClick: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(Lab.Bg).systemBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(14.dp).background(Lab.Accent))
            Spacer(Modifier.width(10.dp))
            Text("BRAINWORM", style = MaterialTheme.typography.headlineSmall, color = Lab.TealDark)
        }

        Column(
            Modifier.fillMaxWidth().background(Lab.Teal, RoundedCornerShape(6.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("FDA FOOD RECALLS", style = MaterialTheme.typography.labelMedium, color = Lab.Mint)
            Text("IS YOUR\nFOOD SAFE\nTODAY?", style = MaterialTheme.typography.displayLarge, color = Color.White)
            Text(
                "Add the foods you buy. If the FDA recalls one, you will see it here first.",
                style = MaterialTheme.typography.bodyLarge,
                color = Lab.Mint,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StepRow("1", "Add your foods", "Type the product name and batch if you have it.")
            StepRow("2", "We check the FDA", "New recalls are matched to your items.")
            StepRow("3", "Get the alert", "See what is wrong and what to do with it.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (loading) {
                Box(Modifier.fillMaxWidth().height(56.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Lab.Teal)
                }
            } else {
                Button(
                    onClick = onSignInClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = Lab.AccentDark, contentColor = Color.White),
                ) {
                    Text("Sign in with Google", style = MaterialTheme.typography.labelLarge)
                }
            }
            error?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Lab.Hazard) }
            Text(
                "We use your Google account to sign in. We never see your password.",
                style = MaterialTheme.typography.bodySmall,
                color = Lab.Muted,
            )
        }
    }
}

@Composable
private fun StepRow(number: String, title: String, body: String) {
    Row(
        Modifier.fillMaxWidth().background(Lab.Surface, RoundedCornerShape(4.dp)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(Lab.Mint, RoundedCornerShape(2.dp)), contentAlignment = Alignment.Center) {
            Text(number, style = MaterialTheme.typography.headlineSmall, color = Lab.TealDark)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Lab.Ink)
            Text(body, style = MaterialTheme.typography.bodySmall, color = Lab.Muted)
        }
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun LoginScreenPreview() {
    RecallTheme { LoginScreen(loading = false, error = null, onSignInClick = {}) }
}