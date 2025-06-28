package com.example.thesisv1

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.thesisv1.data.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class AllReportsActivity : AppCompatActivity() {

    private lateinit var reportListView: ListView
    private lateinit var sortSpinner: Spinner
    private lateinit var chipAll: CheckBox
    private lateinit var chipCasualty: CheckBox
    private lateinit var chipInfrastructure: CheckBox
    private lateinit var chipUtility: CheckBox
    private lateinit var chipAccessibility: CheckBox

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val allReports = mutableListOf<ReportItem>()
    private val displayedReports = mutableListOf<ReportItem>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_reports)

        val profileId = intent.getStringExtra("profileId") ?: return

        reportListView = findViewById(R.id.reportListView)
        sortSpinner = findViewById(R.id.sortSpinner)
        chipAll = findViewById(R.id.chipAll)
        chipCasualty = findViewById(R.id.chipCasualty)
        chipInfrastructure = findViewById(R.id.chipInfrastructure)
        chipUtility = findViewById(R.id.chipUtility)
        chipAccessibility = findViewById(R.id.chipAccessibility)

        val chipList = listOf(chipCasualty, chipInfrastructure, chipUtility, chipAccessibility)
        chipAll.isChecked = true

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        reportListView.adapter = adapter

        val updateDisplay: () -> Unit = {
            val selectedTypes = mutableSetOf<String>()
            if (!chipAll.isChecked) {
                if (chipCasualty.isChecked) selectedTypes.add("Casualty")
                if (chipInfrastructure.isChecked) selectedTypes.add("Infrastructure")
                if (chipUtility.isChecked) selectedTypes.add("Utility")
                if (chipAccessibility.isChecked) selectedTypes.add("Accessibility")
            }

            displayedReports.clear()
            displayedReports.addAll(
                if (chipAll.isChecked) {
                    allReports
                } else {
                    allReports.filter { it.type in selectedTypes }
                }
            )

            val sorted = when (sortSpinner.selectedItemPosition) {
                0 -> displayedReports.sortedByDescending { it.timestamp }
                1 -> displayedReports.sortedBy { it.timestamp }
                else -> displayedReports
            }

            displayedReports.clear()
            displayedReports.addAll(sorted)

            adapter.clear()
            adapter.addAll(displayedReports.map {
                "${it.type} Report\n${dateFormat.format(it.timestamp)}\nID: ${it.id}"
            })
        }

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) = updateDisplay()
            override fun onNothingSelected(p: AdapterView<*>?) {}
        }

        chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) chipList.forEach { it.isChecked = false }
            updateDisplay()
        }

        chipList.forEach { chip ->
            chip.setOnCheckedChangeListener { _, _ ->
                chipAll.isChecked = chipList.none { it.isChecked }
                updateDisplay()
            }
        }

        reportListView.setOnItemClickListener { _, _, position, _ ->
            val selected = displayedReports.getOrNull(position) ?: return@setOnItemClickListener
            val intent = when (selected.type) {
                "Casualty" -> Intent(this, ViewCasualtyReportActivity::class.java)
                "Infrastructure" -> Intent(this, ViewInfrastructureReportActivity::class.java)
                "Utility" -> Intent(this, ViewUtilityDisruptionReportActivity::class.java)
                "Accessibility" -> Intent(this, ViewAccessibilityReportActivity::class.java)
                else -> null
            }
            intent?.putExtra("reportId", selected.id)
            intent?.let { startActivity(it) }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val db = DisasterDatabase.getDatabase(applicationContext)

            val casualty = db.casualtyReportDao().getReportsByProfileId(profileId)
                .map { ReportItem("Casualty", it.reportId, it.timestamp) }
            val infra = db.infrastructureReportDao().getReportsByProfileId(profileId)
                .map { ReportItem("Infrastructure", it.reportId, it.timestamp) }
            val utility = db.utilityDisruptionReportDao().getReportsByProfileId(profileId)
                .map { ReportItem("Utility", it.reportId, it.timestamp) }
            val access = db.accessibilityReportDao().getReportsByProfileId(profileId)
                .map { ReportItem("Accessibility", it.reportId, it.timestamp) }

            allReports.clear()
            allReports.addAll(casualty + infra + utility + access)

            withContext(Dispatchers.Main) {
                updateDisplay()
            }
        }
    }

    data class ReportItem(
        val type: String,
        val id: String,
        val timestamp: Date
    )
}
