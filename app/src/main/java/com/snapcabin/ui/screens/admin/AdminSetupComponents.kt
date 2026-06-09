package com.snapcabin.ui.screens.admin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snapcabin.share.QrCodeGenerator
import com.snapcabin.ui.theme.CabinLine
import com.snapcabin.ui.theme.Clay
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.HoneyDeep
import com.snapcabin.ui.theme.Mist
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Spacing

/** Semantic colour for a configuration state. */
internal enum class StatusTone { Good, Warn, Bad, Neutral }

private fun StatusTone.color(): Color = when (this) {
    StatusTone.Good -> Pine
    StatusTone.Warn -> HoneyDeep
    StatusTone.Bad -> Clay
    StatusTone.Neutral -> Mist
}

/** A small rounded chip: coloured dot + label. Used for "Ready", "Needs setup", etc. */
@Composable
internal fun StatusPill(text: String, tone: StatusTone) {
    val c = tone.color()
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(Radii.xl))
            .background(c.copy(alpha = 0.12f))
            .border(1.dp, c.copy(alpha = 0.5f), RoundedCornerShape(Radii.xl))
            .padding(horizontal = Spacing.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(c)
        )
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Espresso
        )
    }
}

/** A numbered instruction step: circled index + a title and body content. */
@Composable
internal fun NumberedStep(
    n: Int,
    title: String,
    body: @Composable () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Honey),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$n",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Espresso
            )
            body()
        }
    }
}

/**
 * A link that survives kiosk lockdown. Shows a TAP TO OPEN button (works
 * pre-lockdown), the raw URL as copyable text, and an expandable QR the
 * operator can scan with their phone when the tablet's browser is blocked.
 */
@Composable
internal fun KioskSafeLink(
    label: String,
    url: String
) {
    val context = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    val qr = remember(url, showQr) {
        if (showQr) {
            QrCodeGenerator().generate(
                url,
                size = 360,
                darkColor = android.graphics.Color.BLACK,
                lightColor = android.graphics.Color.WHITE
            )
        } else null
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.s)
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radii.xs))
                    .background(Cream)
                    .border(1.dp, Pine.copy(alpha = 0.4f), RoundedCornerShape(Radii.xs))
                    .clickable { openExternalUrl(context, url) }
                    .padding(horizontal = Spacing.sm, vertical = Spacing.xs)
            ) {
                Text(
                    text = "$label →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Pine
                )
            }
            Text(
                text = if (showQr) "Hide QR" else "Scan on phone",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = HoneyDeep,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { showQr = !showQr }
            )
        }
        SelectionContainer {
            Text(
                text = url,
                fontSize = 11.sp,
                color = Espresso.copy(alpha = 0.55f)
            )
        }
        if (qr != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Radii.s))
                    .background(Color.White)
                    .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
                    .padding(Spacing.s),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "QR code linking to $url",
                    modifier = Modifier.size(140.dp)
                )
            }
            Text(
                text = "Scan with your phone camera to open the guide there.",
                fontSize = 10.sp,
                color = Espresso.copy(alpha = 0.5f)
            )
        }
    }
}

/** A small "Show"/"Hide" text button used as the trailingIcon of a masked
 *  secret field, so an operator can verify a long pasted key on a tablet. Text
 *  rather than an eye icon because the project doesn't bundle material-icons. */
@Composable
internal fun RevealToggle(
    visible: Boolean,
    onToggle: () -> Unit
) {
    TextButton(onClick = onToggle) {
        Text(
            text = if (visible) "Hide" else "Show",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Pine
        )
    }
}

/** Inline pass/fail feedback under a text field. */
@Composable
internal fun ValidationHint(
    ok: Boolean,
    okText: String,
    hintText: String
) {
    val tone = if (ok) StatusTone.Good else StatusTone.Warn
    val c = tone.color()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = Spacing.xs)
    ) {
        Text(
            text = if (ok) "✓" else "•",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = c
        )
        Text(
            text = if (ok) okText else hintText,
            fontSize = 11.sp,
            color = Espresso.copy(alpha = 0.7f)
        )
    }
}

/** A readiness checklist row used by the Get Started hub. Tappable when
 *  [onClick] is provided — deep-links into the section that completes the
 *  item, so the operator never has to hunt the side nav. */
@Composable
internal fun ChecklistRow(
    done: Boolean,
    label: String,
    hint: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Radii.s))
            .background(Cream)
            .border(1.dp, CabinLine, RoundedCornerShape(Radii.s))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(Spacing.md),
        horizontalArrangement = Arrangement.spacedBy(Spacing.s)
    ) {
        val tone = if (done) StatusTone.Good else StatusTone.Warn
        val c = tone.color()
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (done) Pine else Color.Transparent)
                .border(1.dp, if (done) Pine else HoneyDeep, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (done) "✓" else "!",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = if (done) Color.White else HoneyDeep
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Espresso
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = Espresso.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.width(Spacing.xs))
        StatusPill(
            text = if (done) "Done" else "To do",
            tone = if (done) StatusTone.Good else StatusTone.Warn
        )
    }
}
