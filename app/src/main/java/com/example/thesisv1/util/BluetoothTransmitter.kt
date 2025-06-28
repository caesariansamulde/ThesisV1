package com.example.thesisv1.util

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import java.io.File
import java.io.OutputStream
import java.util.*

object BluetoothTransmitter {
    private const val TAG = "BluetoothTransmitter"
    private const val TARGET_DEVICE_NAME = "ESP32A"
    private val SERIAL_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    @RequiresPermission(allOf = [Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN])
    fun sendBinFileToESP32A(file: File): Boolean {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val device = adapter?.bondedDevices?.firstOrNull { it.name == TARGET_DEVICE_NAME }
                ?: run {
                    Log.e(TAG, "❌ Bluetooth device '$TARGET_DEVICE_NAME' not found.")
                    return false
                }

            adapter.cancelDiscovery()

            val socket = try {
                Log.d(TAG, "🔗 Attempting secure connection...")
                device.createRfcommSocketToServiceRecord(SERIAL_UUID).apply { connect() }
            } catch (secureException: Exception) {
                Log.e(TAG, "⚠️ Secure connection failed: ${secureException.message}")
                Log.d(TAG, "🔁 Trying insecure fallback...")

                try {
                    // Reflection fallback: insecure connection on channel 1
                    val method = device.javaClass.getMethod("createInsecureRfcommSocket", Int::class.javaPrimitiveType)
                    val fallbackSocket = method.invoke(device, 1) as BluetoothSocket
                    fallbackSocket.connect()
                    fallbackSocket
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "❌ Fallback connection failed: ${fallbackEx.message}")
                    return false
                }
            }

            val output: OutputStream = socket.outputStream
            val bytes = file.readBytes()

            output.write("<FILE>\n".toByteArray())
            output.flush()
            Thread.sleep(50)

            bytes.toList().chunked(100).forEachIndexed { index, chunk ->
                val hexLine = chunk.joinToString("") { "%02X".format(it) }
                val line = "<DATA>$hexLine\n"
                output.write(line.toByteArray())
                output.flush()
                Log.d(TAG, "📤 Sent chunk ${index + 1}")
                Thread.sleep(50)
            }

            output.write("<EOF>\n".toByteArray())
            output.flush()

            socket.close()
            Log.d(TAG, "✅ Transmission complete.")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to send file: ${e.message}", e)
            return false
        }
    }
}
