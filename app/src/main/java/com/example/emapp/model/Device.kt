package com.example.emapp.model

import android.bluetooth.BluetoothDevice

class Device {
    var device: BluetoothDevice? = null
    var name: String = "noName"
    var registered: Boolean = false

    constructor(device: BluetoothDevice?, name: String?, registered: Boolean) {
        this.device = device
        if (name != null) this.name = name
        this.registered = registered
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Device)
            device?.address == other.device?.address
        else
            false;
    }

}