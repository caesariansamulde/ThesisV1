package com.example.thesisv1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.data.ResourceNeedsReport
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ResourceNeedsReportActivity : AppCompatActivity() {

    private var latitude = 0.0
    private var longitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resource_needs_report)

        val profileId = intent.getStringExtra("profileId") ?: return
        val dao = DisasterDatabase.getDatabase(this).resourceNeedsReportDao()

        val remarksInput = findViewById<EditText>(R.id.remarksInput)
        val locationText = findViewById<TextView>(R.id.locationText)

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            val fused = LocationServices.getFusedLocationProviderClient(this)
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                Manifest.permission.ACCESS_FINE_LOCATION else Manifest.permission.ACCESS_COARSE_LOCATION

            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 100)
                return@setOnClickListener
            }

            fused.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                    locationText.text = "Location: $latitude, $longitude"
                }
            }
        }

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val food = collectSelections(
                R.id.checkFoodRice, R.id.checkFoodCanned, R.id.checkFoodWater,
                R.id.checkFoodBaby, R.id.checkFoodOther, R.id.foodOtherInput
            )

            val nonFood = collectSelections(
                R.id.checkNonClothing, R.id.checkNonHygiene, R.id.checkNonBlankets,
                R.id.checkNonUtensils, R.id.checkNonOther, R.id.nonFoodOtherInput
            )

            val medical = collectSelections(
                R.id.checkMedFirstAid, R.id.checkMedMeds, R.id.checkMedPersonnel,
                R.id.checkMedOther, R.id.medicalOtherInput
            )

            val shelter = collectSelections(
                R.id.checkShelterTents, R.id.checkShelterTarps, R.id.checkShelterMats,
                R.id.checkShelterOther, R.id.shelterOtherInput
            )

            val manpower = collectSelections(
                R.id.checkManRescue, R.id.checkManMedical, R.id.checkManMaterials,
                R.id.checkManTransport, R.id.checkManOther, R.id.manpowerOtherInput
            )

            val priority = when (findViewById<RadioGroup>(R.id.priorityGroup).checkedRadioButtonId) {
                R.id.radioHigh -> "High"
                R.id.radioMedium -> "Medium"
                R.id.radioLow -> "Low"
                else -> "Unspecified"
            }

            val timeStamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
            val reportId = "RESOURCE_${timeStamp}_${UUID.randomUUID().toString().take(8)}"

            val report = ResourceNeedsReport(
                id = reportId,
                disasterProfileId = profileId,
                food = food,
                nonFood = nonFood,
                medical = medical,
                shelter = shelter,
                manpower = manpower,
                priority = priority,
                remarks = remarksInput.text.toString().takeIf { it.isNotBlank() },
                latitude = latitude,
                longitude = longitude
            )

            val summary = """
                Report ID: $reportId
                Priority: $priority
                Location: $latitude, $longitude
                Food: ${report.food}
                Non-Food: ${report.nonFood}
                Medical: ${report.medical}
                Shelter: ${report.shelter}
                Manpower: ${report.manpower}
                Remarks: ${report.remarks ?: "None"}
            """.trimIndent()

            AlertDialog.Builder(this)
                .setTitle("Confirm Submission")
                .setMessage(summary)
                .setPositiveButton("Submit") { _, _ ->
                    lifecycleScope.launch {
                        dao.insertReport(report)
                        Toast.makeText(this@ResourceNeedsReportActivity, "Report saved", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun collectSelections(
        vararg ids: Int
    ): String {
        val selections = mutableListOf<String>()
        for (i in ids.indices) {
            val id = ids[i]
            val view = findViewById<android.view.View>(id)
            if (view is CheckBox && view.isChecked) {
                selections.add(view.text.toString())
            } else if (view is EditText && ids.getOrNull(i - 1)?.let { findViewById<CheckBox>(it).isChecked } == true) {
                if (view.text.toString().isNotBlank()) {
                    selections.add("Other: ${view.text}")
                }
            }
        }
        return selections.joinToString(", ")
    }
}
