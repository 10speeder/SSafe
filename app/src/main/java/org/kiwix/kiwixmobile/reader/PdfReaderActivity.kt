package org.kiwix.kiwixmobile.reader

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.barteksc.pdfviewer.PDFView
import org.kiwix.kiwixmobile.R

class PdfReaderActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_reader)

        val uri: Uri = intent?.data ?: run { finish(); return }
        findViewById<PDFView>(R.id.pdfView)
            .fromUri(uri)
            .enableSwipe(true)
            .swipeHorizontal(false)
            .spacing(8)
            .load()
    }
}
