package org.kiwix.kiwixmobile.localdocs

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
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
    private lateinit var btnStorage: Button

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("ssafe_prefs", Context.MODE_PRIVATE)
    }

    private var rootTreeUri: Uri? = null
    private var homeTreeUri: Uri? = null
    private var currentDir: DocumentFile? = null
    private val stack = ArrayDeque<DocumentFile>()
    private var currentChildren: List<DocumentFile> = emptyList()

    // When true we’re showing a search result list
    private var showingSearch: Boolean = false

    // Storage volumes (internal + SD, etc.)
    private var storageRoots: List<File> = emptyList()
    private var storageIndex: Int = 0

    private val openTreeLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
            if (uri != null) {
                val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) { /* ignore */ }
                prefs.edit().putString(KEY_TREE_URI, uri.toString()).apply()
                rootTreeUri = uri
                showingSearch = false
                openRoot()
            } else {
                Toast.makeText(this, "No folder selected", Toast.LENGTH_SHORT).show()
            }
        }

    private val storagePermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) openLegacyRoot()
            else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_LONG).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_local_docs)

        // Hook up our toolbar so the overflow menu appears
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        listView = findViewById(R.id.listView)
        txtPath = findViewById(R.id.txtPath)
        btnChoose = findViewById(R.id.btnChoose)
        btnUp = findViewById(R.id.btnUp)
        btnStorage = findViewById(R.id.btnStorage)

        btnChoose.setOnClickListener { chooseFolder() }
        btnUp.setOnClickListener { showingSearch = false; goUp() }
        btnStorage.setOnClickListener { showingSearch = false; switchStorage() }

        listView.setOnItemClickListener { _, _, position, _ ->
            val item = currentChildren.getOrNull(position) ?: return@setOnItemClickListener
            if (item.isDirectory) {
                showingSearch = false
                enterDir(item)
            } else {
                openFile(item)
            }
        }

        // Detect volumes (internal + SD card[s])
        storageRoots = detectStorageRoots()

        rootTreeUri = prefs.getString(KEY_TREE_URI, null)?.let { Uri.parse(it) }
        homeTreeUri = prefs.getString(KEY_HOME_URI, null)?.let { Uri.parse(it) }

        if (rootTreeUri == null) {
            txtPath.text = "(no folder selected) Tap 'Choose folder' or 'Storage' to switch volumes."
        } else {
            openRoot()
        }
    }

    // ======= Menu =======
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_local_docs, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_set_home -> {
            setHomeToCurrent()
            true
        }
        R.id.action_go_home -> {
            goHome()
            true
        }
        R.id.action_search_home -> {
            showSearchDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showSearchDialog() {
        val startDoc = resolveToDoc(homeTreeUri ?: rootTreeUri)
        if (startDoc == null) {
            Toast.makeText(this, "Pick a folder first", Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(this).apply {
            hint = "e.g., manual or .pdf"
            setSingleLine(true)
        }
        AlertDialog.Builder(this)
            .setTitle("Search in Home")
            .setView(input)
            .setPositiveButton("Search") { _, _ ->
                val q = input.text.toString().trim().lowercase()
                if (q.isEmpty()) return@setPositiveButton
                startSearch(startDoc, q)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ======= Core actions =======
    private fun chooseFolder() {
        val treeIntent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT_TREE)
        val canPickTree = treeIntent.resolveActivity(packageManager) != null
        if (canPickTree) {
            try {
                openTreeLauncher.launch(null); return
            } catch (_: Exception) { /* fall through */ }
        }
        storagePermLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun detectStorageRoots(): List<File> {
        val dirs = getExternalFilesDirs(null).filterNotNull()
        val roots = mutableListOf<File>()
        for (appDir in dirs) {
            val path = appDir.absolutePath
            val idx = path.indexOf("/Android/")
            val rootPath = if (idx > 0) path.substring(0, idx) else path
            val f = File(rootPath)
            if (f.exists() && f.isDirectory && roots.none { it.absolutePath == f.absolutePath }) {
                roots.add(f)
            }
        }
        return roots
    }

    private fun switchStorage() {
        if (storageRoots.isEmpty()) {
            Toast.makeText(this, "No storage volumes found", Toast.LENGTH_SHORT).show()
            return
        }
        storageIndex = (storageIndex + 1) % storageRoots.size
        val target = storageRoots[storageIndex]
        rootTreeUri = Uri.fromFile(target)
        stack.clear()
        openRoot()
        Toast.makeText(this, "Storage: ${target.absolutePath}", Toast.LENGTH_SHORT).show()
    }

    private fun openLegacyRoot() {
        val roots = storageRoots
        if (roots.isEmpty()) {
            Toast.makeText(this, "Storage not accessible", Toast.LENGTH_LONG).show()
            return
        }
        val target = roots[0]
        rootTreeUri = Uri.fromFile(target)
        openRoot()
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
        if (!showingSearch) {
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
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        }
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
            }
            else -> Toast.makeText(this, "Unsupported file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setHomeToCurrent() {
        val uri = (currentDir?.uri ?: rootTreeUri)
        if (uri == null) {
            Toast.makeText(this, "Open a folder first", Toast.LENGTH_SHORT).show()
            return
        }
        homeTreeUri = uri
        prefs.edit().putString(KEY_HOME_URI, uri.toString()).apply()
        Toast.makeText(this, "Home set", Toast.LENGTH_SHORT).show()
    }

    private fun goHome() {
        val doc = resolveToDoc(homeTreeUri)
        if (doc == null) {
            Toast.makeText(this, "Home is not set", Toast.LENGTH_SHORT).show()
            return
        }
        stack.clear()
        showingSearch = false
        currentDir = doc
        refreshList()
    }

    private fun startSearch(startDoc: DocumentFile, query: String) {
        Toast.makeText(this, "Searching…", Toast.LENGTH_SHORT).show()
        Thread {
            val results = mutableListOf<DocumentFile>()
            try {
                searchDocuments(startDoc, query, results, 1000)
            } catch (_: Throwable) { /* ignore */ }

            runOnUiThread {
                showingSearch = true
                currentChildren = results
                txtPath.text = "Search: \"$query\" (${results.size})"
                val names = results.map { "🔎  " + (it.name ?: "(unnamed)") }
                listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
                if (results.isEmpty()) {
                    Toast.makeText(this, "No matches", Toast.LENGTH_LONG).show()
                } else if (results.size >= 1000) {
                    Toast.makeText(this, "Showing first 1000 results", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    private fun searchDocuments(root: DocumentFile, q: String, out: MutableList<DocumentFile>, limit: Int) {
        if (out.size >= limit) return
        val kids = root.listFiles()
        for (c in kids) {
            if (out.size >= limit) return
            if (c.isDirectory) {
                searchDocuments(c, q, out, limit)
            } else {
                val name = c.name?.lowercase() ?: continue
                if (isSupportedFile(c) && name.contains(q)) out.add(c)
            }
        }
    }

    private fun resolveToDoc(uri: Uri?): DocumentFile? = when (uri?.scheme) {
        "content" -> DocumentFile.fromTreeUri(this, uri)
        "file" -> DocumentFile.fromFile(File(uri.path!!))
        else -> null
    }

    companion object {
        private const val KEY_TREE_URI = "tree_uri"
        private const val KEY_HOME_URI = "home_uri"
    }
}
