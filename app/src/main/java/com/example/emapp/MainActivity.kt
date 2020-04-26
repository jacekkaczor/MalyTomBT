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
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.emapp.adapters.DeviceItemAdapter
import com.example.emapp.model.Device
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity() {
    private var uniqueID: String? = null
    private val PREF_UNIQUE_ID = "PREF_UNIQUE_ID"
    private val REQUEST_CODE_ENABLE_BT: Int = 1
    private val MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 2
    private val TAG_FIREBASE = "Firebase"
    private lateinit var bAdapter: BluetoothAdapter
    private var hasBluetooth: Boolean = false
    private var discoveryDevicesList = ArrayList<Device>()
    private var devicesList = ArrayList<Device>()
    private lateinit var deviceAdapter: DeviceItemAdapter
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val pm: PackageManager = applicationContext.packageManager
        val handler = Handler()
        hasBluetooth = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
        createOrRetrieveId(applicationContext)

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
            updateShopClient()
            initShopClientListener()
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
                        updateShopClient()
                    }

                    //clear discovery list
                    discoveryDevicesList.clear()
                    bAdapter.startDiscovery()
                }
            }
        }, 0)

    }

    private fun updateShopClient() {
        val shopClient: MutableMap<String, Any> = HashMap()
        shopClient["devices"] = discoveryDevicesList
        shopClient["name"] = bAdapter.name

        db.collection("Shops").document(uniqueID!!)
            .set(shopClient)
            .addOnSuccessListener { Log.d(TAG_FIREBASE,"DocumentSnapshot added with ID: ") }
            .addOnFailureListener { e -> Log.w(TAG_FIREBASE, "Error adding document", e) }
    }

    private fun initShopClientListener() {
        val docRef = db.collection("Shops").document(uniqueID!!)
        docRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.w(TAG_FIREBASE, "Listen failed.", e)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG_FIREBASE, "Current data: ${snapshot.data}")
                devicesTv.text = "Devices"
                devicesList.clear()
                val devices = snapshot.data!!["devices"] as ArrayList<HashMap<String, Any>>
                devices.forEach {
                    devicesList.add(Device(it["deviceAddress"] as String, it["name"] as String, it["registered"] as Boolean))
                }
                deviceAdapter.notifyDataSetChanged()
            } else {
                Log.d(TAG_FIREBASE, "Current data: null")
            }
        }
    }

    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action

            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val deviceItem = Device(device.address, device.name, false)
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

    @Synchronized
    fun createOrRetrieveId(context: Context): String? {
        if (uniqueID == null) {
            val sharedPrefs = context.getSharedPreferences(
                PREF_UNIQUE_ID, Context.MODE_PRIVATE
            )
            uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null)
            if (uniqueID == null) {
                uniqueID = UUID.randomUUID().toString()
                val editor = sharedPrefs.edit()
                editor.putString(PREF_UNIQUE_ID, uniqueID)
                editor.commit()
            }
        }
        return uniqueID
    }
}
