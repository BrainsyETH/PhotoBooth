package com.snapcabin.ui.screens.branding

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import com.snapcabin.ui.theme.BoothAccent
import com.snapcabin.ui.theme.BoothPrimary
import com.snapcabin.ui.theme.BoothSecondary
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
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val preview = uiState.previewBitmap
            if (preview != null) {
                Image(
                    bitmap = preview.asImageBitmap(),
                    contentDescription = "Branded photo preview",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            if (uiState.isProcessing) {
                CircularProgressIndicator(color = BoothAccent)
            }
        }

        // Right: template picker + text inputs
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "EVENT BRANDING",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Template list
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(EventTemplate.entries) { template ->
                        val isSelected = uiState.selectedTemplate == template
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BoothPrimary else MaterialTheme.colorScheme.surface)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) BoothAccent else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { viewModel.selectTemplate(template) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = template.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Event name input
                val textFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BoothAccent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = BoothAccent,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                    cursorColor = BoothAccent
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }

            // Bottom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BigButton(
                    text = "SKIP",
                    onClick = onSkip,
                    containerColor = MaterialTheme.colorScheme.surface
                )
                BigButton(
                    text = "APPLY",
                    onClick = { onDone(viewModel.getResultBitmap()) },
                    containerColor = BoothSecondary
                )
            }
        }
    }
}
