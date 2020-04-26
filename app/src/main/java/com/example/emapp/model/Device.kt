package com.example.emapp.model

class Device {
    var deviceAddress: String? = null
    var name: String = "noName"
    var registered: Boolean = false

    constructor(deviceAddress: String?, name: String?, registered: Boolean) {
        this.deviceAddress = deviceAddress
        if (name != null) this.name = name
        this.registered = registered
    }

    override fun equals(other: Any?): Boolean {
        return if (other is Device)
            deviceAddress == other.deviceAddress
        else
            false;
    }

}