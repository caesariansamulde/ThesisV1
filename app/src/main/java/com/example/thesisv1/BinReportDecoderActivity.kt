package com.example.thesisv1

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.thesisv1.util.BitPackingDecoder
import java.io.File

class BinReportDecoderActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var contentTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bin_decoder)

        listView = findViewById(R.id.fileList)
        contentTextView = findViewById(R.id.decodedContent)

        val binFiles = filesDir.listFiles { file -> file.name.endsWith(".bin") }?.toList() ?: emptyList()

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, binFiles.map { it.name })
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val file = binFiles[position]
            val fileName = file.name

            val decoded = try {
                when {
                    fileName.startsWith("CAS_") -> BitPackingDecoder.decodeCasualtyReportBin(file)
                    fileName.startsWith("INF_") -> BitPackingDecoder.decodeInfrastructureReportBin(file)
                    fileName.startsWith("ACC_") -> BitPackingDecoder.decodeAccessibilityReportBin(file)
                    fileName.startsWith("UTI_") -> BitPackingDecoder.decodeUtilityReportBin(file)
                    else -> "❌ Unknown report type in file: $fileName"
                }
            } catch (e: Exception) {
                "❌ Error decoding file $fileName:\n${e.message}"
            }

            contentTextView.text = decoded
        }
    }
}
