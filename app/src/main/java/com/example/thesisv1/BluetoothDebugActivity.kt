package com.example.thesisv1

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.util.*

class BluetoothDebugActivity : AppCompatActivity() {
    private lateinit var spinner: Spinner
    private lateinit var connectButton: Button
    private lateinit var sendButton: Button
    private lateinit var inputField: EditText
    private lateinit var consoleOutput: TextView

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var readerJob: Job? = null

    private val SERIAL_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val REQUEST_BT_PERMISSIONS = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_debug)

        spinner = findViewById(R.id.deviceSpinner)
        connectButton = findViewById(R.id.connectButton)
        sendButton = findViewById(R.id.sendButton)
        inputField = findViewById(R.id.inputField)
        consoleOutput = findViewById(R.id.consoleOutput)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                ),
                REQUEST_BT_PERMISSIONS
            )
        } else {
            loadDevices()
        }

        connectButton.setOnClickListener {
            if (hasBluetoothPermission()) {
                connectToSelectedDevice()
            } else {
                Toast.makeText(this, "❌ Bluetooth permission not granted", Toast.LENGTH_SHORT).show()
            }
        }

        sendButton.setOnClickListener {
            sendTextToESP32()
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun loadDevices() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val devices = adapter?.bondedDevices?.toList() ?: emptyList()
        val names = devices.map { it.name }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
    }

    private fun connectToSelectedDevice() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        val name = spinner.selectedItem as? String ?: return
        val device = adapter?.bondedDevices?.firstOrNull { it.name == name } ?: return

        Toast.makeText(this, "🔗 Connecting to $name...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tmpSocket = device.createRfcommSocketToServiceRecord(SERIAL_UUID)
                adapter?.cancelDiscovery()
                tmpSocket.connect()

                socket = tmpSocket
                outputStream = tmpSocket.outputStream
                startReadingFromESP32(tmpSocket.inputStream)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BluetoothDebugActivity, "✅ Connected to $name", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BluetoothDebugActivity, "❌ Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun startReadingFromESP32(input: InputStream) {
        readerJob?.cancel()
        readerJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            while (isActive) {
                try {
                    val bytes = input.read(buffer)
                    if (bytes > 0) {
                        val msg = String(buffer, 0, bytes)
                        withContext(Dispatchers.Main) {
                            consoleOutput.append(msg)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BluetoothDebugActivity, "⚠️ Disconnected: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
        }
    }

    private fun sendTextToESP32() {
        val text = inputField.text.toString()
        if (text.isNotBlank() && outputStream != null) {
            try {
                outputStream!!.write((text + "\n").toByteArray())
                outputStream!!.flush()
                consoleOutput.append(">> $text\n")
                inputField.text.clear()
            } catch (e: Exception) {
                Toast.makeText(this, "⚠️ Send failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        readerJob?.cancel()
        socket?.close()
        outputStream = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BT_PERMISSIONS && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            loadDevices()
        }
    }
}
