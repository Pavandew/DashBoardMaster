package com.example.masterdashboard.print_bill

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Manages Bluetooth printer discovery and data transmission.
 */
class BluetoothPrinterManager(private val context: Context) {

    companion object {
        private const val TAG = "BTPrinterManager"
        // Standard SPP UUID for Bluetooth serial communication
        private val PRINTER_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    interface PrintListener {
        fun onPrintStarted()
        fun onPrintSuccess()
        fun onPrintFailed(error: String)
    }

    /**
     * Checks if Bluetooth is enabled and supported.
     */
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    /**
     * Fetches the list of already paired Bluetooth devices.
     */
    @SuppressLint("MissingPermission")
    fun getPairedPrinters(): List<BluetoothDevice> {
        val pairedDevices = bluetoothAdapter?.bondedDevices
        val printerList = mutableListOf<BluetoothDevice>()
        
        pairedDevices?.forEach { device ->
            // Many thermal printers have names like "MTP-2", "InnerPrinter", or include "Printer"
            Log.d(TAG, "Found Paired Device: ${device.name} (${device.address})")
            printerList.add(device)
        }
        return printerList
    }

    /**
     * Connects to a printer and sends the byte array.
     */
    @SuppressLint("MissingPermission")
    suspend fun printData(device: BluetoothDevice, data: ByteArray, listener: PrintListener) {
        Log.i(TAG, "Starting print task to device: ${device.name}")
        
        withContext(Dispatchers.IO) {
            var socket: BluetoothSocket? = null
            try {
                withContext(Dispatchers.Main) { listener.onPrintStarted() }

                // 1. Create a socket to the device
                socket = device.createRfcommSocketToServiceRecord(PRINTER_UUID)
                
                // 2. Connect (this is blocking, hence IO thread)
                Log.d(TAG, "Attempting to connect to ${device.address}...")
                socket.connect()
                Log.d(TAG, "Connected successfully!")

                // 3. Send data
                val outputStream = socket.outputStream
                Log.d(TAG, "Sending ${data.size} bytes...")
                outputStream.write(data)
                outputStream.flush()
                
                Log.i(TAG, "Data sent successfully to printer.")
                withContext(Dispatchers.Main) { listener.onPrintSuccess() }

            } catch (e: IOException) {
                Log.e(TAG, "Communication error with printer: ${e.message}", e)
                withContext(Dispatchers.Main) { 
                    listener.onPrintFailed("Could not connect to printer. Ensure it is ON.") 
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during printing", e)
                withContext(Dispatchers.Main) { 
                    listener.onPrintFailed("Printing Error: ${e.message}") 
                }
            } finally {
                try {
                    socket?.close()
                    Log.d(TAG, "Socket closed.")
                } catch (e: Exception) {
                    Log.w(TAG, "Error closing socket: ${e.message}")
                }
            }
        }
    }
}
