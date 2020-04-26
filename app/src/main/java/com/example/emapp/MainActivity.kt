package com.example.emapp

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.emapp.adapters.DeviceItemAdapter
import com.example.emapp.model.Device
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_ENABLE_BT: Int = 1
    private val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1
    lateinit var bAdapter: BluetoothAdapter
    var hasBluetooth: Boolean = false
    var discoveryDevicesList = ArrayList<Device>()
    var devicesList = ArrayList<Device>()
    lateinit var deviceAdapter: DeviceItemAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm: PackageManager = applicationContext.packageManager
        val handler = Handler()
        hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

        if (hasBluetooth) {
            bAdapter = BluetoothAdapter.getDefaultAdapter()
            if (bAdapter == null) {
                bluetoothStatusTv.text = "Bluetooth is not available"
            } else {
                bluetoothStatusTv.text = "Bluetooth is available"
            }
            if (bAdapter.isEnabled) {
                bluetoothIv.setImageResource(R.drawable.ic_bluetooth_on)
                turnOnOffBtn.text = "Turn Off"

            } else {
                bluetoothIv.setImageResource(R.drawable.ic_bluetooth_off)
                turnOnOffBtn.text = "Turn On"
            }
            val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
            registerReceiver(mReceiver, intentFilter)

            deviceAdapter = DeviceItemAdapter(this, devicesList)
            devicesLv.adapter = deviceAdapter
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION)
        } else {
            bluetoothStatusTv.text = "Bluetooth is not supported"
        }

        handler.postDelayed(object : Runnable {
            override fun run() {
                handler.postDelayed(this, 15 * 1000)
                if (bAdapter.isEnabled) {
                    if (bAdapter.isDiscovering)
                        bAdapter.cancelDiscovery()

                    //send list to server if changed from previous and display
                    if (!compareList(devicesList, discoveryDevicesList)) {
                        devicesTv.text = "Devices"
                        devicesList.clear()
                        devicesList.addAll(discoveryDevicesList)
                        deviceAdapter.notifyDataSetChanged()
                    }

                    //clear discovery list
                    discoveryDevicesList.clear()
                    bAdapter.startDiscovery()
                }
            }
        }, 0)

    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceItem = Device(device, device.name, false)
                if (!discoveryDevicesList.contains(deviceItem))
                    discoveryDevicesList.add(deviceItem)
            }
        }
    }

    private fun compareList(l1: ArrayList<Device>, l2: ArrayList<Device>): Boolean {
        if (l1.size != l2.size) return false
        return l1.containsAll(l2)
    }

    fun turnOnOffBtnOnClick(v: View) {
        if (!hasBluetooth) return

        var intent: Intent

        if (bAdapter.isEnabled) {
            bAdapter.disable()
            bluetoothIv.setImageResource(R.drawable.ic_bluetooth_off)
            turnOnOffBtn.text = "Turn On"
            Toast.makeText(this, "Bluetooth is off", Toast.LENGTH_LONG).show()
        } else {
            intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(intent, REQUEST_CODE_ENABLE_BT)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when(requestCode) {
            REQUEST_CODE_ENABLE_BT ->
                if (requestCode == Activity.RESULT_OK || bAdapter.isEnabled) {
                    bluetoothIv.setImageResource(R.drawable.ic_bluetooth_on)
                    turnOnOffBtn.text = "Turn Off"
                    bAdapter.startDiscovery()
                    Toast.makeText(this, "Bluetooth is on", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "Could not on bluetooth", Toast.LENGTH_LONG).show()
                }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}
