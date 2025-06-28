package com.example.thesisv1

import DisasterProfileAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.thesisv1.data.DisasterDatabase
import com.example.thesisv1.data.DisasterProfile
import kotlinx.coroutines.launch

class DisasterProfileListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DisasterProfileAdapter
    private var profiles: List<DisasterProfile> = listOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disaster_profile_list)

        recyclerView = findViewById(R.id.disasterProfileRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dao = DisasterDatabase.getDatabase(this).disasterProfileDao()

        lifecycleScope.launch {
            profiles = dao.getAllProfiles()
            adapter = DisasterProfileAdapter(
                profiles,
                onItemClick = { profile ->
                    // ✅ Tap to go to DisasterReportOptionsActivity
                    val intent = Intent(this@DisasterProfileListActivity, DisasterReportOptionsActivity::class.java)
                    intent.putExtra("profileId", profile.profileId)
                    startActivity(intent)
                },
                onItemLongClick = { profile ->
                    // ✅ Long press to show dialog
                    showProfileOptionsDialog(profile)
                }
            )
            recyclerView.adapter = adapter
        }
    }

    private fun showProfileOptionsDialog(profile: DisasterProfile) {
        val options = arrayOf("🗑 Delete Profile", "📄 View Reports")
        AlertDialog.Builder(this)
            .setTitle("Select an option")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDelete(profile)
                    1 -> openAllReports(profile)
                }
            }
            .show()
    }

    private fun confirmDelete(profile: DisasterProfile) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage("Are you sure you want to delete this profile?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    DisasterDatabase.getDatabase(this@DisasterProfileListActivity)
                        .disasterProfileDao()
                        .deleteProfile(profile)
                    Toast.makeText(this@DisasterProfileListActivity, "Profile deleted", Toast.LENGTH_SHORT).show()
                    recreate() // Refresh the list
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openAllReports(profile: DisasterProfile) {
        val intent = Intent(this, AllReportsActivity::class.java)
        intent.putExtra("profileId", profile.profileId)
        startActivity(intent)
    }
}
