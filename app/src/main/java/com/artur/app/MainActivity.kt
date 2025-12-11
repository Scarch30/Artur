package com.artur.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.artur.app.ui.theme.ArturTheme
import androidx.compose.material3.TopAppBar

// ---------------------------------------------------------
// Activity
// ---------------------------------------------------------

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArturTheme {
                ArturApp()
            }
        }
    }
}

// ---------------------------------------------------------
// Navigation simple : Chat <-> ScanPreview
// ---------------------------------------------------------

sealed class Screen {
    object Chat : Screen()
    data class ScanPreview(val imageUri: Uri) : Screen()
}

@Composable
fun ArturApp() {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Chat) }

    when (val screen = currentScreen) {
        is Screen.Chat -> ChatScreen(
            onImageForScan = { uri ->
                currentScreen = Screen.ScanPreview(uri)
            }
        )

        is Screen.ScanPreview -> ScanPreviewScreen(
            imageUri = screen.imageUri,
            onBack = {
                currentScreen = Screen.Chat
            }
        )
    }
}

// ---------------------------------------------------------
// Modèles
// ---------------------------------------------------------

data class ChatMessage(
    val fromUser: Boolean,
    val text: String
)

/**
 * Coordonnées normalisées :
 * x, y ∈ [0,1], (0,0) = haut-gauche, (1,1) = bas-droite
 */
data class NormalizedPoint(
    val x: Float,
    val y: Float
)

data class Polygon(
    val id: Int,
    val points: List<NormalizedPoint>
)

// ---------------------------------------------------------
// Écran de chat
// ---------------------------------------------------------

@Composable
fun ChatScreen(
    onImageForScan: (Uri) -> Unit
) {
    // Messages
    val messages = remember {
        mutableStateListOf(
            ChatMessage(fromUser = false, text = "Bonjour, je suis ARTUR. De quoi as-tu besoin ?")
        )
    }
    var input by remember { mutableStateOf("") }

    // Image sélectionnée (pré-scan)
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher pour ouvrir la galerie
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            MessageInputBar(
                input = input,
                onInputChange = { input = it },
                onSend = {
                    val trimmed = input.trim()
                    if (trimmed.isNotEmpty()) {
                        messages.add(ChatMessage(fromUser = true, text = trimmed))
                        // Pour l’instant : réponse fake
                        messages.add(
                            ChatMessage(
                                fromUser = false,
                                text = "J’ai bien reçu : \"$trimmed\" (connexion n8n à venir)."
                            )
                        )
                        input = ""
                    }
                },
                onAttachClick = {
                    pickImageLauncher.launch("image/*")
                },
                onMicClick = {
                    // TODO : micro plus tard
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Liste des messages
            MessagesList(
                messages = messages,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )

            // Aperçu image sélectionnée
            selectedImageUri?.let { uri ->
                SelectedImagePreview(
                    uri = uri,
                    onClear = { selectedImageUri = null },
                    onSendToArtur = {
                        // 👉 On ouvre l'écran de prévisualisation de scan
                        onImageForScan(uri)
                        selectedImageUri = null
                    }
                )
            }
        }
    }
}

@Composable
fun MessagesList(
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        reverseLayout = false
    ) {
        items(messages) { msg ->
            val bubbleColor =
                if (msg.fromUser) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.secondaryContainer
            val textColor =
                if (msg.fromUser) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSecondaryContainer

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement =
                    if (msg.fromUser) Arrangement.End else Arrangement.Start
            ) {
                Surface(
                    color = bubbleColor,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = msg.text,
                        color = textColor,
                        modifier = Modifier
                            .padding(10.dp)
                            .widthIn(max = 280.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MessageInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onMicClick: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bouton "+"
            IconButton(onClick = onAttachClick) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ajouter une pièce jointe"
                )
            }

            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Parler à ARTUR…")
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.width(4.dp))

            // Bouton micro (plus tard)
            IconButton(onClick = onMicClick) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Parler à ARTUR"
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(onClick = onSend) {
                Text("Envoyer", textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun SelectedImagePreview(
    uri: Uri,
    onClear: () -> Unit,
    onSendToArtur: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Image sélectionnée",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            AsyncImage(
                model = uri,
                contentDescription = "Image sélectionnée",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onClear) {
                    Text("Annuler")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = onSendToArtur) {
                    Text("Prévisualiser le scan")
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Écran de prévisualisation de scan (image + polygone draggable)
// ---------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanPreviewScreen(
    imageUri: Uri,
    onBack: () -> Unit
) {
    // Polygones en état mutable (fake pour l’instant)
    val polygons = remember {
        mutableStateListOf(
            Polygon(
                id = 1,
                points = listOf(
                    NormalizedPoint(0.2f, 0.2f),
                    NormalizedPoint(0.8f, 0.2f),
                    NormalizedPoint(0.8f, 0.8f),
                    NormalizedPoint(0.2f, 0.8f)
                )
            )
        )
    }

    // Taille du canvas en pixels
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Handle actif : Pair(indexPolygone, indexPoint)
    var activeHandle by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prévisualisation du document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                }
            )

        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Image en fond
            AsyncImage(
                model = imageUri,
                contentDescription = "Image à scanner",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Overlay interactif
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(polygons, canvasSize) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures

                                val thresholdPx = 80f // rayon pour "attraper" une poignée
                                var best: Pair<Int, Int>? = null
                                var bestDist2 = Float.MAX_VALUE

                                polygons.forEachIndexed { polyIndex, poly ->
                                    poly.points.forEachIndexed { pointIndex, p ->
                                        val cx = p.x * canvasSize.width
                                        val cy = p.y * canvasSize.height
                                        val dx = offset.x - cx
                                        val dy = offset.y - cy
                                        val dist2 = dx * dx + dy * dy
                                        if (dist2 < thresholdPx * thresholdPx && dist2 < bestDist2) {
                                            bestDist2 = dist2
                                            best = polyIndex to pointIndex
                                        }
                                    }
                                }

                                activeHandle = best
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val handle = activeHandle ?: return@detectDragGestures
                                val (polyIndex, pointIndex) = handle
                                if (canvasSize.width == 0 || canvasSize.height == 0) return@detectDragGestures

                                val poly = polygons[polyIndex]
                                val oldPoint = poly.points[pointIndex]

                                // On convertit le point actuel en pixels, on applique le drag, puis on renormalise
                                val currentPxX = oldPoint.x * canvasSize.width
                                val currentPxY = oldPoint.y * canvasSize.height

                                val newPxX = (currentPxX + dragAmount.x)
                                    .coerceIn(0f, canvasSize.width.toFloat())
                                val newPxY = (currentPxY + dragAmount.y)
                                    .coerceIn(0f, canvasSize.height.toFloat())

                                val newNormX = newPxX / canvasSize.width
                                val newNormY = newPxY / canvasSize.height

                                val newPoint = oldPoint.copy(
                                    x = newNormX,
                                    y = newNormY
                                )

                                val newPoints = poly.points.toMutableList()
                                newPoints[pointIndex] = newPoint
                                polygons[polyIndex] = poly.copy(points = newPoints)
                            },
                            onDragEnd = {
                                activeHandle = null
                            },
                            onDragCancel = {
                                activeHandle = null
                            }
                        )
                    }
            ) {
                // On récupère la taille réelle du canvas
                canvasSize = IntSize(width = size.width.toInt(), height = size.height.toInt())

                // Dessin des polygones
                polygons.forEach { polygon ->
                    if (polygon.points.size >= 4) {
                        val path = Path()
                        val first = polygon.points.first()
                        path.moveTo(
                            first.x * size.width,
                            first.y * size.height
                        )
                        polygon.points.drop(1).forEach { p ->
                            path.lineTo(
                                p.x * size.width,
                                p.y * size.height
                            )
                        }
                        path.close()

                        // Contour
                        drawPath(
                            path = path,
                            color = Color.Red,
                            style = Stroke(width = 4f)
                        )

                        // Poignées (cercles)
                        polygon.points.forEach { p ->
                            val cx = p.x * size.width
                            val cy = p.y * size.height
                            drawCircle(
                                color = Color.Red,
                                radius = 12f,
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------
// Preview
// ---------------------------------------------------------

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    ArturTheme {
        ChatScreen(onImageForScan = {})
    }
}
