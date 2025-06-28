package com.example.thesisv1

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.AccessibilityReport
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.util.BitPackingEncoder
import com.example.thesisv1.util.BluetoothTransmitter
import com.example.thesisv1.util.copyUriToInternalStorage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

class AccessibilityReportActivity : AppCompatActivity() {

    private var latitude = 0.0
    private var longitude = 0.0
    private val imageUris = mutableListOf<Uri>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            imageUris.clear()
            imageUris.addAll(uris.take(3))
            showPhotoPreviews()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accessibility_report)

        val profileId = intent.getStringExtra("profileId") ?: return
        val dao = DisasterDatabase.getDatabase(this).accessibilityReportDao()

        val locationText = findViewById<TextView>(R.id.locationText)
        val assessedBy = findViewById<EditText>(R.id.assessedBy)
        val altDesc = findViewById<EditText>(R.id.altRouteDesc)
        val remarks = findViewById<EditText>(R.id.remarks)

        // Get location
        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            val fused = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
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

        // Pick images
        findViewById<Button>(R.id.btnAddPhoto).setOnClickListener {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(perm), 101)
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }

        // Submit
        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val facility = findViewById<RadioGroup>(R.id.facilityTypeGroup)
                .findViewById<RadioButton>(
                    findViewById<RadioGroup>(R.id.facilityTypeGroup).checkedRadioButtonId
                )?.tag?.toString()?.toIntOrNull() ?: 0

            val status = findViewById<RadioGroup>(R.id.statusGroup)
                .findViewById<RadioButton>(
                    findViewById<RadioGroup>(R.id.statusGroup).checkedRadioButtonId
                )?.tag?.toString()?.toIntOrNull() ?: 0

            val obstructions = mutableListOf<Int>()
            if (findViewById<CheckBox>(R.id.checkFlooding).isChecked) obstructions.add(1)
            if (findViewById<CheckBox>(R.id.checkLandslide).isChecked) obstructions.add(2)
            if (findViewById<CheckBox>(R.id.checkTrees).isChecked) obstructions.add(3)
            if (findViewById<CheckBox>(R.id.checkDebris).isChecked) obstructions.add(4)
            if (findViewById<CheckBox>(R.id.checkBridgeDamage).isChecked) obstructions.add(5)
            if (findViewById<CheckBox>(R.id.checkOther).isChecked) obstructions.add(99)

            val otherObstruction = findViewById<EditText>(R.id.otherObstructionText).text.toString()
                .takeIf { it.isNotBlank() }

            val hasAlt = findViewById<RadioGroup>(R.id.alternativeGroup).checkedRadioButtonId == R.id.radioAltYes
            val altDescValue = altDesc.text.toString().takeIf { it.isNotBlank() }
            val remarksValue = remarks.text.toString().takeIf { it.isNotBlank() }
            val assessedByVal = assessedBy.text.toString().takeIf { it.isNotBlank() }

            // Generate Report ID
            val abbrev = when (facility) {
                1 -> "MAI"
                2 -> "NAT"
                3 -> "ACC"
                4 -> "BRI"
                else -> "UNK"
            }
            val idSuffix = (1..4).map { ('A'..'Z') + ('a'..'z') + ('0'..'9') }.flatten().shuffled().take(4).joinToString("")
            val reportId = "ACC_${abbrev}_$idSuffix"

            // Copy images first
            lifecycleScope.launch {
                val savedImages = imageUris.mapIndexedNotNull { index, uri ->
                    val name = "access_${System.currentTimeMillis()}_$index.jpg"
                    copyUriToInternalStorage(applicationContext, uri, name)
                }

                val report = AccessibilityReport(
                    reportId = reportId,
                    disasterProfileId = profileId,
                    facilityType = facility,
                    status = status,
                    obstructions = obstructions,
                    otherObstruction = otherObstruction,
                    alternativeRouteAvailable = hasAlt,
                    alternativeRouteDescription = altDescValue,
                    remarks = remarksValue,
                    assessedBy = assessedByVal,
                    latitude = latitude,
                    longitude = longitude,
                    photo1 = savedImages.getOrNull(0),
                    photo2 = savedImages.getOrNull(1),
                    photo3 = savedImages.getOrNull(2)
                )

                AlertDialog.Builder(this@AccessibilityReportActivity)
                    .setTitle("Confirm Submission")
                    .setMessage("Facility: $facility | Status: $status | Photos: ${savedImages.size}")
                    .setPositiveButton("Submit") { _, _ ->
                        lifecycleScope.launch {
                            dao.insertReport(report)

                            val packed = BitPackingEncoder.encodeAccessibilityReport(report)
                            val file = File(filesDir, "${report.reportId}.bin")
                            file.writeBytes(packed)

                            val success = withContext(Dispatchers.IO) {
                                BluetoothTransmitter.sendBinFileToESP32A(file)
                            }

                            if (success) {
                                Toast.makeText(this@AccessibilityReportActivity, "📤 Transmitted to ESP32A", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@AccessibilityReportActivity, "⚠️ Bluetooth transmit failed", Toast.LENGTH_LONG).show()
                            }

                            Toast.makeText(this@AccessibilityReportActivity, "Report submitted", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    private fun showPhotoPreviews() {
        val container = findViewById<LinearLayout>(R.id.photoPreviewContainer)
        container.removeAllViews()

        imageUris.forEachIndexed { index, uri ->
            val frame = FrameLayout(this)
            frame.layoutParams = LinearLayout.LayoutParams(240, 240).apply {
                setMargins(8, 8, 8, 8)
            }

            val iv = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val del = ImageButton(this).apply {
                layoutParams = FrameLayout.LayoutParams(70, 70).apply {
                    setMargins(0, 8, 8, 0)
                    gravity = android.view.Gravity.END
                }
                setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                background = null
                setOnClickListener {
                    imageUris.removeAt(index)
                    container.removeView(frame)
                }
            }

            frame.addView(iv)
            frame.addView(del)
            container.addView(frame)
        }
    }
}
