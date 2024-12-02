package com.example.komura

import android.Manifest
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.komura.ui.theme.KomuraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KomuraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RecordingScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RecordingScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableStateOf(0) }
    var playbackDuration by remember { mutableStateOf(0) }
    var mediaRecorder: MediaRecorder? = remember { null }
    var mediaPlayer: MediaPlayer? = remember { null }
    var currentPlaying by remember { mutableStateOf<File?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<File?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var recordings by remember { mutableStateOf(emptyList<File>()) }

    // Lokasi penyimpanan
    val musicDir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Recordings")
    } else {
        File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "Recordings")
    }

    // Fungsi untuk memuat daftar rekaman
    fun loadRecordings() {
        recordings = musicDir.listFiles()?.filter { it.extension == "mp4" } ?: emptyList()
    }

    // Fungsi untuk memulai perekaman
    fun startRecording() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permission not granted!", Toast.LENGTH_SHORT).show()
            return
        }

        if (!musicDir.exists() && !musicDir.mkdirs()) {
            Toast.makeText(context, "Failed to create directory!", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "recording_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.mp4"
        val outputFile = File(musicDir, fileName)

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            try {
                prepare()
                start()
                recordingDuration = 0
                isRecording = true
                Toast.makeText(context, "Recording started!", Toast.LENGTH_SHORT).show()

                // Perbarui durasi secara real-time
                coroutineScope.launch {
                    while (isRecording) {
                        delay(1000)
                        recordingDuration++
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fungsi untuk menghentikan perekaman
    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        recordingDuration = 0
        Toast.makeText(context, "Recording saved!", Toast.LENGTH_LONG).show()
        loadRecordings() // Perbarui daftar rekaman
    }

    // Fungsi untuk memutar atau menghentikan rekaman
    fun togglePlayback(file: File) {
        if (mediaPlayer?.isPlaying == true && currentPlaying == file) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            currentPlaying = null
            playbackDuration = 0
            Toast.makeText(context, "Playback stopped.", Toast.LENGTH_SHORT).show()
        } else {
            currentPlaying = file
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    currentPlaying = null
                    mediaPlayer = null
                    playbackDuration = 0
                    Toast.makeText(context, "Playback completed.", Toast.LENGTH_SHORT).show()
                }

                // Monitor playback duration
                coroutineScope.launch {
                    while (isPlaying) {
                        delay(1000)
                        playbackDuration = currentPosition / 1000 // Convert to seconds
                    }
                }
            }
            Toast.makeText(context, "Playing: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi untuk mengganti nama file
    fun renameFile(file: File, newName: String) {
        val newFile = File(file.parent, "$newName.mp4")
        if (file.renameTo(newFile)) {
            Toast.makeText(context, "File renamed to $newName", Toast.LENGTH_SHORT).show()
            loadRecordings()
        } else {
            Toast.makeText(context, "Rename failed.", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi untuk menghapus file
    fun deleteFile(file: File) {
        if (file.delete()) {
            Toast.makeText(context, "File deleted: ${file.name}", Toast.LENGTH_SHORT).show()
            loadRecordings()
        } else {
            Toast.makeText(context, "Failed to delete file.", Toast.LENGTH_SHORT).show()
        }
    }

    // Muat daftar rekaman saat komponen dimuat
    LaunchedEffect(Unit) {
        loadRecordings()
    }

    // Format durasi menjadi MM:SS
    val formattedRecordingDuration = String.format("%02d:%02d", recordingDuration / 60, recordingDuration % 60)
    val formattedPlaybackDuration = String.format("%02d:%02d", playbackDuration / 60, playbackDuration % 60)

    // UI
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize().padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Durasi Rekaman
            if (isRecording) {
                Text(
                    text = "Recording Duration: $formattedRecordingDuration",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Durasi Playback
            if (currentPlaying != null) {
                Text(
                    text = "Playback Duration: $formattedPlaybackDuration",
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Tombol Rekam dan Stop
            IconButton(
                onClick = {
                    if (isRecording) {
                        stopRecording()
                    } else {
                        startRecording()
                    }
                },
                modifier = Modifier
                    .size(80.dp)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daftar rekaman
            Text(
                text = "Your Recordings:",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 8.dp, bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color(0xFFF5F5F5))
                    .padding(8.dp)
            ) {
                itemsIndexed(recordings) { index, file ->
                    RecordingItem(
                        file = file,
                        isPlaying = file == currentPlaying,
                        onTogglePlayback = { togglePlayback(file) },
                        onRename = {
                            fileToRename = file
                            showRenameDialog = true
                        },
                        onDelete = { deleteFile(file) },
                        index = index + 1 // Menambahkan nomor urut
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Dialog Rename
    if (showRenameDialog && fileToRename != null) {
        var newName by remember { mutableStateOf(TextFieldValue("")) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename File") },
            text = {
                Column {
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("New File Name") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    renameFile(fileToRename!!, newName.text)
                    showRenameDialog = false
                }) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RecordingItem(
    file: File,
    isPlaying: Boolean,
    onTogglePlayback: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    index: Int
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clickable { onTogglePlayback() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$index. ${file.name}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            IconButton(onClick = onRename) {
                Icon(imageVector = Icons.Filled.Edit, contentDescription = "Rename File")
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete File")
            }
            IconButton(onClick = onTogglePlayback) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }
        }
    }
}
