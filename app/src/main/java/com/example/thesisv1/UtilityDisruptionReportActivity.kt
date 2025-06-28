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
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.data.UtilityDisruptionReport
import com.example.thesisv1.util.BitPackingEncoder
import com.example.thesisv1.util.BluetoothTransmitter
import com.example.thesisv1.util.copyUriToInternalStorage
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class UtilityDisruptionReportActivity : AppCompatActivity() {

    private var latitude = 0.0
    private var longitude = 0.0
    private val imageUris = mutableListOf<Uri>()

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            imageUris.clear()
            imageUris.addAll(uris.take(3))
            displayImagePreviews()
        }
    }

    private fun displayImagePreviews() {
        val container = findViewById<LinearLayout>(R.id.photoPreviewContainer)
        container.removeAllViews()

        imageUris.forEachIndexed { index, uri ->
            val frame = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                    setMargins(8, 8, 8, 8)
                }
            }

            val image = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
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
                    displayImagePreviews()
                }
            }

            frame.addView(image)
            frame.addView(deleteBtn)
            container.addView(frame)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_utility_disruption_report)

        val profileId = intent.getStringExtra("profileId") ?: return
        val dao = DisasterDatabase.getDatabase(this).utilityDisruptionReportDao()

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

        findViewById<Button>(R.id.btnAddPhoto).setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE

            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
            } else {
                imagePickerLauncher.launch("image/*")
            }
        }

        findViewById<Button>(R.id.btnSubmit).setOnClickListener {
            val utilityType = when (findViewById<RadioGroup>(R.id.utilityTypeGroup).checkedRadioButtonId) {
                R.id.radioElectric -> 1
                R.id.radioWater -> 2
                R.id.radioTelecom -> 3
                R.id.radioFuel -> 4
                R.id.radioSanitation -> 5
                R.id.radioOtherUtility -> 6
                else -> 0
            }

            val status = when (findViewById<RadioGroup>(R.id.statusGroup).checkedRadioButtonId) {
                R.id.radioOperational -> 1
                R.id.radioPartial -> 2
                R.id.radioFull -> 3
                R.id.radioUnknown -> 4
                else -> 0
            }

            val details = mutableListOf<String>()
            if (findViewById<CheckBox>(R.id.checkDamage).isChecked) details.add("1")
            if (findViewById<CheckBox>(R.id.checkInfraFailure).isChecked) details.add("2")
            if (findViewById<CheckBox>(R.id.checkLogistics).isChecked) details.add("3")
            if (findViewById<CheckBox>(R.id.checkSafety).isChecked) details.add("4")
            if (findViewById<CheckBox>(R.id.checkOtherReason).isChecked) {
                val other = findViewById<EditText>(R.id.otherReasonInput).text.toString()
                if (other.isNotBlank()) details.add("5:$other")
            }

            val remarks = remarksInput.text.toString().takeIf { it.isNotBlank() }

            val utilityTypeCode = when (utilityType) {
                1 -> "ELE"
                2 -> "WAT"
                3 -> "TEL"
                4 -> "FUE"
                5 -> "SAN"
                6 -> "OTH"
                else -> "UNK"
            }

            val randomSuffix = (1..4)
                .map { ('A'..'Z') + ('a'..'z') + ('0'..'9') }
                .flatten()
                .shuffled()
                .take(4)
                .joinToString("")

            val reportId = "UTI_${utilityTypeCode}_$randomSuffix"

            lifecycleScope.launch {
                val filePaths = imageUris.mapIndexedNotNull { index, uri ->
                    val filename = "utility_${System.currentTimeMillis()}_$index.jpg"
                    copyUriToInternalStorage(this@UtilityDisruptionReportActivity, uri, filename)
                }

                val report = UtilityDisruptionReport(
                    reportId = reportId,
                    disasterProfileId = profileId,
                    utilityType = utilityType,
                    status = status,
                    disruptionDetails = details.joinToString(", "),
                    remarks = remarks,
                    latitude = latitude,
                    longitude = longitude,
                    photo1 = filePaths.getOrNull(0),
                    photo2 = filePaths.getOrNull(1),
                    photo3 = filePaths.getOrNull(2)
                )


                val summary = """
                    Report ID: $reportId
                    Type: $utilityTypeCode
                    Status: $status
                    Details: ${report.disruptionDetails}
                    Location: $latitude, $longitude
                    Remarks: ${remarks ?: "None"}
                    Photos: ${filePaths.size}
                """.trimIndent()

                AlertDialog.Builder(this@UtilityDisruptionReportActivity)
                    .setTitle("Confirm Submission")
                    .setMessage(summary)
                    .setPositiveButton("Submit") { _, _ ->
                        lifecycleScope.launch {
                            dao.insertReport(report)

                            val packed = BitPackingEncoder.encodeUtilityDisruptionReport(report)
                            val file = File(filesDir, "${report.reportId}.bin")
                            file.writeBytes(packed)

                            val success = withContext(Dispatchers.IO) {
                                BluetoothTransmitter.sendBinFileToESP32A(file)
                            }

                            if (success) {
                                Toast.makeText(this@UtilityDisruptionReportActivity, "📤 Transmitted to ESP32A", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@UtilityDisruptionReportActivity, "⚠️ Bluetooth transmit failed", Toast.LENGTH_LONG).show()
                            }


                            Toast.makeText(this@UtilityDisruptionReportActivity, "Report Submitted", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }
}
