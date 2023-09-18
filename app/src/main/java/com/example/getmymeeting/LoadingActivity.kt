package com.example.getmymeeting

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoadingActivity : AppCompatActivity() {

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        val progBar = findViewById<ProgressBar>(R.id.progressBar)
        // Show the progress bar
        progBar.visibility = View.VISIBLE

        // Start a coroutine to simulate loading data
        GlobalScope.launch {
            delay(3000) // Wait for 3 seconds
        }

        if (hasInternetConnection(this)) {
            // Device has internet connection
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }, 3000) // Delay for 3 seconds before redirecting to LoginActivity
        } else {
            // Device doesn't have internet connection
            Toast.makeText(
                this,
                "You need an internet connection to use the app. (425)",
                Toast.LENGTH_LONG
            ).show()
            findViewById<TextView>(R.id.lblNoInternet).visibility = View.VISIBLE
        }
    }

    private fun hasInternetConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        if (connectivityManager != null) {
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return networkCapabilities != null && (networkCapabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI
            ) || networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
        }
        return false
    }
}
