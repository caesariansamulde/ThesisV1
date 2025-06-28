package com.example.thesisv1

import com.example.thesisv1.DisasterProfileListActivity
import com.example.thesisv1.CreateDisasterProfileActivity
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.thesisv1.R
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView
    private lateinit var statusText: TextView
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private val REQUEST_PERMISSION = 1
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @SuppressLint("MissingInflatedId")
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.device_list)
        statusText = findViewById(R.id.status_text)

        checkPermissions()

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        if (!bluetoothAdapter!!.isEnabled) {
            bluetoothAdapter.enable()
        }

        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        val deviceNames = ArrayList<String>()
        val deviceList = ArrayList<BluetoothDevice>()

        pairedDevices?.forEach { device ->
            deviceNames.add(device.name + "\n" + device.address)
            deviceList.add(device)
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, deviceNames)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val device = deviceList[position]
            connectToDevice(device)
        }

        val createButton = findViewById<Button>(R.id.btnCreateProfile)
        createButton.setOnClickListener {
            val intent = Intent(this, CreateDisasterProfileActivity::class.java)
            startActivity(intent)
        }

        val btnViewProfiles = findViewById<Button>(R.id.btnViewProfiles)
        btnViewProfiles.setOnClickListener {
            val intent = Intent(this, DisasterProfileListActivity::class.java)
            startActivity(intent)
        }

        val btnTools = findViewById<Button>(R.id.btnTools)
        btnTools.setOnClickListener {
            startActivity(Intent(this, ToolsActivity::class.java))
        }


    }

    private fun checkPermissions() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), REQUEST_PERMISSION)
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun connectToDevice(device: BluetoothDevice) {
        statusText.text = "Connecting to ${device.name}..."
        Thread {
            try {
                socket = device.createRfcommSocketToServiceRecord(uuid)
                socket?.connect()
                runOnUiThread {
                    statusText.text = "Connected to ${device.name}"
                    BluetoothStorage.socket = socket
                }
            } catch (e: IOException) {
                runOnUiThread {
                    statusText.text = "Connection failed"
                }
                e.printStackTrace()
            }
        }.start()
    }
}

object BluetoothStorage {
    var socket: BluetoothSocket? = null
}
