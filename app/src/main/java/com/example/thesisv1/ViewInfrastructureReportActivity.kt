package com.example.thesisv1

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

class ViewInfrastructureReportActivity : AppCompatActivity() {

    private lateinit var reportIdText: TextView
    private lateinit var facilityText: TextView
    private lateinit var damageText: TextView
    private lateinit var remarksText: TextView
    private lateinit var locationText: TextView
    private lateinit var photoContainer: LinearLayout
    private lateinit var btnGeneratePdf: Button

    private val tagMapFacility = mapOf(
        1 to "Residential House",
        2 to "Commercial Building",
        3 to "School",
        4 to "Hospitals",
        5 to "Government Buildings",
        6 to "Other Public Infrastructure"
    )

    private val tagMapDamage = mapOf(
        1 to "Partially Damaged",
        2 to "Totally Damaged"
    )

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_infrastructure_report)

        val reportId = intent.getStringExtra("reportId") ?: return
        val dao = DisasterDatabase.getDatabase(this).infrastructureReportDao()

        reportIdText = findViewById(R.id.reportIdText)
        facilityText = findViewById(R.id.facilityTypeText)
        damageText = findViewById(R.id.damageText)
        remarksText = findViewById(R.id.remarksText)
        locationText = findViewById(R.id.locationText)
        photoContainer = findViewById(R.id.photoPreviewContainer)
        btnGeneratePdf = findViewById(R.id.btnGeneratePdf)

        lifecycleScope.launch {
            val report = dao.getReportById(reportId) ?: return@launch
            reportIdText.text = "Report ID: ${report.reportId}"
            facilityText.text = tagMapFacility[report.facilityType] ?: "Unknown"
            damageText.text = tagMapDamage[report.damage] ?: "Unknown"
            remarksText.text = report.remarks ?: "None"
            locationText.text = "Lat: ${report.latitude}, Lng: ${report.longitude}"

            // Load photos from internal storage
            photoContainer.removeAllViews()
            listOf(report.photo1, report.photo2, report.photo3).forEach { path ->
                path?.let {
                    try {
                        val file = File(it)
                        if (file.exists()) {
                            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                            val imageView = ImageView(this@ViewInfrastructureReportActivity).apply {
                                layoutParams = LinearLayout.LayoutParams(300, 300).apply {
                                    setMargins(8, 8, 8, 8)
                                }
                                setImageBitmap(bitmap)
                                scaleType = ImageView.ScaleType.CENTER_CROP
                            }
                            photoContainer.addView(imageView)
                        } else {
                            Toast.makeText(
                                this@ViewInfrastructureReportActivity,
                                "Image not found: $path",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(
                            this@ViewInfrastructureReportActivity,
                            "Error loading image.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            // Generate PDF
            btnGeneratePdf.setOnClickListener {
                val content = """
        Infrastructure Report
        Report ID: ${report.reportId}
        
        Facility Type: ${tagMapFacility[report.facilityType]}
        Damage Description: ${tagMapDamage[report.damage]}
        Remarks: ${report.remarks ?: "None"}
        Location: Lat ${report.latitude}, Lng ${report.longitude}
        Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(report.timestamp)}
    """.trimIndent()

                val photoPaths = listOf(report.photo1, report.photo2, report.photo3)
                generatePDF(
                    context = this@ViewInfrastructureReportActivity,
                    fileName = "Infrastructure_Report_${report.reportId}",
                    content = content,
                    photoPaths = photoPaths
                )
            }


        }
    }
}
