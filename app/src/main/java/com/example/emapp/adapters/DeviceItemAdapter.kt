package com.example.emapp.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.emapp.R
import com.example.emapp.model.Device

class DeviceItemAdapter : BaseAdapter {
    private var devicesList = ArrayList<Device>()
    private var context: Context? = null

    constructor(context: Context, devicesList: ArrayList<Device>) : super() {
        this.devicesList = devicesList
        this.context = context
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View?
        val vh: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.device_item, parent, false)
            vh = ViewHolder(view)
            view!!.tag = vh
        } else {
            view = convertView
            vh = view.tag as ViewHolder
        }

        vh.deviceName.text = devicesList[position].name +" ("+devicesList[position].device+")"
        if (devicesList[position].registered) {
            vh.subtext.text = "Registered"
        } else {
            vh.subtext.text = "Not Registered"
        }

        return view
    }

    override fun getItem(position: Int): Any {
        return devicesList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return devicesList.size
    }

    private class ViewHolder(view: View?) {
        val deviceName: TextView = view?.findViewById(R.id.firstLine) as TextView
        val subtext: TextView = view?.findViewById(R.id.secondLine) as TextView
    }
}