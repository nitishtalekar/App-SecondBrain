package app.secondbrain.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import app.secondbrain.ui.theme.Spacing
import coil.compose.AsyncImage
import java.io.File

/** Draft survives dismissal so a half-written thought is never lost. */
class CaptureDraft(text: String = "", image: Uri? = null) {
    var text by mutableStateOf(text)
    var image by mutableStateOf(image)
    val isEmpty get() = text.isBlank() && image == null
    fun clear() { text = ""; image = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    draft: CaptureDraft,
    newCaptureTarget: () -> Pair<File, Uri>,
    onSave: (String, Uri?) -> Unit,
    onDismiss: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    var cameraTarget by remember { mutableStateOf<Uri?>(null) }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) draft.image = uri
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) draft.image = cameraTarget
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = Spacing.gutter).navigationBarsPadding().imePadding(),
            verticalArrangement = spacedBy(Spacing.m),
        ) {
            TextField(
                value = draft.text,
                onValueChange = { draft.text = it },
                placeholder = {
                    Text("Write a thought, or paste a link", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                textStyle = MaterialTheme.typography.titleMedium,
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            draft.image?.let { uri ->
                Box {
                    AsyncImage(
                        model = uri,
                        contentDescription = "Attached photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(96.dp).clip(MaterialTheme.shapes.medium),
                    )
                    IconButton(
                        onClick = { draft.image = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                        ),
                    ) { Icon(Icons.Rounded.Close, contentDescription = "Remove photo", Modifier.size(18.dp)) }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Spacing.m)) {
                IconButton(onClick = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Choose from gallery")
                }
                IconButton(onClick = {
                    val (_, uri) = newCaptureTarget()
                    cameraTarget = uri
                    takePhoto.launch(uri)
                }) { Icon(Icons.Rounded.PhotoCamera, contentDescription = "Take a photo") }
                Box(Modifier.weight(1f))
                Button(
                    onClick = { onSave(draft.text, draft.image); draft.clear(); onDismiss() },
                    enabled = !draft.isEmpty,
                ) { Text("Save") }
            }
        }
    }
}
