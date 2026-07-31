package `in`.fivedegree.volineuiandroid.compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.fivedegree.volineui.compose.AdvancedFilePicker
import `in`.fivedegree.volineui.compose.rememberAdvancedFilePickerState
import `in`.fivedegree.volineui.filepickermanager.FilePickerConfig
import `in`.fivedegree.volineui.filepickermanager.FilePickerErrorType
import `in`.fivedegree.volineui.filepickermanager.FilePickerResult
import `in`.fivedegree.volineui.filepickermanager.PickedFile
import `in`.fivedegree.volineuiandroid.ui.theme.AppTheme

/**
 * Demo screen for [AdvancedFilePicker] — single/multi pick, presets, size limits,
 * open-in-app, errors, and state-holder APIs.
 */
class ComposeFilePickerExamplesActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    FilePickerExamplesScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilePickerExamplesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun showError(type: FilePickerErrorType, message: String) {
        Toast.makeText(context, "[$type] $message", Toast.LENGTH_LONG).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Compose AdvancedFilePicker Examples",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
        Text(
            text = "Tap placeholders / add tiles to pick. Use open icon to launch the default app. Errors toast with typed [FilePickerErrorType].",
            fontSize = 13.sp,
            color = Color(0xFF666666),
        )

        Spacer(modifier = Modifier.height(4.dp))

        SectionTitle("1. Single PDF (preset)")
        SectionHint("application/pdf · max 5 MB · open in app")
        SinglePdfExample(onError = ::showError)

        HorizontalDivider()

        SectionTitle("2. Multi PDF carousel (max 3)")
        SectionHint("allowMultiple · maxFileCount=3 · page count shown when available")
        MultiPdfExample(onError = ::showError)

        HorizontalDivider()

        SectionTitle("3. CSV only")
        SectionHint("FilePickerConfig.csv() · extension + MIME filter")
        CsvExample(onError = ::showError)

        HorizontalDivider()

        SectionTitle("4. Images with dimension limits")
        SectionHint("image/* · max 2000×2000 · try a huge photo to trigger IMAGE_DIMENSION_INVALID")
        ImageLimitsExample(onError = ::showError)

        HorizontalDivider()

        SectionTitle("5. Tiny size limit (error test)")
        SectionHint("max 50 KB — pick a larger file to see FILE_TOO_LARGE")
        TinySizeLimitExample(onError = ::showError)

        HorizontalDivider()

        SectionTitle("6. Any file + state holder")
        SectionHint("rememberAdvancedFilePickerState · clear / status / last result")
        StateHolderExample(onError = ::showError)

        HorizontalDivider()

        SectionTitle("7. Controlled List<File> (like ImageCarousel)")
        SectionHint("files: List<File> + onFilesChange: (List<File>) -> Unit")
        ControlledListExample(onError = ::showError)

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF444444),
    )
}

@Composable
private fun SectionHint(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color(0xFF888888),
    )
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = Color(0xFF1976D2),
        modifier = Modifier.padding(top = 6.dp),
    )
}

@Composable
private fun SinglePdfExample(onError: (FilePickerErrorType, String) -> Unit) {
    val state = rememberAdvancedFilePickerState()
    var lastEvent by remember { mutableStateOf("Idle") }

    AdvancedFilePicker(
        state = state,
        label = "Attachment (PDF)",
        config = FilePickerConfig.pdf(maxFileSizeBytes = 5L * 1024L * 1024L),
        placeholderText = "Tap to select a PDF",
        enableOpenInApp = true,
        onError = onError,
        onPickResult = { result ->
            lastEvent = when (result) {
                is FilePickerResult.Processing -> "Processing…"
                is FilePickerResult.Cancelled -> "Cancelled"
                is FilePickerResult.Error -> "Error: ${result.type}"
                is FilePickerResult.Success -> "Pick success"
            }
        },
        onFileClick = { file, _ ->
            lastEvent = "Clicked: ${file.displayName}"
        },
        onFileDelete = { file, _ ->
            lastEvent = "Deleted: ${file.displayName}"
        },
    )
    // Always derive file summary from state.files (not only lastEvent).
    val file = state.firstOrNull()
    StatusLine(
        if (file != null) {
            "Selected: ${file.displayName} · ${file.sizeDisplay} · $lastEvent"
        } else {
            "No file · $lastEvent"
        }
    )
    if (state.hasFiles()) {
        OutlinedButton(
            onClick = {
                state.clear()
                lastEvent = "Cleared"
            },
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Clear PDF")
        }
    }
}

@Composable
private fun MultiPdfExample(onError: (FilePickerErrorType, String) -> Unit) {
    val state = rememberAdvancedFilePickerState()
    var lastEvent by remember { mutableStateOf("Idle — up to 3 PDFs") }

    AdvancedFilePicker(
        state = state,
        label = "Documents",
        config = FilePickerConfig.pdf(
            maxFileSizeBytes = 8L * 1024L * 1024L,
            allowMultiple = true,
            maxFileCount = 3,
        ),
        enableOpenInApp = true,
        onError = onError,
        onPickResult = { result ->
            lastEvent = when (result) {
                is FilePickerResult.Processing -> "Processing…"
                is FilePickerResult.Cancelled -> "Cancelled"
                is FilePickerResult.Error -> "Rejected: ${result.type}"
                is FilePickerResult.Success ->
                    "Added ${result.files.size} (rejected ${result.rejectedCount})"
            }
        },
        onFileDelete = { file, index ->
            lastEvent = "Deleted [$index] ${file.displayName}"
        },
    )
    StatusLine("${state.files.size}/3 selected · $lastEvent")
    if (state.files.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            state.files.forEachIndexed { index, file ->
                FileSummaryRow(index, file)
            }
        }
    }
}

@Composable
private fun CsvExample(onError: (FilePickerErrorType, String) -> Unit) {
    val state = rememberAdvancedFilePickerState()

    AdvancedFilePicker(
        state = state,
        label = "Import CSV",
        config = FilePickerConfig.csv(maxFileSizeBytes = 2L * 1024L * 1024L),
        placeholderText = "Tap to select a CSV",
        onError = onError,
    )
    val file = state.firstOrNull()
    StatusLine(
        if (file != null) "${file.displayName} · ${file.sizeDisplay} · ${file.mimeType}"
        else "No CSV selected"
    )
}

@Composable
private fun ImageLimitsExample(onError: (FilePickerErrorType, String) -> Unit) {
    val state = rememberAdvancedFilePickerState()

    AdvancedFilePicker(
        state = state,
        label = "Image (max 2000×2000)",
        config = FilePickerConfig.images(
            maxFileSizeBytes = 10L * 1024L * 1024L,
            maxImageWidth = 2000,
            maxImageHeight = 2000,
        ),
        placeholderText = "Tap to select an image",
        onError = onError,
    )
    val file = state.firstOrNull()
    StatusLine(
        if (file != null) {
            val dims = if (file.imageWidth != null && file.imageHeight != null) {
                "${file.imageWidth}×${file.imageHeight}"
            } else "dims n/a"
            "${file.displayName} · $dims · ${file.sizeDisplay}"
        } else {
            "No image selected"
        }
    )
}

@Composable
private fun TinySizeLimitExample(onError: (FilePickerErrorType, String) -> Unit) {
    val state = rememberAdvancedFilePickerState()

    AdvancedFilePicker(
        state = state,
        label = "Max 50 KB (any type)",
        config = FilePickerConfig(
            allowedMimeTypes = listOf("*/*"),
            maxFileSizeBytes = 50L * 1024L,
        ),
        placeholderText = "Pick a small file, or a large one to fail",
        onError = onError,
    )
    val file = state.firstOrNull()
    StatusLine(
        if (file != null) "Accepted: ${file.displayName} · ${file.sizeDisplay}"
        else "Waiting — oversized files should toast FILE_TOO_LARGE"
    )
}

@Composable
private fun StateHolderExample(onError: (FilePickerErrorType, String) -> Unit) {
    val state = rememberAdvancedFilePickerState()
    var lastResult by remember { mutableStateOf("—") }

    AdvancedFilePicker(
        state = state,
        label = "Mixed docs (PDF / CSV / TXT)",
        config = FilePickerConfig(
            allowedMimeTypes = listOf(
                "application/pdf",
                "text/csv",
                "text/plain",
                "text/comma-separated-values",
            ),
            allowedExtensions = listOf("pdf", "csv", "txt"),
            allowMultiple = true,
            maxFileCount = 5,
            maxFileSizeBytes = 5L * 1024L * 1024L,
        ),
        onError = onError,
        onPickResult = { result ->
            lastResult = when (result) {
                is FilePickerResult.Processing -> "Processing"
                is FilePickerResult.Cancelled -> "Cancelled"
                is FilePickerResult.Error -> "${result.type}: ${result.message}"
                is FilePickerResult.Success -> result.files.joinToString { it.displayName }
            }
        },
        onFileDelete = { file, index ->
            lastResult = "Deleted [$index] ${file.displayName}"
        },
    )

    StatusLine(
        "UI state: ${state.currentState} · files=${state.files.size} · loading=${state.isLoading()}"
    )
    StatusLine("Last event: $lastResult")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = {
                state.clear()
                lastResult = "Cleared"
            },
            enabled = state.hasFiles(),
        ) {
            Text("Clear all")
        }
        Button(
            onClick = {
                if (state.files.isNotEmpty()) {
                    val removed = state.files.first()
                    state.removeAt(0)
                    lastResult = "Removed first: ${removed.displayName}"
                }
            },
            enabled = state.hasFiles(),
        ) {
            Text("Remove first")
        }
    }

    if (state.files.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            state.files.forEachIndexed { index, file ->
                FileSummaryRow(index, file)
            }
        }
    } else {
        StatusLine("No files in state")
    }
}

@Composable
private fun ControlledListExample(onError: (FilePickerErrorType, String) -> Unit) {
    var files by remember { mutableStateOf<List<java.io.File>>(emptyList()) }

    AdvancedFilePicker(
        files = files,
        onFilesChange = { files = it },
        label = "Controlled multi (max 4)",
        config = FilePickerConfig(
            allowedMimeTypes = listOf("*/*"),
            allowMultiple = true,
            maxFileCount = 4,
            maxFileSizeBytes = 10L * 1024L * 1024L,
        ),
        onError = onError,
        enableOpenInApp = true,
        showDeleteButton = true,
    )
    StatusLine("${files.size}/4 files in list state")
    if (files.isNotEmpty()) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            files.forEachIndexed { index, file ->
                FileSummaryRow(index, file)
            }
        }
    }
}

@Composable
private fun FileSummaryRow(index: Int, file: PickedFile) {
    Text(
        text = "${index + 1}. ${file.displayName} · ${file.sizeDisplay}" +
            (file.extension.takeIf { it.isNotBlank() }?.let { " · .$it" } ?: "") +
            (file.pageCount?.let { " · $it pages" } ?: ""),
        fontSize = 12.sp,
        color = Color(0xFF333333),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun FileSummaryRow(index: Int, file: java.io.File) {
    Text(
        text = "${index + 1}. ${file.name} · ${PickedFile.formatFileSize(file.length())}" +
            (file.extension.takeIf { it.isNotBlank() }?.let { " · .$it" } ?: ""),
        fontSize = 12.sp,
        color = Color(0xFF333333),
        modifier = Modifier.padding(vertical = 2.dp),
    )
}
