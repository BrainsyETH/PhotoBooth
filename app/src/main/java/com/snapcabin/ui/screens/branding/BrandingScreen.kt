package com.snapcabin.ui.screens.branding

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.shadow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.snapcabin.filter.EventBrandingRenderer
import com.snapcabin.filter.EventTemplate
import com.snapcabin.ui.components.BigButton
import com.snapcabin.ui.components.BigButtonVariant
import com.snapcabin.ui.components.Eyebrow
import com.snapcabin.ui.theme.CabinAccent
import com.snapcabin.ui.theme.CabinLineStrong
import com.snapcabin.ui.theme.CabinSurface
import com.snapcabin.ui.theme.Cream
import com.snapcabin.ui.theme.Espresso
import com.snapcabin.ui.theme.Honey
import com.snapcabin.ui.theme.Pine
import com.snapcabin.ui.theme.Radii
import com.snapcabin.ui.theme.Sidebar
import com.snapcabin.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BrandingUiState(
    val selectedTemplate: EventTemplate = EventTemplate.NONE,
    val eventName: String = "",
    val eventDate: String = "",
    val previewBitmap: Bitmap? = null,
    val isProcessing: Boolean = false
)

@HiltViewModel
class BrandingViewModel @Inject constructor(
    private val brandingRenderer: EventBrandingRenderer
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrandingUiState())
    val uiState: StateFlow<BrandingUiState> = _uiState.asStateFlow()

    private var sourcePhoto: Bitmap? = null

    fun setPhoto(bitmap: Bitmap) {
        sourcePhoto = bitmap
        renderPreview()
    }

    fun selectTemplate(template: EventTemplate) {
        _uiState.value = _uiState.value.copy(selectedTemplate = template)
        renderPreview()
    }

    fun setEventName(name: String) {
        _uiState.value = _uiState.value.copy(eventName = name)
        renderPreview()
    }

    fun setEventDate(date: String) {
        _uiState.value = _uiState.value.copy(eventDate = date)
        renderPreview()
    }

    fun getResultBitmap(): Bitmap? = _uiState.value.previewBitmap

    private fun renderPreview() {
        val photo = sourcePhoto ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            val result = withContext(Dispatchers.Default) {
                brandingRenderer.applyTemplate(
                    photo = photo,
                    template = _uiState.value.selectedTemplate,
                    eventName = _uiState.value.eventName,
                    eventDate = _uiState.value.eventDate
                )
            }
            _uiState.value = _uiState.value.copy(
                previewBitmap = result,
                isProcessing = false
            )
        }
    }
}

@Composable
fun BrandingScreen(
    photo: Bitmap?,
    onDone: (Bitmap?) -> Unit,
    onSkip: () -> Unit,
    viewModel: BrandingViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(photo) {
        photo?.let { viewModel.setPhoto(it) }
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Left: preview
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(Spacing.xl),
            contentAlignment = Alignment.Center
        ) {
            val preview = uiState.previewBitmap
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Branded photo preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(Radii.s))
                        .clip(RoundedCornerShape(Radii.s)),
                    contentScale = ContentScale.Fit
                )
            }
            if (uiState.isProcessing) {
                CircularProgressIndicator(color = CabinAccent)
            }
        }

        // Right: template picker + text inputs
        Column(
            modifier = Modifier
                .width(Sidebar.width)
                .fillMaxHeight()
                .background(CabinSurface)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(text = "EVENT BRANDING")

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Template list
                LazyColumn(
                    contentPadding = PaddingValues(vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.s),
                    modifier = Modifier.weight(1f)
                ) {
                    items(EventTemplate.entries) { template ->
                        val isSelected = uiState.selectedTemplate == template
                        val bg = if (isSelected) Pine else Cream
                        val outline = if (isSelected) Pine else CabinLineStrong
                        val textColor = if (isSelected) Color.White else Espresso

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Radii.s))
                                .background(bg)
                                .border(1.dp, outline, RoundedCornerShape(Radii.s))
                                .clickable { viewModel.selectTemplate(template) }
                                .padding(horizontal = Spacing.md, vertical = Spacing.sm + Spacing.xs)
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .padding(2.dp)
                                        .border(
                                            width = 2.dp,
                                            color = Honey,
                                            shape = RoundedCornerShape(Radii.s - 2.dp)
                                        )
                                )
                            }
                            Text(
                                text = template.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.sm))

                // Event-detail text inputs on cream sidebar background
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Pine,
                    unfocusedBorderColor = CabinLineStrong,
                    focusedTextColor = Espresso,
                    unfocusedTextColor = Espresso,
                    focusedLabelColor = Pine,
                    unfocusedLabelColor = Espresso.copy(alpha = 0.72f),
                    cursorColor = Pine,
                    focusedContainerColor = Cream,
                    unfocusedContainerColor = Cream
                )

                var nameText by remember { mutableStateOf(uiState.eventName) }
                OutlinedTextField(
                    value = nameText,
                    onValueChange = {
                        nameText = it
                        viewModel.setEventName(it)
                    },
                    label = { Text("Event Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(Radii.s),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(Spacing.s))

                var dateText by remember { mutableStateOf(uiState.eventDate) }
                OutlinedTextField(
                    value = dateText,
                    onValueChange = {
                        dateText = it
                        viewModel.setEventDate(it)
                    },
                    label = { Text("Event Date") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = textFieldColors,
                    shape = RoundedCornerShape(Radii.s),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                BigButton(
                    text = "SKIP",
                    onClick = onSkip,
                    variant = BigButtonVariant.Surface,
                    modifier = Modifier.weight(1f)
                )
                BigButton(
                    text = "APPLY",
                    onClick = { onDone(viewModel.getResultBitmap()) },
                    variant = BigButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
