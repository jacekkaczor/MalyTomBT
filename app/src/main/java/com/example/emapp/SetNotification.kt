package com.example.emapp

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_set_notification.*


class SetNotification : AppCompatActivity() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val TAG_FIREBASE = "Firebase"

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_notification)
        progressbar.visibility = View.VISIBLE
        timeTp.setIs24HourView(true)
        db.collection("Notifications")
            .document("Daily")
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val result = task.result?.data as HashMap<String, Any>
                    Log.d(TAG_FIREBASE, result.toString())
                    titleEt.text = Editable.Factory.getInstance().newEditable(result["title"].toString())
                    messageEt.text = Editable.Factory.getInstance().newEditable(result["message"].toString())
                    timeTp.hour = result["time"].toString().split(":")[0].toInt()
                    timeTp.minute = result["time"].toString().split(":")[1].toInt()
                } else {
                    Log.w(TAG_FIREBASE,"Error getting documents.", task.exception)
                    Toast.makeText(this, "Error getting notification, please try again!", Toast.LENGTH_LONG).show()
                    this.onBackPressed()
                }
                progressbar.visibility = View.INVISIBLE
            }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun saveNotificationBtnOnClick(v: View) {
        if (titleEt.text.toString() == "") {
            Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_LONG).show()
            return
        }
        if (messageEt.text.toString() == "") {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_LONG).show()
            return
        }

        val inputManager:InputMethodManager =getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(currentFocus?.windowToken, InputMethodManager.SHOW_FORCED)
        progressbar.visibility = View.VISIBLE
        val notification: MutableMap<String, Any> = HashMap()
        notification["title"] = titleEt.text.toString()
        notification["message"] = messageEt.text.toString()
        notification["time"] = timeTp.hour.toString() + ":" + timeTp.minute.toString()

        db.collection("Notifications")
            .document("Daily")
            .set(notification)
            .addOnSuccessListener { documentReference ->
                Toast.makeText(this, "Daily notification has been set", Toast.LENGTH_LONG).show()
                progressbar.visibility = View.INVISIBLE
                this.onBackPressed()
            }
            .addOnFailureListener { e ->
                Log.w(TAG_FIREBASE, "Error adding document", e)
                progressbar.visibility = View.INVISIBLE
                Toast.makeText(this, "Error, please try again!", Toast.LENGTH_LONG).show()
            }
    }
}
