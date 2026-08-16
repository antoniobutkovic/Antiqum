package com.strive.antiqum.museums.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumDimens
import com.strive.antiqum.onboarding.ui.SignInOptions
import com.strive.antiqum.profile.data.SignInProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MuseumSignInSheet(
    action: MuseumProfileAction,
    showAppleSignIn: Boolean,
    onSignIn: (SignInProvider) -> Unit,
    onDismiss: () -> Unit
) {
    val title = when (action) {
        MuseumProfileAction.Save -> "Sign in to save this museum"
        MuseumProfileAction.MarkVisited -> "Sign in to mark this museum as visited"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier.padding(
                start = AntiqumDimens.ScreenPadding,
                end = AntiqumDimens.ScreenPadding,
                bottom = 20.dp
            )
        ) {
            Text(title, style = MaterialTheme.typography.displayMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Your saved museums and visited places will be kept with your account.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(22.dp))
            SignInOptions(
                showAppleSignIn = showAppleSignIn,
                onSignIn = onSignIn
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally).fillMaxWidth()
            ) {
                Text("Not now")
            }
        }
    }
}
