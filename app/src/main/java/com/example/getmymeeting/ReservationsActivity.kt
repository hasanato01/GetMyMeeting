package com.example.getmymeeting

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ReservationsActivity : AppCompatActivity() {

    //region declare variables
    private lateinit var selectDateBtn: Button
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var meetingsColRef: CollectionReference
    private lateinit var reservationsTbl: TableLayout
    private lateinit var reservationId: String
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private lateinit var name: String
    private var formattedDate: String? = null
    private var localDate: LocalDate? = null
    private var rowsCount: Int = 0
    private var startTime: String? = ""
    private var endTime: String? = ""
    private var date: String? = ""
    private var currentUser: String? = null
    private var isInternetConnected = true
    private val textSizeInSp = 16
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reservations)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region Initializing variables
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        meetingsColRef = db.collection("meetings")
        reservationsTbl = findViewById(R.id.reservations)
        selectDateBtn = findViewById(R.id.btnSelectDate)
        fullName = ""
        company = ""
        userType = ""
        name = ""
        setIntentValues()
        getReservationsCards()
        //endregion

        //region Checking if the user is an admin
        if (userType != "admin") {
            println("User not authorized!")
            Toast.makeText(this, "You are not authorized to enter this page!", Toast.LENGTH_SHORT)
                .show()
            startActivity(Intent(this, HomeActivity::class.java))
        }
        //endregion
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle the back button click here
        val intentBack = Intent(this, ProfileActivity::class.java)
        intentBack.putExtra("source", "addRoom")
        intentBack.putExtra("currentUser", currentUser)
        intentBack.putExtra("fullname", fullName)
        intentBack.putExtra("userType", userType)
        intentBack.putExtra("company", company)
        startActivity(intentBack)

        // Call super.onBackPressed() to allow the default back button behavior
        super.onBackPressed()
    }

    private fun setIntentValues() {
        currentUser = intent.getStringExtra("currentUser")
        fullName = intent.getStringExtra("fullname").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
    }

    private fun sendIntentValues(btn: Button, ID: Int) {
        btn.setOnClickListener {
            val intent = Intent(this, ReservationDetailsActivity::class.java)
            intent.putExtra("source", "reservationsAdmin")
            intent.putExtra("currentUser", currentUser)
            intent.putExtra("reservationId", ID)
            println(reservationId)
            startActivity(intent)
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

    fun onSelectDate(view: View) {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                localDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)

                formattedDate = localDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                selectDateBtn.text = formattedDate
                createFilteredReservationButtons(this, reservationsTbl, formattedDate.toString())
            }
        val currentDate = LocalDate.now()
        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )
        datePickerDialog.show()
    }

    private fun createReservationButtons(context: Context, parent: TableLayout) {
        // Get a limited number of documents from the meetings collection
        meetingsColRef.limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Retrieve all documents from the meetings collection
                    meetingsColRef.get()
                        .addOnSuccessListener { reservation ->
                            // Get the total count of reservations
                            val count = reservation.size()
                            var row: TableRow? = null
                            // Iterate over the reservations
                            for (i in 0 until count) {
                                // Retrieve reservation details from the document
                                startTime = reservation.documents[i].get("start_time") as String?
                                endTime = reservation.documents[i].get("end_time") as String?
                                date = reservation.documents[i].get("date") as String?
                                name = reservation.documents[i].get("name").toString()
                                reservationId = reservation.documents[i].id

                                // Create a new button for the reservation
                                val button = Button(context)
                                button.id = reservationId.toInt()
                                button.text = "$name\n $startTime - $endTime \n $date"
                                button.isAllCaps = false
                                button.setTextSize(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    textSizeInSp.toFloat()
                                )
                                button.setBackgroundResource(R.drawable.default_button_style)
                                val buttonLayoutParams = TableRow.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                                )
                                buttonLayoutParams.setMargins(
                                    5.dpToPx(),
                                    5.dpToPx(),
                                    5.dpToPx(),
                                    5.dpToPx()
                                )
                                button.layoutParams = buttonLayoutParams

                                if (i % 2 == 0) {
                                    // Create a new row for every second reservation
                                    row = TableRow(context)

                                    // Set layout parameters for the row
                                    val rowLayoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.WRAP_CONTENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
                                    rowLayoutParams.setMargins(
                                        5.dpToPx(), // left margin
                                        5.dpToPx(), // top margin
                                        5.dpToPx(), // right margin
                                        5.dpToPx() // bottom margin
                                    )
                                    row.layoutParams = rowLayoutParams
                                    row.gravity = Gravity.CENTER_HORIZONTAL

                                    // Add the row to the parent table layout
                                    parent.addView(row)
                                }

                                // Add the button to the row
                                row?.addView(button)

                                // Set a click listener for the button
                                sendIntentValues(button, reservationId.toInt())
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
        println("Table rows added to parent")
    }

    private fun createFilteredReservationButtons(
        context: Context,
        parent: TableLayout,
        filter: String
    ) {
        // Get a limited number of documents from the meetings collection
        parent.removeAllViews()
        meetingsColRef.limit(1).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    // Retrieve all documents from the meetings collection
                    meetingsColRef.whereEqualTo("date", filter).get()
                        .addOnSuccessListener { reservation ->
                            // Get the total count of reservations
                            val count = reservation.size()

                            var row: TableRow? = null

                            // Iterate over the reservations
                            for (i in 0 until count) {
                                // Retrieve reservation details from the document
                                startTime = reservation.documents[i].get("start_time") as String?
                                endTime = reservation.documents[i].get("end_time") as String?
                                date = reservation.documents[i].get("date") as String?
                                name = reservation.documents[i].get("name").toString()
                                reservationId = reservation.documents[i].id

                                // Create a new button for the reservation
                                val button = Button(context)
                                button.id = reservationId.toInt()
                                button.text = "$name\n $startTime - $endTime \n $date"
                                button.isAllCaps = false
                                button.setTextSize(
                                    TypedValue.COMPLEX_UNIT_SP,
                                    textSizeInSp.toFloat()
                                )
                                button.setBackgroundResource(R.drawable.default_button_style)
                                val buttonLayoutParams = TableRow.LayoutParams(
                                    TableRow.LayoutParams.MATCH_PARENT,
                                    TableRow.LayoutParams.WRAP_CONTENT
                                )
                                buttonLayoutParams.setMargins(
                                    5.dpToPx(),
                                    5.dpToPx(),
                                    5.dpToPx(),
                                    5.dpToPx()
                                )
                                button.layoutParams = buttonLayoutParams

                                if (i % 2 == 0) {
                                    // Create a new row for every second reservation
                                    row = TableRow(context)

                                    // Set layout parameters for the row
                                    val trLayoutParams = TableRow.LayoutParams(
                                        TableRow.LayoutParams.MATCH_PARENT,
                                        TableRow.LayoutParams.WRAP_CONTENT
                                    )
                                    trLayoutParams.setMargins(
                                        5.dpToPx(),
                                        5.dpToPx(),
                                        5.dpToPx(),
                                        5.dpToPx()
                                    )
                                    row.layoutParams = trLayoutParams
                                    row.gravity = Gravity.CENTER_HORIZONTAL

                                    // Add the row to the parent table layout
                                    parent.addView(row)
                                }

                                // Add the button to the row
                                row?.addView(button)

                                // Set a click listener for the button
                                sendIntentValues(button, reservationId.toInt())
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
        println("Table rows added to parent")
    }

    private fun getReservationsCards() {
        //Getting rows count and creating buttons
        meetingsColRef.get().addOnSuccessListener { querySnapshot ->
            rowsCount = querySnapshot.size()
            println("Rows count: $rowsCount")
        }.addOnFailureListener { e ->
            println("Error getting document count: $e")
        }
        for (i in 0..rowsCount) {
            createReservationButtons(this, reservationsTbl)
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