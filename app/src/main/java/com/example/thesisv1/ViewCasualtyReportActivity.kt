package com.example.thesisv1

import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.CasualtyReport
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.util.ReportMsgPackSerializer
import com.example.thesisv1.util.generatePDF
import kotlinx.coroutines.launch

class ViewCasualtyReportActivity : AppCompatActivity() {

    private lateinit var reportId: String
    private lateinit var reportTextView: TextView
    private lateinit var btnGeneratePDF: Button
    private var report: CasualtyReport? = null

    private val tagMapPriority = mapOf(
        "1" to "High",
        "2" to "Medium",
        "3" to "Low"
    )

    private val tagMapResource = mapOf(
        "1" to "Rice",
        "2" to "Canned Goods",
        "3" to "Drinking Water",
        "4" to "Infant/Baby Needs",
        "11" to "Clothing",
        "12" to "Hygiene Kits",
        "13" to "Blankets/Mats",
        "14" to "Cooking Utensils",
        "21" to "First Aid Kit",
        "22" to "Medicines",
        "23" to "Medical Personnel",
        "31" to "Tents",
        "32" to "Tarpaulins",
        "33" to "Sleeping Mats",
        "41" to "Rescue Personnel",
        "42" to "Medical Teams",
        "43" to "Construction Materials",
        "44" to "Transportation",
        "99" to "Other"
    )

    private fun buildMinimalReportMap(report: CasualtyReport): Map<String, Any?> {
        return mutableMapOf<String, Any?>(
            "reportId" to report.reportId,
            "disasterProfileId" to report.disasterProfileId,
            "affectedFamilies" to report.affectedFamilies,
            "affectedPersons" to report.affectedPersons,
            "age0to2" to report.age0to2,
            "age3to5" to report.age3to5,
            "age6to12" to report.age6to12,
            "age13to17" to report.age13to17,
            "pwdCount" to report.pwdCount,
            "elderlyCount" to report.elderlyCount,
            "missingMale" to report.missingMale,
            "missingFemale" to report.missingFemale,
            "injuredMale" to report.injuredMale,
            "injuredFemale" to report.injuredFemale,
            "deadMale" to report.deadMale,
            "deadFemale" to report.deadFemale,
            "remarks" to report.remarks,
            "latitude" to report.latitude,
            "longitude" to report.longitude,
            "needResources" to if (report.needResources) 1 else 0,
            "food" to report.food,
            "nonFood" to report.nonFood,
            "medical" to report.medical,
            "shelter" to report.shelter,
            "manpower" to report.manpower,
            "priority" to report.priority,
            "timestamp" to report.timestamp.time
        )
    }


    private fun mapTagsToReadable(tags: String?, category: String): String {
        if (tags.isNullOrBlank()) return "None"
        return tags.split(",").joinToString(", ") { tag ->
            val parts = tag.split(":")
            val key = parts[0]
            val other = parts.getOrNull(1)
            when {
                key == "99" && other != null -> "Other: $other"
                category == "priority" -> tagMapPriority[key] ?: "Unknown"
                else -> tagMapResource[key] ?: "Unknown"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_casualty_report)

        reportTextView = findViewById(R.id.reportContent)
        btnGeneratePDF = findViewById(R.id.btnGeneratePDF)
        reportId = intent.getStringExtra("reportId") ?: return

        val dao = DisasterDatabase.getDatabase(this).casualtyReportDao()

        lifecycleScope.launch {
            report = dao.getReportById(reportId)
            report?.let { showReport(it) }
        }

        btnGeneratePDF.setOnClickListener {

            ReportMsgPackSerializer.serializeToMsgPack(
                context = this,
                fileName = report!!.reportId,
                data = buildMinimalReportMap(report!!)
            )

            report?.let {
                val content = buildPdfContent(it)
                generatePDF(this, "Casualty_Report_$reportId", content)
            }
        }
    }

    private fun showReport(report: CasualtyReport) {
        val builder = StringBuilder()

        builder.appendLine("Casualty Report ID: ${report.reportId}")
        builder.appendLine("Disaster Profile ID: ${report.disasterProfileId}")
        builder.appendLine("Affected Families: ${report.affectedFamilies}")
        builder.appendLine("Affected Persons: ${report.affectedPersons}")
        builder.appendLine("Age 0–2: ${report.age0to2}")
        builder.appendLine("Age 3–5: ${report.age3to5}")
        builder.appendLine("Age 6–12: ${report.age6to12}")
        builder.appendLine("Age 13–17: ${report.age13to17}")
        builder.appendLine("Persons with Disability (PWD): ${report.pwdCount}")
        builder.appendLine("Elderly: ${report.elderlyCount}")
        builder.appendLine("Missing Male: ${report.missingMale}")
        builder.appendLine("Missing Female: ${report.missingFemale}")
        builder.appendLine("Injured Male: ${report.injuredMale}")
        builder.appendLine("Injured Female: ${report.injuredFemale}")
        builder.appendLine("Dead Male: ${report.deadMale}")
        builder.appendLine("Dead Female: ${report.deadFemale}")
        builder.appendLine("Remarks: ${report.remarks ?: "None"}")
        builder.appendLine("Location: ${report.latitude}, ${report.longitude}")
        builder.appendLine("Needs Resources: ${if (report.needResources) "Yes" else "No"}")

        if (report.needResources) {
            builder.appendLine("Food: ${mapTagsToReadable(report.food, "resource")}")
            builder.appendLine("Non-Food: ${mapTagsToReadable(report.nonFood, "resource")}")
            builder.appendLine("Medical: ${mapTagsToReadable(report.medical, "resource")}")
            builder.appendLine("Shelter: ${mapTagsToReadable(report.shelter, "resource")}")
            builder.appendLine("Manpower/Equipment: ${mapTagsToReadable(report.manpower, "resource")}")
            builder.appendLine("Priority Level: ${mapTagsToReadable(report.priority, "priority")}")
        }

        reportTextView.text = builder.toString()
    }

    private fun buildPdfContent(report: CasualtyReport): String {
        return reportTextView.text.toString()
    }
}
