package com.example.thesisv1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ToolsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tools)

        findViewById<Button>(R.id.btnBitDecoder).setOnClickListener {
            startActivity(Intent(this, BinReportDecoderActivity::class.java))
        }

        findViewById<Button>(R.id.btnBluetoothDebug).setOnClickListener {
            startActivity(Intent(this, BluetoothDebugActivity::class.java))
        }


        // Add more tool buttons here in future
    }
}
