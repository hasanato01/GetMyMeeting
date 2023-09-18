package com.example.getmymeeting

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.text.SpannableString
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalTime
import java.util.*

class TimePickerActivity : AppCompatActivity() {

    //region declare variables
    private lateinit var tboxStartTime: EditText
    private lateinit var tboxEndTime: EditText
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var source: String
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var meetingsColRef: CollectionReference
    private lateinit var currentTime: Calendar
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private var currentUser: String? = null
    private var title: String? = null
    private var reservationName: String? = null
    private var agreement: String? = null
    private var date: String? = null
    private var desc: String? = null
    private var room: String? = null
    private var rowsCount: Int = 0
    private var currentHour: Int = 12
    private var currentMinute: Int = 0
    private var isInternetConnected = true
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_time_picker)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region Initializing
        tboxStartTime = findViewById(R.id.tboxStartTime)
        tboxEndTime = findViewById(R.id.tboxEndTime)
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        meetingsColRef = db.collection("meetings")
        currentTime = Calendar.getInstance()
        currentHour = currentTime.get(Calendar.HOUR_OF_DAY)
        currentMinute = currentTime.get(Calendar.MINUTE)
        val unavailableTimes: TableLayout = findViewById(R.id.unavailableTimes)
        setIntentValues()
        //endregion

        //region Getting rows count and creating buttons
        meetingsColRef.get().addOnSuccessListener { querySnapshot ->
            rowsCount = querySnapshot.size()
            println("Rows count: $rowsCount")
        }.addOnFailureListener { e ->
            println("Error getting document count: $e")
        }
        for (i in 0..rowsCount) {
            createTimesButtons(this, unavailableTimes)
        }
        //endregion
    }

    private fun setIntentValues() {
        currentUser = intent.getStringExtra("currentUser")
        source = intent.getStringExtra("source").toString()
        fullName = intent.getStringExtra("fullname").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
        title = intent.getStringExtra("title")
        reservationName = intent.getStringExtra("reservationName")
        agreement = intent.getStringExtra("agreement")
        date = intent.getStringExtra("date")
        desc = intent.getStringExtra("desc")
        room = intent.getStringExtra("room")
    }

    private fun createTimesButtons(context: Context, parent: TableLayout) {
        // Retrieve the first document to check if any meetings exist
        meetingsColRef.limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Query meetings for the specified date and room
                    meetingsColRef.whereEqualTo("date", date).whereEqualTo("room", room).get()
                        .addOnSuccessListener { querySnapshot2 ->
                            val count = querySnapshot2.size()
                            var row: TableRow? = null
                            for (i in 0 until count) {
                                // Retrieve start and end time for each meeting
                                val startTime =
                                    querySnapshot2.documents[i].get("start_time") as String?
                                val endTime = querySnapshot2.documents[i].get("end_time") as String?

                                // Create a button for the time slot
                                val button = Button(context)
                                button.text = "$startTime - $endTime"
                                button.isEnabled = false

                                if (i % 3 == 0) {
                                    // Create a new row for every third button
                                    row = TableRow(context)
                                    val trLayoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
                                    trLayoutParams.setMargins(0, 5.dpToPx(), 0, 5.dpToPx())
                                    row.layoutParams = trLayoutParams
                                    row.gravity = Gravity.CENTER
                                    parent.addView(row)
                                }
                                row?.addView(button)
                            }
                        }
                        .addOnFailureListener { exception ->
                            println("Error: $exception")
                        }
                }
            }
            .addOnFailureListener { exception ->
                println("Error: $exception")
            }
    }

    private fun Int.dpToPx(): Int {
        // Convert dp to px
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            Resources.getSystem().displayMetrics
        ).toInt()
    }

    fun showStartTimePickerDialogue(view: View) {
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
            tboxStartTime.setText(selectedTime)
        }, currentHour, currentMinute, true)
        timePickerDialog.show()
    }

    fun showEndTimePickerDialogue(view: View) {
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
            tboxEndTime.setText(selectedTime)
        }, currentHour, currentMinute, true)
        timePickerDialog.show()
    }

    private fun compareTimes(time1: String, time2: String): Int {
        val localTime1 = LocalTime.parse(time1)
        val localTime2 = LocalTime.parse(time2)
        return localTime1.compareTo(localTime2)
    }

    fun onSaveTimeButtonClicked(view: View) {
        val startTime = SpannableString(tboxStartTime.text)
        val endTime = SpannableString(tboxEndTime.text)
        if (tboxStartTime.text.isEmpty()) {
            Toast.makeText(this, "Please choose start time", Toast.LENGTH_SHORT).show()
        } else if (tboxEndTime.text.isEmpty()) {
            Toast.makeText(this, "Please choose end time", Toast.LENGTH_SHORT).show()
        } else {
            if (compareTimes(startTime.toString(), endTime.toString()) >= 0) {
                Toast.makeText(
                    this,
                    "Start time can't be after or equal to end time.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //region Save time intent send values
                // T in the start of variable names refers to Time
                val saveTimeIntent: Intent = if (source != "editReservation") {
                    Intent(this, MakeReservationActivity::class.java)
                } else {
                    Intent(this, EditReservationActivity::class.java)
                }
                saveTimeIntent.putExtra("source", "timePicker")
                saveTimeIntent.putExtra(
                    "TreservationID",
                    intent.getStringExtra("reservationID")
                )
                saveTimeIntent.putExtra("TstartTime", startTime.toString())
                saveTimeIntent.putExtra("TendTime", endTime.toString())
                saveTimeIntent.putExtra("TcurrentUser", currentUser.toString())
                saveTimeIntent.putExtra("Tfullname", fullName)
                saveTimeIntent.putExtra("Tcompany", company)
                saveTimeIntent.putExtra("TuserType", userType)
                saveTimeIntent.putExtra("Ttitle", title.toString())
                saveTimeIntent.putExtra("TreservationName", reservationName.toString())
                saveTimeIntent.putExtra("Tagreement", agreement.toString())
                saveTimeIntent.putExtra("Tdate", date.toString())
                saveTimeIntent.putExtra("Tdesc", desc.toString())
                //endregion
                println("***********************************")
                println("Time Picker Activity: Title: $title - Full Name: $fullName")
                println("***********************************")
                startActivity(saveTimeIntent)
            }
        }
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