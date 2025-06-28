package com.example.thesisv1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.CasualtyReport
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.util.BitPackingEncoder
import com.example.thesisv1.util.BluetoothTransmitter
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class CasualtyReportActivity : AppCompatActivity() {

    private var latitude = 0.0
    private var longitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_casualty_report)

        requestBluetoothPermissionsIfNeeded()

        val profileId = intent.getStringExtra("profileId") ?: return
        val dao = DisasterDatabase.getDatabase(this).casualtyReportDao()

        val remarksInput = findViewById<EditText>(R.id.remarksInput)
        val locationText = findViewById<TextView>(R.id.locationText)
        val resourceNeedsLayout = findViewById<LinearLayout>(R.id.resourceNeedsLayout)

        val needResourcesGroup = findViewById<RadioGroup>(R.id.needResourcesGroup)
        var needResources = false
        needResourcesGroup.setOnCheckedChangeListener { _, checkedId ->
            needResources = checkedId == R.id.radioNeedYes
            resourceNeedsLayout.visibility = if (needResources) LinearLayout.VISIBLE else LinearLayout.GONE
        }

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
            val affectedFamilies = findViewById<EditText>(R.id.affectedFamilies).text.toString().toIntOrNull() ?: 0
            val affectedPersons = findViewById<EditText>(R.id.affectedPersons).text.toString().toIntOrNull() ?: 0
            val age0to2 = findViewById<EditText>(R.id.age0to2).text.toString().toIntOrNull() ?: 0
            val age3to5 = findViewById<EditText>(R.id.age3to5).text.toString().toIntOrNull() ?: 0
            val age6to12 = findViewById<EditText>(R.id.age6to12).text.toString().toIntOrNull() ?: 0
            val age13to17 = findViewById<EditText>(R.id.age13to17).text.toString().toIntOrNull() ?: 0
            val pwdCount = findViewById<EditText>(R.id.pwdCount).text.toString().toIntOrNull() ?: 0
            val elderlyCount = findViewById<EditText>(R.id.elderlyCount).text.toString().toIntOrNull() ?: 0
            val missingMale = findViewById<EditText>(R.id.missingMale).text.toString().toIntOrNull() ?: 0
            val missingFemale = findViewById<EditText>(R.id.missingFemale).text.toString().toIntOrNull() ?: 0
            val injuredMale = findViewById<EditText>(R.id.injuredMale).text.toString().toIntOrNull() ?: 0
            val injuredFemale = findViewById<EditText>(R.id.injuredFemale).text.toString().toIntOrNull() ?: 0
            val deadMale = findViewById<EditText>(R.id.deadMale).text.toString().toIntOrNull() ?: 0
            val deadFemale = findViewById<EditText>(R.id.deadFemale).text.toString().toIntOrNull() ?: 0
            val remarks = remarksInput.text.toString().takeIf { it.isNotBlank() }

            val food = if (needResources) getTagValues(
                listOf(R.id.checkFoodRice, R.id.checkFoodCanned, R.id.checkFoodWater, R.id.checkFoodBaby, R.id.checkFoodOther),
                R.id.foodOtherInput
            ) else null

            val nonFood = if (needResources) getTagValues(
                listOf(R.id.checkNonClothing, R.id.checkNonHygiene, R.id.checkNonBlankets, R.id.checkNonUtensils, R.id.checkNonOther),
                R.id.nonFoodOtherInput
            ) else null

            val medical = if (needResources) getTagValues(
                listOf(R.id.checkMedFirstAid, R.id.checkMedMeds, R.id.checkMedPersonnel, R.id.checkMedOther),
                R.id.medicalOtherInput
            ) else null

            val shelter = if (needResources) getTagValues(
                listOf(R.id.checkShelterTents, R.id.checkShelterTarps, R.id.checkShelterMats, R.id.checkShelterOther),
                R.id.shelterOtherInput
            ) else null

            val manpower = if (needResources) getTagValues(
                listOf(R.id.checkManRescue, R.id.checkManMedical, R.id.checkManMaterials, R.id.checkManTransport, R.id.checkManOther),
                R.id.manpowerOtherInput
            ) else null

            val priority = if (needResources) {
                val checkedId = findViewById<RadioGroup>(R.id.priorityGroup).checkedRadioButtonId
                findViewById<RadioButton?>(checkedId)?.tag?.toString()
            } else null

            val resourceFlag = if (needResources) "Y" else "N"
            val randomCode = (1..4).map { ('A'..'Z') + ('a'..'z') + ('0'..'9') }.flatten().shuffled().take(4).joinToString("")
            val reportId = "CAS_${resourceFlag}_$randomCode"

            val report = CasualtyReport(
                reportId = reportId,
                disasterProfileId = profileId,
                affectedFamilies = affectedFamilies,
                affectedPersons = affectedPersons,
                age0to2 = age0to2,
                age3to5 = age3to5,
                age6to12 = age6to12,
                age13to17 = age13to17,
                pwdCount = pwdCount,
                elderlyCount = elderlyCount,
                missingMale = missingMale,
                missingFemale = missingFemale,
                injuredMale = injuredMale,
                injuredFemale = injuredFemale,
                deadMale = deadMale,
                deadFemale = deadFemale,
                remarks = remarks,
                latitude = latitude,
                longitude = longitude,
                needResources = needResources,
                food = food,
                nonFood = nonFood,
                medical = medical,
                shelter = shelter,
                manpower = manpower,
                priority = priority
            )

            lifecycleScope.launch {
                dao.insertReport(report)

                // Save and transmit .bin
                val packed = BitPackingEncoder.encodeCasualtyReport(report)
                val file = File(filesDir, "${report.reportId}.bin")
                file.writeBytes(packed)

                val success = withContext(Dispatchers.IO) {
                    BluetoothTransmitter.sendBinFileToESP32A(file)
                }

                if (success) {
                    Toast.makeText(this@CasualtyReportActivity, "📤 Transmitted to ESP32A", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@CasualtyReportActivity, "⚠️ Bluetooth transmit failed", Toast.LENGTH_LONG).show()
                }

                Toast.makeText(this@CasualtyReportActivity, "Report submitted", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun getTagValues(ids: List<Int>, otherInputId: Int): String {
        val result = ids.mapNotNull { id ->
            val cb = findViewById<CheckBox>(id)
            if (cb.isChecked && cb.text != "Others") cb.tag?.toString() else null
        }.toMutableList()

        val othersCb = findViewById<CheckBox>(ids.last())
        val othersText = findViewById<EditText>(otherInputId).text.toString()
        if (othersCb.isChecked && othersText.isNotBlank()) {
            result.add("99:$othersText")
        }

        return result.joinToString(",")
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                101
            )
        }
    }

}
