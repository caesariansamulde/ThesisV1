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
import com.example.thesisv1.data.InfrastructureReport
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.util.BitPackingEncoder
import com.example.thesisv1.util.BluetoothTransmitter
import com.example.thesisv1.util.copyUriToInternalStorage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import com.example.thesisv1.util.copyUriToInternalStorage
import java.io.File

import java.util.*

class InfrastructureReportActivity : AppCompatActivity() {

    private var latitude = 0.0
    private var longitude = 0.0
    private val imageUris = mutableListOf<Uri>()

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            if (!uris.isNullOrEmpty()) {
                imageUris.clear()
                imageUris.addAll(uris.take(3))
                showPhotoPreviews()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_infrastructure_report)

        val profileId = intent.getStringExtra("profileId") ?: return
        val dao = DisasterDatabase.getDatabase(this).infrastructureReportDao()

        findViewById<Button>(R.id.btnGetLocation).setOnClickListener {
            val fused = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
                return@setOnClickListener
            }
            fused.lastLocation.addOnSuccessListener { loc: Location? ->
                loc?.let {
                    latitude = it.latitude
                    longitude = it.longitude
                    findViewById<TextView>(R.id.locationText).text = "Location: $latitude, $longitude"
                }
            }
        }

        findViewById<Button>(R.id.btnAddPhoto).setOnClickListener {
            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(perm), 101)
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val facilityTag = findViewById<RadioGroup>(R.id.facilityTypeGroup)
                .findViewById<RadioButton>(
                    findViewById<RadioGroup>(R.id.facilityTypeGroup).checkedRadioButtonId
                )?.tag?.toString()?.toIntOrNull() ?: 0

            val damageTag = findViewById<RadioGroup>(R.id.damageGroup)
                .findViewById<RadioButton>(
                    findViewById<RadioGroup>(R.id.damageGroup).checkedRadioButtonId
                )?.tag?.toString()?.toIntOrNull() ?: 0

            val remarks = findViewById<EditText>(R.id.remarksInput).text.toString().takeIf { it.isNotBlank() }

            val facilityTypeLabel = when (facilityTag) {
                1 -> "Residential"
                2 -> "Commercial"
                3 -> "School"
                4 -> "Hospital"
                5 -> "Government"
                6 -> "PublicInfra"
                else -> "Unknown"
            }

            val abbrev = facilityTypeLabel.take(3).uppercase()
            val chars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
            val shortId = (1..4).map { chars.random() }.joinToString("")
            val reportId = "INF_${abbrev}_$shortId"

            // Copy images to internal storage
            lifecycleScope.launch {
                val filePaths = imageUris.mapIndexedNotNull { index, uri ->
                    val filename = "infra_${System.currentTimeMillis()}_$index.jpg"
                    copyUriToInternalStorage(this@InfrastructureReportActivity, uri, filename)

                }

                val report = InfrastructureReport(
                    reportId = reportId,
                    disasterProfileId = profileId,
                    facilityType = facilityTag,
                    damage = damageTag,
                    remarks = remarks,
                    latitude = latitude,
                    longitude = longitude,
                    photo1 = filePaths.getOrNull(0),
                    photo2 = filePaths.getOrNull(1),
                    photo3 = filePaths.getOrNull(2)
                )

                AlertDialog.Builder(this@InfrastructureReportActivity)
                    .setTitle("Confirm Submission")
                    .setMessage("Type: $facilityTag, Damage: $damageTag, Location: $latitude, $longitude")
                    .setPositiveButton("Submit") { _, _ ->
                        lifecycleScope.launch {
                            dao.insertReport(report)

                            val packed = BitPackingEncoder.encodeInfrastructureReport(report)
                            val file = File(filesDir, "${report.reportId}.bin")
                            file.writeBytes(packed)

                            val success = withContext(Dispatchers.IO) {
                                BluetoothTransmitter.sendBinFileToESP32A(file)
                            }

                            if (success) {
                                Toast.makeText(this@InfrastructureReportActivity, "📤 Transmitted to ESP32A", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@InfrastructureReportActivity, "⚠️ Bluetooth transmit failed", Toast.LENGTH_LONG).show()
                            }


                            Toast.makeText(this@InfrastructureReportActivity, "Submitted", Toast.LENGTH_SHORT).show()
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

            val imageView = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
                )
                setImageURI(uri)
                scaleType = ImageView.ScaleType.CENTER_CROP
            }

            val deleteBtn = ImageButton(this).apply {
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

            frame.addView(imageView)
            frame.addView(deleteBtn)
            container.addView(frame)
        }
    }
}
