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
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class EditReservationActivity : AppCompatActivity() {

    //region Declaring variables
    private var date: LocalDate? = null
    private var chosenStartTime: String? = null
    private var chosenEndTime: String? = null
    private var currentUser: String? = null
    private var formattedDate: String? = null
    private var selectedRoomId: Int = 0
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
    private lateinit var meetingColRef: CollectionReference
    private lateinit var source: String
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_reservation)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        try {
            //region initialize variables
            titleTbox = findViewById(R.id.tboxTitle)
            nameTbox = findViewById(R.id.tboxName)
            agreementChk = findViewById(R.id.chkAgree)
            dateBtn = findViewById(R.id.btnSelectDate)
            timeBtn = findViewById(R.id.btnSelectTime)
            descTbox = findViewById(R.id.tboxDesc)
            trimmer = TrimTexts()
            db = FirebaseFirestore.getInstance()
            meetingColRef = db.collection("meetings")
            fullName = ""
            company = ""
            userType = ""
            setIntentValues()
            //endregion

            //region Spinner rooms
            spinnerRooms = findViewById(R.id.spnrRoom)
            selectedRoom = ""
            roomsAdapter = ArrayAdapter(
                this, R.layout.spinner_item
            )
            roomsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerRooms.adapter = roomsAdapter

            val roomColRef = db.collection("rooms")
            roomColRef.get().addOnSuccessListener { querySnapshot ->
                val roomItems = ArrayList<String>()
                for (document in querySnapshot) {
                    val data = document.data
                    val roomName = data["room_name"].toString()
                    roomItems.add(roomName)
                }
                roomsAdapter.addAll(roomItems)
                roomsAdapter.notifyDataSetChanged()
            }.addOnFailureListener { e ->
                println("Error retrieving rooms: $e (377)") // 377 = ERR = Error Retrieve Rooms
                Toast.makeText(this, "Error retrieving rooms: $e (377)", Toast.LENGTH_SHORT).show() // 377 = ERR = Error Retrieve Rooms
            }

            spinnerRooms.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    selectedRoom = parent.getItemAtPosition(position) as String
                    selectedRoomId = position
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    selectedRoomId = 0
                }
            }
            //endregion
        } catch (e: java.lang.Exception) {
            Toast.makeText(this, "Error occurred: $e (862)", Toast.LENGTH_SHORT).show() // 862 = UNE = UnKnown Error
            println("Edit Reservation: $e (862)") // 862 = UNE = UnKnown Error
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Handle the back button click here
        val intentBack = Intent(this, HomeActivity::class.java)
        intentBack.putExtra("source", "addRoom")
        intentBack.putExtra("currentUser", currentUser)
        intentBack.putExtra("fullname", fullName)
        intentBack.putExtra("userType", userType)
        intentBack.putExtra("company", company)
        startActivity(intentBack)

        // Call super.onBackPressed() to allow the default back button behavior (e.g., finish the activity)
        super.onBackPressed()
    }

    private fun setIntentValues() {
        source = intent.getStringExtra("source").toString()
        if (source == "timePicker") {
            source = intent.getStringExtra("source").toString()
            currentUser = intent.getStringExtra("TcurrentUser")
            fullName = intent.getStringExtra("Tfullname").toString()
            company = intent.getStringExtra("Tcompany").toString()
            userType = intent.getStringExtra("TuserType").toString()
            reservationId = intent.getStringExtra("TreservationID").toString()
            titleTbox.setText(intent.getStringExtra("Ttitle"))
            nameTbox.setText(intent.getStringExtra("TreservationName"))
            dateBtn.text = intent.getStringExtra("Tdate")
            chosenStartTime = intent.getStringExtra("TstartTime")
            chosenEndTime = intent.getStringExtra("TendTime")
            descTbox.setText(intent.getStringExtra("Tdesc"))
            timeBtn.text = "$chosenStartTime - $chosenEndTime"
        } else if (source == "reservationDetails") {
            reservationId = intent.getStringExtra("reservationID").toString()
            currentUser = intent.getStringExtra("currentUser")
            fullName = intent.getStringExtra("name").toString()
            company = intent.getStringExtra("company").toString()
            userType = intent.getStringExtra("userType").toString()
            setContent()
        }
    }

    fun onSelectDate(view: View) {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                date = LocalDate.of(year, monthOfYear + 1, dayOfMonth)
            }
        val currentDate = LocalDate.now()
        val lastDayOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.datePicker.maxDate =
            lastDayOfMonth.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        datePickerDialog.show()

        formattedDate = date?.format(DateTimeFormatter.ISO_DATE)
        findViewById<Button>(R.id.btnSelectDate).text = formattedDate
    }

    fun showTimePicker(view: View) {
        val timeIntent = Intent(this, TimePickerActivity::class.java)
        timeIntent.putExtra("source", "editReservation")
        timeIntent.putExtra("currentUser", currentUser)
        timeIntent.putExtra("reservationID", reservationId)
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

    private fun checkBooking(
        roomId: String,
        date: String,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        resId: String
    ): String {
        // Convert the input values to LocalTime and LocalDate objects
        val reqStartTime = LocalTime.of(startHour, startMinute)
        val reqEndTime = LocalTime.of(endHour, endMinute)
        val reqDate = LocalDate.parse(date)

        // Construct the query to check for conflicting bookings
        val query = meetingColRef
            .whereEqualTo("date", date)
            .whereEqualTo("room", roomId)
            .whereEqualTo("status", "coming")
            .whereNotEqualTo(FieldPath.documentId(), resId)

        query.get().addOnSuccessListener { snapshots ->
            for (doc in snapshots.documents) {
                val bookingStartTime = LocalTime.parse(doc.getString("start_time"))
                val bookingEndTime = LocalTime.parse(doc.getString("end_time"))
                val bookingDate = LocalDate.parse(doc.getString("date"))
                if (bookingStartTime <= reqEndTime && bookingEndTime >= reqStartTime && reqDate == bookingDate) {
                    Toast.makeText(
                        this,
                        "This time is not available. Please choose another time. (828)", // 828 = UAT = unAvailable Time
                        Toast.LENGTH_SHORT
                    ).show()
                    println("Unavailable time (828)") // 828 = UAT = unAvailable Time
                    return@addOnSuccessListener
                }
            }
            println("No conflicts detected.")

            // If there are no conflicting bookings, proceed with editing the reservation
            meetingColRef.document(resId).get().addOnSuccessListener { reservationDoc ->
                if (reservationDoc.exists()) {
                    // Update the reservation details
                    val meeting = HashMap<String, Any>()
                    meeting["title"] = trimmer.trimText(titleTbox.text.toString()).lowercase()
                    meeting["name"] = trimmer.trimText(nameTbox.text.toString()).lowercase()
                    meeting["room"] = spinnerRooms.selectedItem.toString()
                    meeting["date"] = date
                    meeting["start_time"] = timeBtn.text.toString().substring(0, 5)
                    meeting["end_time"] = timeBtn.text.toString().substring(8)
                    meeting["description"] = trimmer.trimText(descTbox.text.toString()).lowercase()
                    meeting["user"] = currentUser.toString()

                    // Update the reservation document with the modified meeting details
                    reservationDoc.reference.update(meeting).addOnSuccessListener {
                        println("Reservation update success. (787)") // 787 = RUS = Reservation Update Success
                        Toast.makeText(
                            this, "Reservation has been updated successfully! (787)", Toast.LENGTH_SHORT // 787 = RUS = Reservation Update Success
                        ).show()

                        // Proceed to the reservation details activity
                        val intentReservationDetails =
                            Intent(this, ReservationDetailsActivity::class.java)
                        //region intent values
                        intentReservationDetails.putExtra("source", "editReservation")
                        intentReservationDetails.putExtra("currentUser", currentUser)
                        intentReservationDetails.putExtra("fullname", fullName)
                        intentReservationDetails.putExtra("userType", userType)
                        intentReservationDetails.putExtra("company", company)
                        intentReservationDetails.putExtra("EreservationId", reservationId)
                        intentReservationDetails.putExtra(
                            "Etitle",
                            trimmer.trimText(titleTbox.text.toString())
                        )
                        intentReservationDetails.putExtra(
                            "Ename",
                            trimmer.trimText(nameTbox.text.toString())
                        )
                        intentReservationDetails.putExtra("Eroom", selectedRoom)
                        intentReservationDetails.putExtra("Edate", dateBtn.text)
                        intentReservationDetails.putExtra("Etime", timeBtn.text)
                        intentReservationDetails.putExtra(
                            "Edesc",
                            trimmer.trimText(descTbox.text.toString())
                        )
                        //endregion
                        startActivity(intentReservationDetails)
                    }.addOnFailureListener { error ->
                        println("-*-*-* Error updating reservation: $error  (387)-*-*-*") //TODO
                        Toast.makeText(
                            this, "Error updating reservation: ${error.message} (387)", Toast.LENGTH_SHORT //TODO
                        ).show()
                    }
                } else {
                    println("-*-*-* Reservation document not found.  (762)-*-*-*") // 762 = RNF = Reservation Not Found
                    Toast.makeText(
                        this, "Error: Reservation not found. (762)", Toast.LENGTH_SHORT // 762 = RNF = Reservation Not Found
                    ).show()
                }
            }.addOnFailureListener { error ->
                println("-*-*-* Error retrieving reservation document: $error (373)-*-*-*") // 373 = ERD = Error Retrieve Document
                Toast.makeText(
                    this,
                    "Error retrieving reservation: ${error.message} (373)", // 373 = ERD = Error Retrieve Document
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener { error ->
            println("-*-*-* Error retrieving reservation document. (373)-*-*-* $error") // 373 = ERD = Error Retrieve Document
            Toast.makeText(
                this, "Error retrieving reservation: ${error.message} (373)", Toast.LENGTH_SHORT // 373 = ERD = Error Retrieve Document
            ).show()
        }
        return resId
    }

    private fun setContent() {
        val query = meetingColRef.document(reservationId).get()
        query.addOnSuccessListener { document ->
            if (document.exists() && source == "reservationDetails") {
                val title = document.getString("title")
                val name = document.getString("name")
                val date = document.getString("date")
                val startTime = document.getString("start_time")
                val endTime = document.getString("end_time")
                val desc = document.getString("description")

                chosenStartTime = startTime
                chosenEndTime = endTime
                titleTbox.setText(title)
                nameTbox.setText(name)
                agreementChk.isChecked = true
                dateBtn.text = date
                timeBtn.text = "$startTime - $endTime"
                descTbox.setText(desc)
            } else {
                println("Document doesn't exist") // 363 = DNE = Document Not Exist
                Toast.makeText(this, "Reservation doesn't exist.", Toast.LENGTH_SHORT).show() // 363 = DNE = Document Not Exist
            }
        }
    }

    fun onDiscardButtonClicked(view: View) {
        //create an intent to home
        val profileIintent = Intent(this, HomeActivity::class.java)
        profileIintent.putExtra("fullname", fullName)
        profileIintent.putExtra("company", company)
        profileIintent.putExtra("userType", userType)
        profileIintent.putExtra("currentUser", currentUser)
        //start the activity
        startActivity(profileIintent)
    }

    fun onSaveButtonClick(view: View) {
        try {
            println("Source is: $source")
            val myDate = LocalDate.parse(dateBtn.text.toString())
            val nowDate = LocalDate.now()
            val myTime = LocalTime.parse(timeBtn.text.substring(0..4))
            val nowTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))

            when {
                titleTbox.text.none { !it.isWhitespace() } -> {
                    // Check if the title is empty or contains only whitespace
                    Toast.makeText(
                        this, "Please enter a title for the reservation. (683)", Toast.LENGTH_SHORT // 683 = MTF = eMpTy Field
                    ).show()
                    titleTbox.requestFocus()
                }
                trimmer.trimText(nameTbox.text.toString()).none { !it.isWhitespace() } -> {
                    // Check if the name is empty or contains only whitespace
                    Toast.makeText(
                        this, "Please enter a name for the reservation. (683)", Toast.LENGTH_SHORT // 683 = MTF = eMpTy Field
                    ).show()
                    nameTbox.requestFocus()
                }
                selectedRoom.none { !it.isWhitespace() } -> {
                    // Check if a room is selected
                    Toast.makeText(
                        this, "Please select a room. (683)", Toast.LENGTH_SHORT // 683 = MTF = eMpTy Field
                    ).show()
                    spinnerRooms.requestFocus()
                }
                dateBtn.text.none { !it.isWhitespace() } -> {
                    // Check if a date is chosen
                    Toast.makeText(
                        this, "Please choose date. (683)", Toast.LENGTH_SHORT // 683 = MTF = eMpTy Field
                    ).show()
                    dateBtn.requestFocus()
                }
                timeBtn.text.none { !it.isWhitespace() } -> {
                    // Check if a time is chosen
                    Toast.makeText(
                        this, "Please choose time. (683)", Toast.LENGTH_SHORT // 683 = MTF = eMpTy Field
                    ).show()
                    timeBtn.requestFocus()
                }
                trimmer.trimText(descTbox.text.toString()).none { !it.isWhitespace() } -> {
                    // Check if the description is empty or contains only whitespace
                    Toast.makeText(
                        this, "Please enter a description for the reservation. (683)", Toast.LENGTH_LONG // 683 = MTF = eMpTy Field
                    ).show()
                    descTbox.requestFocus()
                }
                !agreementChk.isChecked -> {
                    // Check if the agreement checkbox is checked
                    Toast.makeText(
                        this,
                        "You have to agree to the Terms of Use in order to use the room. (683)", // 683 = MTF = eMpTy Field
                        Toast.LENGTH_LONG
                    ).show()
                    agreementChk.requestFocus()
                }
                myDate == nowDate && myTime < LocalTime.parse(nowTime) -> {
                    // Check if the chosen time is in the past
                    println("Past time chosen! (782)") // 782 = PTC = Past Time Chosen
                    Toast.makeText(this, "You can't choose a past time. (782)", Toast.LENGTH_SHORT).show() // 782 = PTC = Past Time Chosen
                }
                else -> {
                    // Proceed to check the availability and save the reservation
                    val reqRoom = spinnerRooms.selectedItem.toString()
                    val reqDate = dateBtn.text.toString()
                    val reqStartHour =
                        chosenStartTime.toString().substring(0, 2).toInt() // 14:55 -> 14 INT
                    val reqStartMin =
                        chosenStartTime.toString().substring(3).toInt() // 14:55 -> 55 INT
                    val reqEndHour =
                        chosenEndTime.toString().substring(0, 2).toInt() // 15:55 -> 15 INT
                    val reqEndMin = chosenEndTime.toString().substring(3).toInt() // 15:55 -> 55 INT

                    checkBooking(
                        reqRoom,
                        reqDate,
                        reqStartHour,
                        reqStartMin,
                        reqEndHour,
                        reqEndMin,
                        reservationId
                    )
                }
            }
        } catch (e: java.lang.Exception) {
            // Handle any exceptions that occur during the process
            Toast.makeText(this, "Error saving changes: $e (372)", Toast.LENGTH_LONG).show() // 372 = ESC = Error Saving Changes
            println("Edit Reservation: Error saving changes: $e (372)") // 372 = ESC = Error Saving Changes
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
        Snackbar.make(rootView, "Internet connection lost (425)", Snackbar.LENGTH_LONG).show() // 425 = ICL = Internet Connection Lost
    }
    //endregion
}