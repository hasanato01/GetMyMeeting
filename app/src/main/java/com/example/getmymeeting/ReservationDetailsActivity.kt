package com.example.getmymeeting

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ReservationDetailsActivity : AppCompatActivity() {

    //region declaring variables
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var source: String
    private lateinit var reservationId: String
    private lateinit var lblReservationId: TextView
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private var resId: Int? = null
    private var currentUser: String? = null
    private var isInternetConnected = true
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservation_details)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initialize variables
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        lblReservationId = findViewById(R.id.lblreservationId)
        val lblUpperSection = findViewById<TextView>(R.id.upperSection)
        val lblReservationTitle = findViewById<TextView>(R.id.lblMeetingTitle)
        val lblRoomName = findViewById<TextView>(R.id.lblRoomName)
        val lblDate = findViewById<TextView>(R.id.lblDate)
        val lblTime = findViewById<TextView>(R.id.lblTime)
        val lblName = findViewById<TextView>(R.id.lblName)
        setIntentValues()
        //endregion

        when (source) {
            "makeReservation" -> {
                reservationId = intent.getStringExtra("SreservationId").toString()
                val title = intent.getStringExtra("Stitle").toString()
                val name = intent.getStringExtra("Sname").toString()
                val room = intent.getStringExtra("Sroom")
                val date = intent.getStringExtra("Sdate")
                val time = intent.getStringExtra("Stime")

                // Set text values using apply function for lblUpperSection
                with(lblReservationId) { text = reservationId }
                with(lblUpperSection) { text = "$date , $time" }
                with(lblReservationTitle) { text = title }
                with(lblRoomName) { text = room }
                with(lblDate) { text = date }
                with(lblTime) { text = time }
                with(lblName) { text = name }
            }
            "home", "reservationsAdmin" -> {
                resId = intent.getIntExtra("reservationId", 1)
                println(resId)
                db.collection("meetings").document(resId.toString())
                    .get().addOnSuccessListener { meeting ->
                        if (meeting.exists()) {
                            val data = meeting.data
                            // Set text values using apply function for lblUpperSection
                            lblReservationId.text = meeting.id
                            lblUpperSection.apply {
                                text =
                                    "${data?.get("date")}, ${data?.get("start_time")}-${data?.get("end_time")}"
                            }
                            lblReservationTitle.text = data?.get("title").toString()
                            lblRoomName.text = data?.get("room").toString()
                            lblDate.text = data?.get("date").toString()
                            lblTime.text = "${data?.get("start_time")} - ${data?.get("end_time")}"
                            lblName.text = data?.get("name").toString()
                        }
                    }
            }
            "editReservation" -> {
                reservationId = intent.getStringExtra("reservationId").toString()
                val title = intent.getStringExtra("Etitle").toString()
                val name = intent.getStringExtra("Ename").toString()
                val room = intent.getStringExtra("Eroom")
                val date = intent.getStringExtra("Edate")
                val time = intent.getStringExtra("Etime")

                // Set text values using apply function for lblUpperSection
                with(lblReservationId) { text = reservationId }
                with(lblUpperSection) { text = "$date , $time" }
                with(lblReservationTitle) { text = title }
                with(lblRoomName) { text = room }
                with(lblDate) { text = date }
                with(lblTime) { text = time }
                with(lblName) { text = name }
            }
            else -> startActivity(Intent(this, HomeActivity::class.java))
        }
        findViewById<Button>(R.id.btnDone).setOnClickListener {
            val homeIntent = Intent(this, HomeActivity::class.java)
            homeIntent.putExtra("source", "reservationDetails")
            homeIntent.putExtra("currentUser", currentUser)
            homeIntent.putExtra("fullname", fullName)
            homeIntent.putExtra("company", company)
            homeIntent.putExtra("userType", userType)
            startActivity(homeIntent)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle the back button click here
        val intentBack = Intent(this, HomeActivity::class.java)
        intentBack.putExtra("source", "reservationDetails")
        intentBack.putExtra("currentUser", currentUser)
        intentBack.putExtra("fullname", fullName)
        intentBack.putExtra("userType", userType)
        intentBack.putExtra("company", company)
        startActivity(intentBack)

        // Call super.onBackPressed() to allow the default back button behavior
        super.onBackPressed()
    }

    private fun setIntentValues() {
        source = intent.getStringExtra("source").toString()
        currentUser = intent.getStringExtra("currentUser")
        fullName = intent.getStringExtra("name").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
    }

    fun onEditClicked(view: View) {
        val editIntent = Intent(this, EditReservationActivity::class.java)
        editIntent.putExtra("reservationID", lblReservationId.text)
        editIntent.putExtra("source", "reservationDetails")
        editIntent.putExtra("currentUser", currentUser)
        editIntent.putExtra("fullname", fullName)
        editIntent.putExtra("company", company)
        editIntent.putExtra("userType", userType)
        startActivity(editIntent)
    }

    fun onDeleteClicked(view: View) {
        val alertDialog = AlertDialog.Builder(this)
        alertDialog.apply {
            setTitle("Confirmation")
            setMessage("Are you sure you want to delete this reservation?")
            setPositiveButton("Yes") { _: DialogInterface, _: Int ->
                deleteItem()
            }
            setNegativeButton("No") { _: DialogInterface, _: Int ->
                // Remove this block if you don't need a negative button
            }
            setCancelable(false)
            show()
        }
    }

    private fun deleteItem() {
        val docRef = db.collection("meetings").document(resId.toString())
        docRef.update("status", "deleted")
            .addOnSuccessListener {
                println("Reservation details: Delete success")
                Toast.makeText(this, "Reservation deleted successfully!", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                println("Reservation details: Delete failed")
                Toast.makeText(this, "Error occurred while deleting!", Toast.LENGTH_SHORT).show()
            }
        val homeIntent = Intent(this, HomeActivity::class.java)
        homeIntent.putExtra("source", "reservationDetails")
        homeIntent.putExtra("currentUser", currentUser)
        homeIntent.putExtra("fullname", fullName)
        homeIntent.putExtra("company", company)
        homeIntent.putExtra("userType", userType)
        startActivity(homeIntent)
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