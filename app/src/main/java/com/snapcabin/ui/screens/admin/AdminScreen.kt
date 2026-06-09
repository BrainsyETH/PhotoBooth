package com.snapcabin.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
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
import com.snapcabin.ui.screens.admin.sections.GetStartedSection
import com.snapcabin.ui.screens.admin.sections.KioskSection
import com.snapcabin.ui.screens.admin.sections.ModesSection
import com.snapcabin.ui.screens.admin.sections.ResendSection
import com.snapcabin.ui.screens.admin.sections.ShareSection
import com.snapcabin.ui.screens.admin.sections.SoundSection
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SETUP_GUIDE_RESEND = "https://snapcabin.app/setup/resend"
private const val SETUP_GUIDE_CLOUDINARY = "https://snapcabin.app/setup/cloudinary"
private const val SETUP_GUIDE_KIOSK = "https://snapcabin.app/setup/kiosk"

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
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()
            val clickedIndexState = remember { mutableStateOf<Int?>(null) }

            // Reassigned right after `sections` is built; content lambdas only
            // invoke it at click time, long after assignment. Lets the
            // GET STARTED checklist deep-link into other sections.
            var jumpToSection: (String) -> Unit = {}

            // Section order follows the operator's setup journey: identity
            // first (event, branding), then the guest experience, then
            // delivery, then device/maintenance.
            val sections = listOf(
                AdminSection("getstarted", "GET STARTED") {
                    GetStartedSection(
                        settings = settings,
                        onJumpTo = { key -> jumpToSection(key) },
                        onCollapse = { v -> viewModel.updateSetting { copy(getStartedCollapsed = v) } }
                    )
                },
                AdminSection("event", "EVENT") {
                    EventSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("branding", "BRANDING") {
                    BrandingSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("modes", "MODES") {
                    ModesSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("capture", "CAPTURE") {
                    CaptureSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("share", "SHARE OPTIONS") {
                    ShareSection(settings = settings, viewModel = viewModel)
                },
                AdminSection(
                    key = "resend",
                    label = "EMAIL DELIVERY",
                    helpUrl = SETUP_GUIDE_RESEND
                ) {
                    ResendSection(settings = settings, viewModel = viewModel)
                },
                AdminSection(
                    key = "cloudinary",
                    label = "QR DOWNLOADS",
                    helpUrl = SETUP_GUIDE_CLOUDINARY
                ) {
                    CloudinarySection(settings = settings, viewModel = viewModel)
                },
                AdminSection("camera", "CAMERA") {
                    CameraSection(settings = settings, cameras = cameras, viewModel = viewModel)
                },
                AdminSection("sound", "SOUND") {
                    SoundSection(settings = settings, viewModel = viewModel)
                },
                AdminSection(
                    key = "kiosk",
                    label = "KIOSK",
                    helpUrl = SETUP_GUIDE_KIOSK
                ) {
                    KioskSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("audit", "AUDIT LOG") {
                    AuditSection(settings = settings, viewModel = viewModel)
                },
                AdminSection("about", "TOOLS & ABOUT") {
                    AboutSection(onGallery = onGallery, onPrivacyPolicy = onPrivacyPolicy)
                }
            )

            jumpToSection = { key ->
                val i = sections.indexOfFirst { it.key == key }
                if (i >= 0) {
                    clickedIndexState.value = i
                    // Instant jump — animating through tall sections reads as
                    // a laggy scroll. +1 skips the LazyColumn's title item.
                    scope.launch { listState.scrollToItem(i + 1) }
                }
            }

            AdminContent(
                sections = sections,
                onDismiss = onDismiss,
                listState = listState,
                clickedIndexState = clickedIndexState
            )
        }
    }
}

@Composable
private fun AdminContent(
    sections: List<AdminSection>,
    onDismiss: () -> Unit,
    listState: LazyListState,
    clickedIndexState: MutableState<Int?>
) {
    val scope = rememberCoroutineScope()

    // The LazyColumn has a title item at index 0 and a close button at the end.
    // Section indices in the side nav are offset by 1.
    val titleOffset = 1
    // Track the side-nav-clicked index so the highlight updates immediately,
    // even when the LazyColumn can't scroll the bottom sections all the way
    // to the top. Reset to null when the user scrolls manually so the
    // firstVisibleItemIndex-derived highlight takes over again. Hoisted to
    // AdminScreen so the GET STARTED checklist's deep links share it.
    var clickedIndex by clickedIndexState
    val scrolledIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            (first - titleOffset).coerceIn(0, sections.size - 1)
        }
    }
    // Clear the clicked highlight only on an actual user drag — not on the
    // programmatic jump, which would otherwise null it mid-scroll and make
    // the highlight flicker back to wherever the list happened to land.
    LaunchedEffect(listState) {
        listState.interactionSource.interactions.collect { interaction ->
            if (interaction is DragInteraction.Start) clickedIndex = null
        }
    }
    val activeSectionIndex = clickedIndex ?: scrolledIndex

    val screen = rememberScreenClass()
    val navWidth = screen.scaledDp(240).dp

    Row(modifier = Modifier.fillMaxSize()) {
        SideNav(
            sections = sections,
            activeIndex = activeSectionIndex,
            onSelect = { i ->
                clickedIndex = i
                scope.launch { listState.scrollToItem(i + titleOffset) }
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

            // Trailing spacer so the last sections (AUDIT, TOOLS, ABOUT) can
            // animate-scroll all the way to the top. Without this, clicking
            // those side-nav items appears to do nothing because the list
            // is already pinned to its natural end.
            item(key = "__tail_pad") {
                Spacer(modifier = Modifier.fillParentMaxHeight(0.7f))
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

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(sections, key = { _, s -> s.key }) { i, section ->
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

/**
 * Process-wide PIN attempt throttle. Deliberately not persisted — it's a
 * speed bump against a bored guest grinding 4-digit PINs at the kiosk, not a
 * vault. Living outside composition means backing out of the PIN screen and
 * re-entering doesn't reset the lockout.
 */
private object PinGuard {
    const val MAX_ATTEMPTS = 5
    const val LOCKOUT_MS = 30_000L
    var failedAttempts = 0
    var lockedUntilMs = 0L

    fun registerFailure(now: Long) {
        failedAttempts++
        if (failedAttempts >= MAX_ATTEMPTS) {
            lockedUntilMs = now + LOCKOUT_MS
            failedAttempts = 0
        }
    }

    fun reset() {
        failedAttempts = 0
        lockedUntilMs = 0L
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

    var lockRemainingS by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            lockRemainingS = ((PinGuard.lockedUntilMs - System.currentTimeMillis()) / 1000L)
                .coerceAtLeast(0L).toInt()
            delay(250)
        }
    }
    val locked = lockRemainingS > 0

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

            if (error && !locked) {
                Spacer(modifier = Modifier.height(Spacing.s))
                Text(
                    text = "Incorrect PIN",
                    color = Clay,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (locked) {
                Spacer(modifier = Modifier.height(Spacing.s))
                Text(
                    text = "Too many attempts — try again in ${lockRemainingS}s",
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
                        if (onPinSubmit(pin)) {
                            PinGuard.reset()
                        } else {
                            PinGuard.registerFailure(System.currentTimeMillis())
                            error = true
                            pin = ""
                        }
                    },
                    variant = BigButtonVariant.Primary,
                    enabled = !locked
                )
            }
        }
    }
}
