package com.snapcabin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.rememberScreenClass
import com.snapcabin.ui.components.scaledDp
import com.snapcabin.ui.screens.admin.sections.AboutSection
import com.snapcabin.ui.screens.admin.sections.AuditSection
import com.snapcabin.ui.screens.admin.sections.BrandingSection
import com.snapcabin.ui.screens.admin.sections.CameraSection
import com.snapcabin.ui.screens.admin.sections.CaptureSection
import com.snapcabin.ui.screens.admin.sections.CloudinarySection
import com.snapcabin.ui.screens.admin.sections.EventSection
import com.snapcabin.ui.screens.admin.sections.KioskSection
import com.snapcabin.ui.screens.admin.sections.ModesSection
import com.snapcabin.ui.screens.admin.sections.ShareSection
import com.snapcabin.ui.screens.admin.sections.SoundSection
import com.snapcabin.ui.screens.admin.sections.ToolsSection
import com.snapcabin.ui.screens.admin.sections.TwilioSection
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.FrankRuhlLibre
import com.snapcabin.ui.theme.HankenGrotesk
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Oat
import com.snapcabin.ui.theme.Parchment
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing
import kotlinx.coroutines.launch

private const val SETUP_GUIDE_TWILIO = "https://snapcabin.app/setup/twilio"
private const val SETUP_GUIDE_CLOUDINARY = "https://snapcabin.app/setup/cloudinary"

@Composable
fun AdminScreen(
    onDismiss: () -> Unit,
    onPrivacyPolicy: () -> Unit = {},
    onGallery: () -> Unit = {},
    viewModel: AdminViewModel
) {
    val pinVerified by viewModel.pinVerified.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val cameras by viewModel.availableCameras.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Parchment)
    ) {
        if (!pinVerified) {
            PinEntry(
                onPinSubmit = { viewModel.verifyPin(it) },
                onCancel = onDismiss,
                showDefaultHint = settings.adminPin == "1234"
            )
        } else {
            val sections = listOf(
                AdminSection("event", "EVENT") {
                    EventSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("camera", "CAMERA") {
                    CameraSection(settings = settings, cameras = cameras, viewModel = viewModel)
                },
                AdminSection("modes", "MODES") {
                    ModesSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("capture", "CAPTURE") {
                    CaptureSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("sound", "SOUND") {
                    SoundSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("share", "SHARE OPTIONS") {
                    ShareSection(settings = settings, viewModel = viewModel)
                },
                AdminSection(
                    key = "twilio",
                    label = "TWILIO SMS",
                    helpUrl = SETUP_GUIDE_TWILIO
                ) {
                    TwilioSection(settings = settings, viewModel = viewModel)
                },
                AdminSection(
                    key = "cloudinary",
                    label = "CLOUDINARY PHOTO HOSTING",
                    helpUrl = SETUP_GUIDE_CLOUDINARY
                ) {
                    CloudinarySection(settings = settings, viewModel = viewModel)
                },
                AdminSection("kiosk", "KIOSK") {
                    KioskSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("branding", "BRANDING") {
                    BrandingSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("audit", "AUDIT LOG") {
                    AuditSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("tools", "TOOLS") {
                    ToolsSection(onGallery = onGallery)
                },
                AdminSection("about", "ABOUT") {
                    AboutSection(onPrivacyPolicy = onPrivacyPolicy)
                }
            )

            AdminContent(
                sections = sections,
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
private fun AdminContent(
    sections: List<AdminSection>,
    onDismiss: () -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // The LazyColumn has a title item at index 0 and a close button at the end.
    // Section indices in the side nav are offset by 1.
    val titleOffset = 1
    val activeSectionIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            (first - titleOffset).coerceIn(0, sections.size - 1)
        }
    }

    val screen = rememberScreenClass()
    val navWidth = screen.scaledDp(240).dp

    Row(modifier = Modifier.fillMaxSize()) {
        SideNav(
            sections = sections,
            activeIndex = activeSectionIndex,
            onSelect = { i ->
                scope.launch { listState.animateScrollToItem(i + titleOffset) }
            },
            onDismiss = onDismiss,
            modifier = Modifier
                .width(navWidth)
                .fillMaxHeight()
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentPadding = PaddingValues(
                horizontal = Spacing.xl,
                vertical = Spacing.lg
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            item(key = "__title") {
                Text(
                    text = "Settings",
                    fontSize = 36.sp,
                    fontFamily = FrankRuhlLibre,
                    fontWeight = FontWeight.Bold,
                    color = Espresso,
                    modifier = Modifier.padding(bottom = Spacing.s)
                )
            }

            sections.forEach { section ->
                item(key = section.key) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.s)) {
                        SectionHeader(
                            label = section.label,
                            helpUrl = section.helpUrl,
                            helpLabel = section.helpLabel
                        )
                        section.content()
                    }
                }
            }

            item(key = "__close") {
                Spacer(modifier = Modifier.height(Spacing.md))
                BigButton(
                    text = "CLOSE SETTINGS",
                    onClick = onDismiss,
                    variant = BigButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun SideNav(
    sections: List<AdminSection>,
    activeIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Cream)
            .padding(vertical = Spacing.lg, horizontal = Spacing.md)
    ) {
        Text(
            text = "SnapCabin",
            fontFamily = FrankRuhlLibre,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Espresso,
            modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.xs)
        )
        Text(
            text = "Admin",
            style = MaterialTheme.typography.labelSmall,
            color = Honey,
            modifier = Modifier.padding(start = Spacing.sm, bottom = Spacing.md)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            sections.forEachIndexed { i, section ->
                SideNavItem(
                    label = section.label,
                    active = i == activeIndex,
                    hasHelp = section.helpUrl != null,
                    onClick = { onSelect(i) }
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))
        BigButton(
            text = "CLOSE",
            onClick = onDismiss,
            variant = BigButtonVariant.Surface,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SideNavItem(
    label: String,
    active: Boolean,
    hasHelp: Boolean,
    onClick: () -> Unit
) {
    val container = if (active) Oat else Cream
    val accent = if (active) Pine else Espresso.copy(alpha = 0.7f)
    val border = if (active) Pine.copy(alpha = 0.5f) else CabinLine

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.xs))
            .background(container)
            .border(1.dp, border, RoundedCornerShape(Radii.xs))
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp)
                .background(if (active) Pine else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(Spacing.s))
        Text(
            text = label,
            fontFamily = HankenGrotesk,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            color = accent,
            modifier = Modifier.weight(1f)
        )
        if (hasHelp) {
            Text(
                text = "?",
                fontFamily = HankenGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Honey
            )
        }
    }
}

@Composable
private fun PinEntry(
    onPinSubmit: (String) -> Boolean,
    onCancel: () -> Unit,
    showDefaultHint: Boolean = false
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(Radii.xl))
                .background(Cream)
                .border(1.dp, CabinLine, RoundedCornerShape(Radii.xl))
                .padding(Spacing.xxl)
        ) {
            Text(
                text = "Admin PIN",
                fontSize = 36.sp,
                fontFamily = FrankRuhlLibre,
                fontWeight = FontWeight.Bold,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(Spacing.lg))

            OutlinedTextField(
                value = pin,
                onValueChange = {
                    pin = it.filter { c -> c.isDigit() }.take(8)
                    error = false
                },
                label = { Text("Enter PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error,
                singleLine = true,
                shape = RoundedCornerShape(Radii.s),
                colors = adminTextFieldColors()
            )

            if (showDefaultHint && !error) {
                Spacer(modifier = Modifier.height(Spacing.s))
                Text(
                    text = "Default PIN is 1234. Change it under KIOSK after you sign in.",
                    color = Honey,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            if (error) {
                Spacer(modifier = Modifier.height(Spacing.s))
                Text(
                    text = "Incorrect PIN",
                    color = Clay,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                BigButton(
                    text = "CANCEL",
                    onClick = onCancel,
                    variant = BigButtonVariant.Surface
                )
                BigButton(
                    text = "ENTER",
                    onClick = {
                        if (!onPinSubmit(pin)) {
                            error = true
                            pin = ""
                        }
                    },
                    variant = BigButtonVariant.Primary
                )
            }
        }
    }
}
