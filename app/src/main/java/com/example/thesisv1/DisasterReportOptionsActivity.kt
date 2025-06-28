package com.example.thesisv1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.thesisv1.data.DisasterDatabase
import kotlinx.coroutines.launch


class DisasterReportOptionsActivity : AppCompatActivity() {

    val regionNameMap = mapOf(
        1 to "Region I – Ilocos Region",
        2 to "Region II – Cagayan Valley",
        3 to "Region III – Central Luzon",
        4 to "Region IV‑A – CALABARZON",
        5 to "Region IV-B - MIMAROPA",
        6 to "Region V – Bicol Region",
        7 to "Region VI – Western Visayas",
        8 to "Region VII – Central Visayas",
        9 to "Region VIII – Eastern Visayas",
        10 to "Region IX – Zamboanga Peninsula",
        11 to "Region X – Northern Mindanao",
        12 to "Region XI – Davao Region",
        13 to "Region XII – SOCCSKSARGEN",
        14 to "Region XIII – Caraga",
        15 to "NCR – National Capital Region",
        16 to "CAR – Cordillera Administrative Region",
        17 to "BARMM – Bangsamoro Autonomous Region in Muslim Mindanao"
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disaster_report_options)

        val profileId = intent.getStringExtra("profileId")
        val profileTextView = findViewById<TextView>(R.id.profileDetailsText)

        if (!profileId.isNullOrBlank()) {
            val dao = DisasterDatabase.getDatabase(this).disasterProfileDao()

            lifecycleScope.launch {
                val profile = dao.getProfileById(profileId)
                profile?.let {
                    val regionName = regionNameMap[it.region] ?: "Unknown Region"

                    val details = """
                        ID: ${it.profileId}
                        Type: ${it.type}
                        Name: ${it.name}
                        Date: ${it.dateTime}
                        Region: $regionName
                        Province: ${it.province}
                        Municipality: ${it.municipality}
                        Barangay: ${it.barangay}
                        """.trimIndent()
                    profileTextView.text = details

                } ?: run {
                    profileTextView.text = "Profile not found"
                }
            }
        } else {
            profileTextView.text = "Invalid profile ID"
        }

        findViewById<Button>(R.id.btnCasualty).setOnClickListener {
            val intent = Intent(this, CasualtyReportActivity::class.java)
            intent.putExtra("profileId", profileId)
            startActivity(intent)
        }


        findViewById<Button>(R.id.btnInfrastructure).setOnClickListener {
            val intent = Intent(this, InfrastructureReportActivity::class.java)
            intent.putExtra("profileId", profileId)
            startActivity(intent)
        }


        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            val intent = Intent(this, AccessibilityReportActivity::class.java)
            intent.putExtra("profileId", profileId)
            startActivity(intent)
        }


        findViewById<Button>(R.id.btnUtilityDisruption).setOnClickListener {
            val intent = Intent(this, UtilityDisruptionReportActivity::class.java)
            intent.putExtra("profileId", profileId) // Pass disaster profile ID
            startActivity(intent)
        }


    }
}
