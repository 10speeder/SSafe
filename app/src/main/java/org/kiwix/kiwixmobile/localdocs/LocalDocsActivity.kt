package org.kiwix.kiwixmobile.localdocs

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import org.kiwix.kiwixmobile.R
import java.io.File
import java.util.ArrayDeque

class LocalDocsActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var txtPath: TextView
    private lateinit var btnChoose: Button
    private lateinit var btnUp: Button

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("ssafe_prefs", Context.MODE_PRIVATE)
    }

    private var rootTreeUri: Uri? = null
    private var currentDir: DocumentFile? = null
    private val stack = ArrayDeque<DocumentFile>()
    private var currentChildren: List<DocumentFile> = emptyList()

    private val openTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                val flags: Int =
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {
                    // ignore if already granted
                }
                prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
                rootTreeUri = uri
                openRoot()
            } else {
                Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val storagePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                openLegacyRoot()
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_docs)

        listView = findViewById(R.id.listView)
        txtPath = findViewById(R.id.txtPath)
        btnChoose = findViewById(R.id.btnChoose)
        btnUp = findViewById(R.id.btnUp)

        btnChoose.setOnClickListener { chooseFolder() }
        btnUp.setOnClickListener { goUp() }

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = currentChildren.getOrNull(position) ?: return@setOnItemClickListener
            if (item.isDirectory) {
                enterDir(item)
            } else {
                openFile(item)
            }
        }

        val saved = prefs.getString(KEY_TREE_URI, null)
        rootTreeUri = saved?.let { Uri.parse(it) }
        if (rootTreeUri == null) {
            txtPath.text = "(no folder selected) Tap 'Choose folder' to pick one."
        } else {
            openRoot()
        }
    }

    private fun chooseFolder() {
        // Try system folder picker first
        val treeIntent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
        val canPickTree = treeIntent.resolveActivity(packageManager) != null
        if (canPickTree) {
            try {
                openTreeLauncher.launch(null)
                return
            } catch (_: Exception) {
                // fall through to legacy
            }
        }
        // Legacy fallback: request permission, then browse /sdcard
        storagePermLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun openLegacyRoot() {
        val rootFile = android.os.Environment.getExternalStorageDirectory()
        if (rootFile != null && rootFile.exists()) {
            rootTreeUri = Uri.fromFile(rootFile)
            openRoot()
        } else {
            Toast.makeText(this, "Storage not accessible", Toast.LENGTH_LONG).show()
        }
    }

    private fun openRoot() {
        val uri = rootTreeUri ?: return
        val root: DocumentFile? = when (uri.scheme) {
            "content" -> DocumentFile.fromTreeUri(this, uri)
            "file" -> DocumentFile.fromFile(File(uri.path!!))
            else -> null
        }
        if (root == null) {
            Toast.makeText(this, "Folder not accessible. Choose again.", Toast.LENGTH_LONG).show()
            return
        }
        stack.clear()
        currentDir = root
        refreshList()
    }

    private fun enterDir(dir: DocumentFile) {
        currentDir?.let { stack.addLast(it) }
        currentDir = dir
        refreshList()
    }

    private fun goUp() {
        if (stack.isEmpty()) return
        currentDir = stack.removeLast()
        refreshList()
    }

    private fun refreshList() {
        val dir = currentDir ?: return
        txtPath.text = dir.name ?: (dir.uri.lastPathSegment ?: dir.uri.toString())

        val children = dir.listFiles().toList()
        val allowed = children.filter { it.isDirectory || isSupportedFile(it) }

        val (folders, files) = allowed.partition { it.isDirectory }
        val sorted = folders.sortedBy { it.name?.lowercase() ?: "" } +
                files.sortedBy { it.name?.lowercase() ?: "" }

        currentChildren = sorted

        val names = sorted.map {
            val n = it.name ?: "(unnamed)"
            if (it.isDirectory) "📁  $n" else "📄  $n"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listView.adapter = adapter
    }

    private fun isSupportedFile(f: DocumentFile): Boolean {
        val n = f.name?.lowercase() ?: return false
        return n.endsWith(".pdf") || n.endsWith(".epub")
    }

    private fun openFile(f: DocumentFile) {
        val n = f.name?.lowercase() ?: ""
        when {
            n.endsWith(".pdf") -> {
                val intent = android.content.Intent(
                    this,
                    org.kiwix.kiwixmobile.reader.PdfReaderActivity::class.java
                )
                intent.setData(f.uri)
                if (f.uri.scheme == "content") {
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(intent)
            }
            n.endsWith(".epub") -> {
                Toast.makeText(this, "EPUB reader coming next ✨", Toast.LENGTH_SHORT).show()
                // When ready:
                // val intent = android.content.Intent(this, org.kiwix.kiwixmobile.reader.EpubReaderActivity::class.java)
                // intent.setData(f.uri)
                // intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "Unsupported file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val KEY_TREE_URI = "tree_uri"
    }
}
