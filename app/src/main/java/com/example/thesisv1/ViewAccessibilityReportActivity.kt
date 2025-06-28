package com.example.thesisv1

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.util.generatePDF
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ViewAccessibilityReportActivity : AppCompatActivity() {

    private lateinit var reportIdText: TextView
    private lateinit var facilityTypeText: TextView
    private lateinit var statusText: TextView
    private lateinit var obstructionsText: TextView
    private lateinit var alternativeText: TextView
    private lateinit var remarksText: TextView
    private lateinit var locationText: TextView
    private lateinit var photoContainer: LinearLayout
    private lateinit var btnGeneratePdf: Button

    // Tag maps (same structure as in Infrastructure)
    private val tagMapFacility = mapOf(
        1 to "Main Road",
        2 to "National Road",
        3 to "Access Road to Affected Area",
        4 to "Bridge"
    )

    private val tagMapStatus = mapOf(
        1 to "Passable to All Vehicles",
        2 to "Passable to Light Vehicles Only",
        3 to "Passable by Foot Only",
        4 to "Not Passable",
        5 to "Unknown"
    )

    private val tagMapObstructions = mapOf(
        1 to "Flooding",
        2 to "Landslide",
        3 to "Fallen Trees",
        4 to "Debris",
        5 to "Damaged Bridge/Infrastructure",
        99 to "Other"
    )

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_accessibility_report)

        val reportId = intent.getStringExtra("reportId") ?: return
        val dao = DisasterDatabase.getDatabase(this).accessibilityReportDao()

        // Bind views
        reportIdText = findViewById(R.id.reportIdText)
        facilityTypeText = findViewById(R.id.facilityTypeText)
        statusText = findViewById(R.id.statusText)
        obstructionsText = findViewById(R.id.obstructionsText)
        alternativeText = findViewById(R.id.alternativeText)
        remarksText = findViewById(R.id.remarksText)
        locationText = findViewById(R.id.locationText)
        photoContainer = findViewById(R.id.photoPreviewContainer)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)

        lifecycleScope.launch {
            val report = dao.getReportById(reportId) ?: return@launch

            // Map facility/status
            val facilityLabel = tagMapFacility[report.facilityType] ?: "Unknown"
            val statusLabel = tagMapStatus[report.status] ?: "Unknown"

            // Map obstruction values
            val obstructionLabels = report.obstructions.map {
                if (it == 99 && !report.otherObstruction.isNullOrBlank()) {
                    "Other: ${report.otherObstruction}"
                } else {
                    tagMapObstructions[it] ?: "Unknown"
                }
            }.joinToString(", ")

            // Set texts
            reportIdText.text = "Report ID: ${report.reportId}"
            facilityTypeText.text = "Facility Type: $facilityLabel"
            statusText.text = "Status: $statusLabel"
            obstructionsText.text = "Obstructions: $obstructionLabels"
            alternativeText.text = "Alternative Access: ${if (report.alternativeRouteAvailable) "Yes" else "No"}\n${report.alternativeRouteDescription ?: ""}"
            remarksText.text = "Remarks: ${report.remarks ?: "None"}"
            locationText.text = "Lat: ${report.latitude}, Lng: ${report.longitude}"

            // Load photo previews
            val photoPaths = listOf(report.photo1, report.photo2, report.photo3)
            photoContainer.removeAllViews()
            photoPaths.forEach { path ->
                path?.let {
                    val file = File(it)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        val iv = ImageView(this@ViewAccessibilityReportActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                                setMargins(8, 8, 8, 8)
                            }
                            setImageBitmap(bitmap)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        photoContainer.addView(iv)
                    }
                }
            }

            // PDF
            btnGeneratePdf.setOnClickListener {
                val content = """
                    Accessibility Report
                    Report ID: ${report.reportId}
                    
                    Facility Type: $facilityLabel
                    Status: $statusLabel
                    Obstructions: $obstructionLabels
                    Alternative Access: ${if (report.alternativeRouteAvailable) "Yes" else "No"}
                    Description: ${report.alternativeRouteDescription ?: "None"}
                    Remarks: ${report.remarks ?: "None"}
                    Location: Lat ${report.latitude}, Lng ${report.longitude}
                    Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(report.timestamp)}
                """.trimIndent()

                generatePDF(
                    context = this@ViewAccessibilityReportActivity,
                    fileName = "Accessibility_Report_${report.reportId}",
                    content = content,
                    photoPaths = photoPaths
                )
            }
        }
    }
}
