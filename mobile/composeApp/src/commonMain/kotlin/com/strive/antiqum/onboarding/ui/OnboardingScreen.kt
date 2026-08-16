package com.strive.antiqum.onboarding.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.strive.antiqum.designsystem.AntiqumColors
import com.strive.antiqum.designsystem.AntiqumPrimaryButton
import com.strive.antiqum.profile.data.SignInProvider

private const val PAGE_COUNT = 3

@Composable
fun OnboardingScreen(
    showAppleSignIn: Boolean,
    onSignIn: (SignInProvider) -> Unit,
    onSkip: () -> Unit
) {
    var page by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (page > 0) {
                IconButton(onClick = { page -= 1 }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Previous tutorial page")
                }
            } else {
                Spacer(Modifier.size(48.dp))
            }
            Spacer(Modifier.weight(1f))
            TutorialProgress(currentPage = page)
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(28.dp))
        when (page) {
            0 -> TutorialPage(
                icon = Icons.Outlined.Explore,
                accentIcon = Icons.Outlined.Map,
                eyebrow = "DISCOVER",
                title = "Museums worth\na detour",
                body = "Explore museums around you, browse collections worldwide, and open every place directly on the map."
            )
            1 -> TutorialPage(
                icon = Icons.Outlined.FavoriteBorder,
                accentIcon = Icons.Outlined.CheckCircleOutline,
                eyebrow = "REMEMBER",
                title = "Build your\ncultural trail",
                body = "Save the museums you love and mark the places you have visited, so your next discovery is always easy to find."
            )
            else -> AccountTutorialPage(
                showAppleSignIn = showAppleSignIn,
                onSignIn = onSignIn
            )
        }

        Spacer(Modifier.weight(1f))
        if (page < PAGE_COUNT - 1) {
            AntiqumPrimaryButton(
                label = "Continue",
                onClick = { page += 1 },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            OutlinedButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text("Skip for now", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun TutorialProgress(currentPage: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        repeat(PAGE_COUNT) { index ->
            Box(
                Modifier
                    .size(width = if (index == currentPage) 25.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    )
            )
        }
    }
}

@Composable
private fun TutorialPage(
    icon: ImageVector,
    accentIcon: ImageVector,
    eyebrow: String,
    title: String,
    body: String
) {
    TutorialArtwork(icon = icon, accentIcon = accentIcon)
    Spacer(Modifier.height(34.dp))
    Text(
        eyebrow,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelSmall
    )
    Spacer(Modifier.height(10.dp))
    Text(
        title,
        color = MaterialTheme.colorScheme.onBackground,
        style = MaterialTheme.typography.displayLarge,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(14.dp))
    Text(
        body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun TutorialArtwork(icon: ImageVector, accentIcon: ImageVector) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        AntiqumColors.Bronze.copy(alpha = 0.34f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(142.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(70.dp)
            )
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(32.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                accentIcon,
                contentDescription = null,
                tint = AntiqumColors.Bronze,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AccountTutorialPage(
    showAppleSignIn: Boolean,
    onSignIn: (SignInProvider) -> Unit
) {
    Icon(
        Icons.Outlined.AccountCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(92.dp)
    )
    Spacer(Modifier.height(20.dp))
    Text(
        "YOUR PROFILE",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelSmall
    )
    Spacer(Modifier.height(10.dp))
    Text(
        "Keep your discoveries\nwith you",
        style = MaterialTheme.typography.displayLarge,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(12.dp))
    Text(
        "Sign in to keep a personal profile with your favorite museums and visited places. You can also continue without an account.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    SignInOptions(
        showAppleSignIn = showAppleSignIn,
        onSignIn = onSignIn
    )
}

@Composable
fun SignInOptions(
    showAppleSignIn: Boolean,
    onSignIn: (SignInProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProviderButton(
            label = "Continue with Google",
            mark = "G",
            onClick = { onSignIn(SignInProvider.Google) }
        )
        if (showAppleSignIn) {
            ProviderButton(
                label = "Continue with Apple",
                mark = "A",
                onClick = { onSignIn(SignInProvider.Apple) }
            )
        }
    }
}

@Composable
private fun ProviderButton(label: String, mark: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(999.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(mark, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.size(10.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}
