package com.example.getmymeeting

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MakeReservationActivity : AppCompatActivity() {

    //region Declare variables
    private var date: LocalDate? = null
    private var chosenStartTime: String? = null
    private var chosenEndTime: String? = null
    private var chosenTime: String? = null
    private var currentUser: String? = null
    private var formattedDate: String? = null
    private var selectedRoomId: Int = 0
    private var docCount: Int? = null
    private var isInternetConnected = true
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var spinnerRooms: Spinner
    private lateinit var titleTbox: EditText
    private lateinit var nameTbox: EditText
    private lateinit var descTbox: EditText
    private lateinit var agreementChk: CheckBox
    private lateinit var selectedRoom: String
    private lateinit var dateBtn: Button
    private lateinit var timeBtn: Button
    private lateinit var roomsAdapter: ArrayAdapter<String>
    private lateinit var db: FirebaseFirestore
    private lateinit var reservationId: String
    private lateinit var reservationColRef: CollectionReference
    private lateinit var source: String
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_make_reservation)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        try {
            //region initialize the variables
            titleTbox = findViewById(R.id.tboxTitle)
            nameTbox = findViewById(R.id.tboxName)
            agreementChk = findViewById(R.id.chkAgree)
            dateBtn = findViewById(R.id.btnSelectDate)
            timeBtn = findViewById(R.id.btnSelectTime)
            descTbox = findViewById(R.id.tboxDesc)
            trimmer = TrimTexts()
            db = FirebaseFirestore.getInstance()
            fullName = ""
            company = ""
            userType = ""
            setIntentValues()
            //endregion

            //region get a reference for the "meetings" collection and get the document count
            reservationColRef = db.collection("meetings")
            reservationColRef.get().addOnSuccessListener { querySnapshot ->
                docCount = querySnapshot.size()
            }.addOnFailureListener { e ->
                docCount = null
                println("Error getting document count: $e (332)")
            }
            //endregion

            //region set the time button's text according to the chosen time
            chosenTime = if (chosenStartTime != null && chosenEndTime != null) {
                "$chosenStartTime - $chosenEndTime"
            } else {
                println("chosen times are empty (683)")
                ""
            }
            timeBtn.text = chosenTime
            //endregion

            //region set up the spinner rooms
            // Find the spinner view for rooms
            spinnerRooms = findViewById(R.id.spnrRoom)

            // Initialize the selected room variable
            selectedRoom = ""

            // Create an ArrayAdapter for the spinner
            roomsAdapter = ArrayAdapter(
                this,
                R.layout.spinner_item
            )
            roomsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            // Set the adapter for the spinner
            spinnerRooms.adapter = roomsAdapter

            // Get the collection reference for rooms
            val roomColRef = db.collection("rooms")

            // Retrieve the rooms from Firestore
            roomColRef.get().addOnSuccessListener { querySnapshot ->
                // Create an ArrayList to store the room names
                val roomItems = ArrayList<String>()

                // Iterate through the documents in the query snapshot
                for (document in querySnapshot) {
                    // Get the data of the document
                    val data = document.data

                    // Get the room name from the data
                    val roomName = data["room_name"].toString()

                    // Add the room name to the ArrayList
                    roomItems.add(roomName)
                }

                // Add all the room names to the adapter
                roomsAdapter.addAll(roomItems)

                // Notify the adapter that the data has changed
                roomsAdapter.notifyDataSetChanged()
            }.addOnFailureListener { e ->
                // Display an error message if retrieving rooms fails
                println("Error retrieving rooms: $e (377)")
            }

            // Set the item selection listener for the spinner
            spinnerRooms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    // Get the selected room from the spinner
                    selectedRoom = parent.getItemAtPosition(position) as String

                    // Set the selected room ID
                    selectedRoomId = position
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Set the selected room ID to 0 if nothing is selected
                    selectedRoomId = 0
                }
            }
            //endregion

        } catch (e: java.lang.Exception) {
            println("Make reservation: $e (862)")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle the back button click here
        val intentBack = Intent(this, HomeActivity::class.java)
        intentBack.putExtra("source", "makeReservation")
        intentBack.putExtra("currentUser", currentUser)
        intentBack.putExtra("fullname", fullName)
        intentBack.putExtra("userType", userType)
        intentBack.putExtra("company", company)
        startActivity(intentBack)

        // Call super.onBackPressed() to allow the default back button behavior (e.g., finish the activity)
        super.onBackPressed()
    }

    private fun setIntentValues() {
        // T refers to Time. Coming from Choose Time Activity
        source = intent.getStringExtra("source").toString()
        if (source == "timePicker") {

            val title = intent.getStringExtra("Ttitle")
            val reservationName = intent.getStringExtra("TreservationName")
            val agreement = intent.getStringExtra("Tagreement")
            val desc = intent.getStringExtra("Tdesc")
            val date = intent.getStringExtra("Tdate")
            chosenEndTime = intent.getStringExtra("TendTime")
            chosenStartTime = intent.getStringExtra("TstartTime")

            currentUser = intent.getStringExtra("TcurrentUser")
            fullName = intent.getStringExtra("Tfullname").toString()
            company = intent.getStringExtra("Tcompany").toString()
            userType = intent.getStringExtra("TuserType").toString()

            titleTbox.setText(trimmer.trimText(title.toString()))
            nameTbox.setText(trimmer.trimText(reservationName.toString()))
            descTbox.setText(trimmer.trimText(desc.toString()))
            agreementChk.isChecked = agreement.toBoolean()
            dateBtn.text = date.toString()
        } else if (source == "profile" || source == "home") {
            // HP refers to Home & Profile. Coming from Home & Profile Activities
            currentUser = intent.getStringExtra("HPcurrentUser")
            fullName = intent.getStringExtra("HPname").toString()
            company = intent.getStringExtra("HPcompany").toString()
            userType = intent.getStringExtra("HPuserType").toString()
        }
    }

    fun onSelectDate(view: View) {
        // Create a date set listener to handle the selected date
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                // Update the "date" variable with the selected date
                date = LocalDate.of(year, monthOfYear + 1, dayOfMonth)

                // Format the selected date as a string
                formattedDate = date?.format(DateTimeFormatter.ISO_DATE)

                // Set the formatted date as the text of the button
                findViewById<Button>(R.id.btnSelectDate).text = formattedDate
            }

        // Get the current date
        val currentDate = LocalDate.now()

        // Get the last day of the current month
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth())

        // Create a date picker dialog
        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )

        // Set the minimum and maximum selectable dates
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.datePicker.maxDate =
            lastDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Show the date picker dialog
        datePickerDialog.show()
    }

    fun showTimePicker(view: View) {
        val timeIntent = Intent(this, TimePickerActivity::class.java)
        timeIntent.putExtra("source", "makeReservation")
        timeIntent.putExtra("currentUser", currentUser)
        timeIntent.putExtra("fullname", fullName)
        timeIntent.putExtra("company", company)
        timeIntent.putExtra("userType", userType)
        timeIntent.putExtra("title", trimmer.trimText(titleTbox.text.toString()))
        timeIntent.putExtra("desc", trimmer.trimText(descTbox.text.toString()))
        timeIntent.putExtra("room", spinnerRooms.selectedItem.toString())
        timeIntent.putExtra("reservationName", trimmer.trimText(nameTbox.text.toString()))
        timeIntent.putExtra("agreement", agreementChk.isChecked.toString())
        timeIntent.putExtra("date", dateBtn.text)
        startActivity(timeIntent)
    }

    fun onSaveButtonClicked(view: View) {
        try {
            println("Source is: $source")

            // Generate a unique reservation ID
            val id = "mt${docCount?.plus(1)}".hashCode()
            reservationId = id.toString()

            // Parse the chosen date and time
            val myDate = LocalDate.parse(dateBtn.text.toString())
            val nowDate = LocalDate.now()
            val myTime = LocalTime.parse(timeBtn.text.substring(0..4))
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

            when {
                titleTbox.text.none { !it.isWhitespace() } -> {
                    // Check if the title is empty or contains only whitespace
                    Toast.makeText(
                        this,
                        "Please enter a title for the reservation. (683)",
                        Toast.LENGTH_SHORT
                    ).show()
                    titleTbox.requestFocus()
                }
                trimmer.trimText(nameTbox.text.toString()).none { !it.isWhitespace() } -> {
                    // Check if the name is empty or contains only whitespace
                    Toast.makeText(
                        this,
                        "Please enter a name for the reservation. (683)",
                        Toast.LENGTH_SHORT
                    ).show()
                    nameTbox.requestFocus()
                }
                selectedRoom.none { !it.isWhitespace() } -> {
                    // Check if a room is selected
                    Toast.makeText(this, "Please select a room. (683)", Toast.LENGTH_SHORT).show()
                    spinnerRooms.requestFocus()
                }
                dateBtn.text.none { !it.isWhitespace() } -> {
                    // Check if a date is chosen
                    Toast.makeText(this, "Please choose a date. (683)", Toast.LENGTH_SHORT).show()
                    dateBtn.requestFocus()
                }
                timeBtn.text.none { !it.isWhitespace() } -> {
                    // Check if a time is chosen
                    Toast.makeText(this, "Please choose a time. (683)", Toast.LENGTH_SHORT).show()
                    timeBtn.requestFocus()
                }
                trimmer.trimText(descTbox.text.toString()).none { !it.isWhitespace() } -> {
                    // Check if the description is empty or contains only whitespace
                    Toast.makeText(
                        this,
                        "Please enter a description for the reservation. (683)",
                        Toast.LENGTH_LONG
                    ).show()
                    descTbox.requestFocus()
                }
                !agreementChk.isChecked -> {
                    // Check if the agreement checkbox is checked
                    Toast.makeText(
                        this,
                        "You have to agree to the Terms of Use in order to use the room. (683)",
                        Toast.LENGTH_LONG
                    ).show()
                    agreementChk.requestFocus()
                }
                myDate == nowDate && myTime < LocalTime.parse(nowTime) -> {
                    // Check if the chosen time is in the past
                    println("Past time chosen! (782)")
                    Toast.makeText(this, "You can't choose a past time. (782)", Toast.LENGTH_SHORT)
                        .show()
                }
                else -> {
                    // Proceed to check the availability and save the reservation
                    val reqRoom = spinnerRooms.selectedItem.toString()
                    val reqDate = dateBtn.text.toString()
                    val reqStartHour = chosenStartTime.toString().substring(0, 2).toInt()
                    val reqStartMin = chosenStartTime.toString().substring(3).toInt()
                    val reqEndHour = chosenEndTime.toString().substring(0, 2).toInt()
                    val reqEndMin = chosenEndTime.toString().substring(3).toInt()

                    checkBookingAvailability(
                        reqRoom,
                        reqDate,
                        reqStartHour,
                        reqStartMin,
                        reqEndHour,
                        reqEndMin
                    )
                }
            }
        } catch (e: Exception) {
            // Handle any exceptions that occur during the process
            Toast.makeText(this, "Error: (862)", Toast.LENGTH_LONG).show()
            println("Make reservation: $e (862)")
        }
    }

    private fun checkBookingAvailability(
        room: String,
        date: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): String {
        // Convert input values to LocalTime and LocalDate objects
        val reqStartTime = LocalTime.of(startHour, startMinute)
        val reqEndTime = LocalTime.of(endHour, endMinute)
        val reqDate = LocalDate.parse(date)

        // Create a query to check for existing reservations
        val query = reservationColRef.whereEqualTo("date", date)
            .whereEqualTo("room", room)
            .whereNotEqualTo("status", "deleted")

        query.get().addOnSuccessListener { snapshots ->
            for (doc in snapshots.documents) {
                // Retrieve booking details from the document
                val bookingStartTime = LocalTime.parse(doc.getString("start_time"))
                val bookingEndTime = LocalTime.parse(doc.getString("end_time"))
                val bookingDate = LocalDate.parse(doc.getString("date"))

                // Check if the requested time slot overlaps with an existing booking
                if (bookingStartTime <= reqEndTime && bookingEndTime >= reqStartTime && reqDate == bookingDate) {
                    Toast.makeText(
                        this,
                        "This time is not available. Please choose another time. (828)",
                        Toast.LENGTH_SHORT
                    ).show()
                    println("-*-*-* Chosen time isn't available. (828)-*-*-*")
                    return@addOnSuccessListener
                }
            }

            // If the requested time slot is available, proceed to save the reservation
            reservationColRef.get().addOnSuccessListener {
                // Create a HashMap to store reservation data
                val reservation = HashMap<String, Any>()
                reservation["title"] = trimmer.trimText(titleTbox.text.toString()).lowercase()
                reservation["name"] = trimmer.trimText(nameTbox.text.toString()).lowercase()
                reservation["room"] = spinnerRooms.selectedItem.toString()
                reservation["date"] = date
                reservation["start_time"] = timeBtn.text.toString().substring(0, 5)
                reservation["end_time"] = timeBtn.text.toString().substring(8)
                reservation["description"] = trimmer.trimText(descTbox.text.toString()).lowercase()
                reservation["user"] = currentUser.toString()
                reservation["status"] = "coming"

                // Save the reservation document
                val meetingsColRef = reservationColRef.document(reservationId)
                meetingsColRef.set(reservation).addOnSuccessListener {
                    println("Reservation save success. (327)")
                    Toast.makeText(
                        this,
                        "Reservation has been saved successfully! (327)",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Create an intent to start the reservation details activity
                    val intentSave = Intent(this, ReservationDetailsActivity::class.java)
                    intentSave.putExtra("source", "makeReservation")
                    intentSave.putExtra("currentUser", currentUser)
                    intentSave.putExtra("fullname", fullName)
                    intentSave.putExtra("userType", userType)
                    intentSave.putExtra("company", company)
                    intentSave.putExtra("SreservationId", reservationId)
                    intentSave.putExtra("Stitle", trimmer.trimText(titleTbox.text.toString()))
                    intentSave.putExtra("Sname", trimmer.trimText(nameTbox.text.toString()))
                    intentSave.putExtra("Sroom", selectedRoom)
                    intentSave.putExtra("Sdate", dateBtn.text)
                    intentSave.putExtra("Stime", timeBtn.text)
                    intentSave.putExtra("Sdesc", trimmer.trimText(descTbox.text.toString()))
                    startActivity(intentSave)
                }
                    .addOnFailureListener { error ->
                        println("-*-*-*Error creating document $error (323)-*-*-*")
                        Toast.makeText(
                            this,
                            "Error saving reservation: (323)",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }.addOnFailureListener { error ->
                reservationId = "433"
                println("-*-*-* Error getting document count: $error (332)-*-*-*")
            }
            if (reservationId == "433") {
                Toast.makeText(
                    this,
                    "Error occurred while trying to generate ID (433)",
                    Toast.LENGTH_LONG
                ).show()
                println("-*-*-* ID Error (433) -*-*-*")
            }
        }.addOnFailureListener { error ->
            println("-*-*-* Error retrieving documents. (373)-*-*-* $error")
        }
        return reservationId
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
        Snackbar.make(rootView, "Internet connection lost (425)", Snackbar.LENGTH_LONG).show()
    }
    //endregion
}