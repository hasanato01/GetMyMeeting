package com.example.getmymeeting

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {

    //region user variables
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private var currentUser: String? = null
    private var isInternetConnected = true
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region user variables
        fullName = ""
        company = ""
        userType = ""
        setIntentValues()
        //endregion
    }

    private fun setIntentValues() {
        currentUser = intent.getStringExtra("currentUser")
        fullName = intent.getStringExtra("fullname").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
    }

    //region Check for connection status
    override fun onStart() {
        super.onStart()
        // Start listening for network connectivity changes
        networkConnectivityListener.startListening()
    }

    override fun onStop() {
        super.onStop()
        // Stop listening for network connectivity changes
        networkConnectivityListener.stopListening()
    }

    private fun handleControlsState() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        val enableControls = isInternetConnected
        enableControls(rootView, enableControls)
        if (enableControls) {
            // do nothing
        } else {
            showConnectionLostMessage()
        }
    }

    private fun enableControls(viewGroup: ViewGroup, enable: Boolean) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is ViewGroup) {
                enableControls(child, enable)
            } else {
                runOnUiThread {
                    child.isEnabled = enable
                }
            }
        }
    }

    private fun showConnectionLostMessage() {
        val rootView = findViewById<ViewGroup>(android.R.id.content)
        Snackbar.make(rootView, "Internet connection lost", Snackbar.LENGTH_LONG).show()
    }
    //endregion
}