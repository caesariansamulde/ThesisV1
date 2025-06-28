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

class ViewUtilityDisruptionReportActivity : AppCompatActivity() {

    private lateinit var reportIdText: TextView
    private lateinit var facilityText: TextView
    private lateinit var disruptionText: TextView
    private lateinit var disruptionDetailsText: TextView
    private lateinit var remarksText: TextView
    private lateinit var locationText: TextView
    private lateinit var photoContainer: LinearLayout
    private lateinit var btnGeneratePdf: Button

    private val tagMapFacility = mapOf(
        1 to "Electricity",
        2 to "Water Supply",
        3 to "Telecommunications",
        4 to "Fuel Supply",
        5 to "Sanitation",
        6 to "Other Utility"
    )

    private val tagMapDisruption = mapOf(
        1 to "Fully Operational",
        2 to "Partially Disrupted",
        3 to "Fully Disrupted",
        4 to "Unknown"
    )

    private val tagMapDisruptionDetail = mapOf(
        "1" to "Physical Damage",
        "2" to "Infrastructure Failure",
        "3" to "Logistical Blockage",
        "4" to "Safety Hazard",
        "5" to "Other"
    )

    @SuppressLint("SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_utility_disruption_report)

        val reportId = intent.getStringExtra("reportId") ?: return
        val dao = DisasterDatabase.getDatabase(this).utilityDisruptionReportDao()

        // View bindings
        reportIdText = findViewById(R.id.reportIdText)
        facilityText = findViewById(R.id.facilityTypeText)
        disruptionText = findViewById(R.id.statusText)
        disruptionDetailsText = findViewById(R.id.detailsText)
        remarksText = findViewById(R.id.remarksText)
        locationText = findViewById(R.id.locationText)
        photoContainer = findViewById(R.id.photoPreviewContainer)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)

        lifecycleScope.launch {
            val report = dao.getReportById(reportId) ?: return@launch

            reportIdText.text = "Report ID: ${report.reportId}"
            facilityText.text = "Facility Type: ${tagMapFacility[report.utilityType] ?: "Unknown"}"
            disruptionText.text = "Status: ${tagMapDisruption[report.status] ?: "Unknown"}"
            remarksText.text = "Remarks: ${report.remarks ?: "None"}"
            locationText.text = "Lat: ${report.latitude}, Lng: ${report.longitude}"

            // Parse disruption details
            val details = report.disruptionDetails.split(",").mapNotNull { code ->
                val trimmed = code.trim()
                if (trimmed.contains(":")) {
                    val parts = trimmed.split(":", limit = 2)
                    if (parts[0] == "5") "Other: ${parts[1]}" else null
                } else {
                    tagMapDisruptionDetail[trimmed]
                }
            }

            disruptionDetailsText.text = "Details: ${if (details.isEmpty()) "None" else details.joinToString(", ")}"

            // Load photos
            photoContainer.removeAllViews()
            listOf(report.photo1, report.photo2, report.photo3).forEach { path ->
                path?.let {
                    val file = File(it)
                    if (file.exists()) {
                        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                        val imageView = ImageView(this@ViewUtilityDisruptionReportActivity).apply {
                            layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                                setMargins(8, 8, 8, 8)
                            }
                            setImageBitmap(bitmap)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        photoContainer.addView(imageView)
                    }
                }
            }

            // PDF generation
            btnGeneratePdf.setOnClickListener {
                val content = """
                    Utility Disruption Report
                    Report ID: ${report.reportId}
                    
                    Facility Type: ${tagMapFacility[report.utilityType]}
                    Status: ${tagMapDisruption[report.status]}
                    Disruption Details: ${details.joinToString(", ")}
                    Remarks: ${report.remarks ?: "None"}
                    Location: Lat ${report.latitude}, Lng ${report.longitude}
                    Photos attached: ${listOf(report.photo1, report.photo2, report.photo3).count { it != null }}
                    Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(report.timestamp)}
                """.trimIndent()

                generatePDF(
                    context = this@ViewUtilityDisruptionReportActivity,
                    fileName = "Utility_Report_${report.reportId}",
                    content = content,
                    photoPaths = listOf(report.photo1, report.photo2, report.photo3)
                )
            }
        }
    }
}
