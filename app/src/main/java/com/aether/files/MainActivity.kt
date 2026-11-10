package com.aether.files

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Background = Color(0xFF101319)
private val Surface = Color(0xFF1B2029)
private val Accent = Color(0xFF92B9FF)

class MainActivity : ComponentActivity() {
    private val prefs by lazy { getSharedPreferences("aether", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AetherScreen() }
    }

    @Composable
    private fun AetherScreen() {
        val folders = remember { mutableStateListOf<DocumentFile>() }
        val files = remember { mutableStateListOf<DocumentFile>() }
        var busy by remember { mutableStateOf(false) }
        var message by remember { mutableStateOf("") }
        var search by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf<DocumentFile?>(null) }
        var dialog by remember { mutableStateOf("") }
        var field by remember { mutableStateOf("") }
        var clipboard by remember { mutableStateOf<DocumentFile?>(null) }
        var cut by remember { mutableStateOf(false) }
        val current = folders.lastOrNull()

        fun refresh() {
            val folder = folders.lastOrNull() ?: return
            lifecycleScope.launch {
                busy = true
                try {
                    val result = withContext(Dispatchers.IO) { FileOps.list(folder) }
                    files.clear(); files.addAll(result)
                } catch (e: Exception) { message = e.message ?: "Erro ao listar arquivos" }
                finally { busy = false }
            }
        }
        fun execute(task: () -> Unit) {
            lifecycleScope.launch {
                busy = true
                try { withContext(Dispatchers.IO) { task() }; message = "Operação concluída" }
                catch (e: Exception) { message = e.message ?: "Falha na operação" }
                finally { busy = false; selected = null; refresh() }
            }
        }
        fun navigateBack() {
            if (folders.size > 1) { folders.removeAt(folders.lastIndex); search = ""; selected = null; refresh() }
            else { folders.clear(); files.clear(); selected = null }
        }
        BackHandler(folders.isNotEmpty()) { navigateBack() }

        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    val root = DocumentFile.fromTreeUri(this, uri)
                    if (root != null) {
                        prefs.edit().putString("tree", uri.toString()).apply()
                        folders.clear(); folders.add(root); refresh()
                    }
                } catch (e: Exception) { message = e.message ?: "Permissão não concedida" }
            }
        }
        LaunchedEffect(Unit) {
            prefs.getString("tree", null)?.let { saved ->
                try {
                    val uri = Uri.parse(saved)
                    if (contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }) {
                        DocumentFile.fromTreeUri(this@MainActivity, uri)?.let { folders.add(it); refresh() }
                    }
                } catch (_: Exception) { }
            }
        }

        MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Background, surface = Surface, onSurface = Color.White)) {
            Column(Modifier.fillMaxSize().background(Background).statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, null, tint = Accent, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Aether Files", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { picker.launch(null) }) { Icon(Icons.Default.FolderOpen, "Escolher pasta") }
                }
                if (current == null) {
                    Column(Modifier.fillMaxSize().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.FolderOpen, null, modifier = Modifier.size(72.dp), tint = Accent)
                        Spacer(Modifier.height(18.dp))
                        Text("Seus arquivos, seu controle", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(8.dp))
                        Text("Selecione uma pasta para começar. O Android solicitará sua autorização.", color = Color.LightGray)
                        Spacer(Modifier.height(22.dp))
                        Button(onClick = { picker.launch(null) }) { Text("Selecionar pasta") }
                    }
                } else {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { navigateBack() }) { Icon(Icons.Default.ArrowBack, "Voltar") }
                        Text(current.name ?: "Armazenamento", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { refresh() }) { Icon(Icons.Default.Refresh, "Atualizar") }
                    }
                    OutlinedTextField(search, { search = it }, label = { Text("Pesquisar nesta pasta") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ActionChip("Nova pasta", Icons.Default.CreateNewFolder) { field = ""; dialog = "folder" }
                        ActionChip("Novo arquivo", Icons.Default.NoteAdd) { field = ""; dialog = "file" }
                        if (clipboard != null) ActionChip("Colar", Icons.Default.ContentPaste) {
                            val src = clipboard!!
                            val dest = current
                            if (src.uri == dest.uri || (src.isDirectory && folders.any { it.uri == src.uri })) {
                                message = "Não é possível colar uma pasta dentro dela mesma"
                            } else execute {
                                FileOps.copy(this@MainActivity, src, dest)
                                if (cut) {
                                    if (!src.delete()) throw IllegalStateException("Copiado, mas não foi possível apagar a origem")
                                }
                                clipboard = null; cut = false
                            }
                        }
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("${files.size} itens", modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp), color = Color.LightGray)
                    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(files.filter { it.name?.contains(search, ignoreCase = true) == true }, key = { it.uri.toString() }) { item ->
                            Row(Modifier.fillMaxWidth().clickable {
                                if (item.isDirectory) { folders.add(item); search = ""; refresh() }
                                else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(item.uri, item.type ?: "application/octet-stream")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        startActivity(Intent.createChooser(intent, "Abrir com"))
                                    } catch (_: Exception) { message = "Nenhum aplicativo disponível para abrir este arquivo" }
                                }
                            }.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (item.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile, null, tint = if (item.isDirectory) Accent else Color.LightGray, modifier = Modifier.size(34.dp))
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.name ?: "Sem nome", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(if (item.isDirectory) "Pasta" else formatSize(item.length()), color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { selected = item; dialog = "actions" }) { Icon(Icons.Default.MoreVert, "Opções") }
                            }
                            HorizontalDivider(color = Color(0xFF272C36))
                        }
                    }
                }
                if (message.isNotBlank()) {
                    Row(Modifier.fillMaxWidth().background(Surface).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { message = "" }) { Text("OK") }
                    }
                }
            }
            if (dialog == "actions" && selected != null) {
                val item = selected!!
                AlertDialog(onDismissRequest = { dialog = "" }, title = { Text(item.name ?: "Arquivo") }, text = {
                    Column {
                        TextButton(onClick = { clipboard = item; cut = false; dialog = "" }) { Text("Copiar") }
                        TextButton(onClick = { clipboard = item; cut = true; dialog = "" }) { Text("Mover (recortar)") }
                        TextButton(onClick = { field = item.name ?: ""; dialog = "rename" }) { Text("Renomear") }
                        TextButton(onClick = { dialog = "delete" }) { Text("Excluir", color = Color(0xFFFF9D9D)) }
                    }
                }, confirmButton = { TextButton(onClick = { dialog = "" }) { Text("Fechar") } })
            }
            if (dialog in listOf("folder", "file", "rename")) {
                val kind = dialog
                AlertDialog(onDismissRequest = { dialog = "" }, title = { Text(when(kind) { "folder" -> "Nova pasta"; "file" -> "Novo arquivo"; else -> "Renomear" }) },
                    text = { OutlinedTextField(field, { field = it }, label = { Text("Nome") }, singleLine = true) },
                    confirmButton = { TextButton(enabled = field.isNotBlank(), onClick = {
                        val name = field.trim()
                        val target = current
                        val item = selected
                        dialog = ""
                        execute {
                            val success = when (kind) {
                                "folder" -> target?.createDirectory(name) != null
                                "file" -> target?.createFile("application/octet-stream", name) != null
                                else -> item?.renameTo(name) == true
                            }
                            if (!success) error("Não foi possível concluir a operação")
                        }
                    }) { Text("Salvar") } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancelar") } })
            }
            if (dialog == "delete") {
                AlertDialog(onDismissRequest = { dialog = "" }, title = { Text("Excluir permanentemente?") }, text = { Text("O item selecionado poderá não ser recuperável.") },
                    confirmButton = { TextButton(onClick = {
                        val item = selected; dialog = ""
                        execute { if (item?.delete() != true) error("Falha ao excluir") }
                    }) { Text("Excluir", color = Color(0xFFFF9D9D)) } }, dismissButton = { TextButton(onClick = { dialog = "" }) { Text("Cancelar") } })
            }
        }
    }
}

@Composable
private fun ActionChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) }, leadingIcon = { Icon(icon, null, Modifier.size(18.dp)) })
}
private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.1f GB".format(bytes / (1024.0 * 1024 * 1024))
}
