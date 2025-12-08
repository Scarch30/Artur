package com.artur.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.artur.app.ui.theme.ArturTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArturTheme {
                ChatScreen()
            }
        }
    }
}

// --- Modèle très simple de message ---
data class ChatMessage(
    val fromUser: Boolean,
    val text: String
)

// --- Écran principal de chat ---
@Composable
fun ChatScreen() {
    // Messages
    val messages = remember {
        mutableStateListOf(
            ChatMessage(fromUser = false, text = "Bonjour, je suis ARTUR. De quoi as-tu besoin ?")
        )
    }
    var input by remember { mutableStateOf("") }

    // Image sélectionnée (pour futur scan)
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
                        // Pour l’instant : réponse fake d’ARTUR
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
                    // Ouvre la galerie pour choisir une image
                    pickImageLauncher.launch("image/*")
                },
                onMicClick = {
                    // TODO : à brancher plus tard (enregistrement vocal)
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

            // Si une image est sélectionnée, on affiche un petit aperçu
            selectedImageUri?.let { uri ->
                SelectedImagePreview(
                    uri = uri,
                    onClear = { selectedImageUri = null },
                    onSendToArtur = {
                        // Plus tard : on enverra l’image vers /artur/scan
                        messages.add(
                            ChatMessage(
                                fromUser = true,
                                text = "[Image sélectionnée pour scan]"
                            )
                        )
                        selectedImageUri = null
                    }
                )
            }
        }
    }
}

// --- Liste des messages ---
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

// --- Barre de saisie en bas (champ + + + micro + envoyer) ---
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

            // Bouton micro (pour plus tard)
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

// --- Aperçu de l’image sélectionnée ---
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
                    Text("Envoyer à ARTUR")
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    ArturTheme {
        ChatScreen()
    }
}
