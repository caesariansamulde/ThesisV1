package com.example.thesisv1

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.data.DisasterProfile
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CreateDisasterProfileActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var latitude = 0.0
    private var longitude = 0.0

    private val regionMap = mapOf(
        "Region I – Ilocos Region" to 1,
        "Region II – Cagayan Valley" to 2,
        "Region III – Central Luzon" to 3,
        "Region IV‑A – CALABARZON" to 4,
        "Region IV-B - MIMAROPA" to 5,
        "Region V – Bicol Region" to 6,
        "Region VI – Western Visayas" to 7,
        "Region VII – Central Visayas" to 8,
        "Region VIII – Eastern Visayas" to 9,
        "Region IX – Zamboanga Peninsula" to 10,
        "Region X – Northern Mindanao" to 11,
        "Region XI – Davao Region" to 12,
        "Region XII – SOCCSKSARGEN" to 13,
        "Region XIII – Caraga" to 14,
        "NCR – National Capital Region" to 15,
        "CAR – Cordillera Administrative Region" to 16,
        "BARMM – Bangsamoro Autonomous Region in Muslim Mindanao" to 17
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_disaster_profile)

        val db = DisasterDatabase.getDatabase(this)
        val dao = db.disasterProfileDao()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val btnLocation = findViewById<Button>(R.id.btnLocation)
        val btnSubmit = findViewById<Button>(R.id.btnSubmit)
        val locationText = findViewById<TextView>(R.id.locationText)
        val dateTime = findViewById<EditText>(R.id.dateTime)
        val regionSpinner = findViewById<Spinner>(R.id.regionSpinner)

        val disasterTypeInput = findViewById<EditText>(R.id.disasterType)
        val disasterNameInput = findViewById<EditText>(R.id.disasterName)

        // Set up date picker
        dateTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    calendar.set(year, month, dayOfMonth)
                    dateTime.setText(sdf.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        // Set up spinner
        val regionNames = regionMap.keys.toList()
        regionSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, regionNames)

        btnLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    1
                )
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        latitude = it.latitude
                        longitude = it.longitude
                        locationText.text = "Location: $latitude, $longitude"
                    }
                }
            }
        }

        btnSubmit.setOnClickListener {
            val type = disasterTypeInput.text.toString().trim()
            val name = disasterNameInput.text.toString().trim()

            if (type.isEmpty() || name.isEmpty()) {
                Toast.makeText(this, "Disaster type and name are required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedRegionName = regionSpinner.selectedItem.toString()
            val regionValue = regionMap[selectedRegionName] ?: 0

            val profileId = generateDisasterProfileId(type, name)

            val profile = DisasterProfile(
                profileId = profileId,
                type = type,
                name = name,
                dateTime = dateTime.text.toString(),
                region = regionValue,
                province = findViewById<EditText>(R.id.province).text.toString(),
                municipality = findViewById<EditText>(R.id.municipality).text.toString(),
                barangay = findViewById<EditText>(R.id.barangay).text.toString(),
                latitude = latitude,
                longitude = longitude
            )

            lifecycleScope.launch {
                dao.insertProfile(profile)
                Toast.makeText(this@CreateDisasterProfileActivity, "Profile saved!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@CreateDisasterProfileActivity, DisasterProfileListActivity::class.java))
                finish()
            }
        }
    }

    private fun generateDisasterProfileId(type: String, name: String): String {
        val cleanType = type.replace("\\s+".toRegex(), "").take(15)
        val cleanName = name.replace("\\s+".toRegex(), "").take(15)
        val suffix = (1..4)
            .map { ('A'..'Z') + ('0'..'9') }
            .flatten()
            .shuffled()
            .take(4)
            .joinToString("")
        return "$cleanType-$cleanName-$suffix"
    }
}
