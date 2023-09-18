package com.example.getmymeeting

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PastReservationsActivity : AppCompatActivity() {

    //region Declare variables
    private var formattedDate: String? = null
    private var currentUser: String? = null
    private var isInternetConnected = true
    private var localDate: LocalDate? = null
    private lateinit var selectDateBtn: Button
    private lateinit var cardContainer: LinearLayout
    private lateinit var searchTbox: EditText
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var startTime: String
    private lateinit var endTime: String
    private lateinit var title: String
    private lateinit var room: String
    private lateinit var date: String
    private lateinit var description: String
    private lateinit var meetingColRef: CollectionReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private lateinit var noResultLbl: TextView
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_past_reservations)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initializing variables
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        meetingColRef = db.collection("meetings")
        noResultLbl = findViewById(R.id.noResultLbl)
        fullName = ""
        company = ""
        userType = ""
        selectDateBtn = findViewById(R.id.btnSelectDate)
        cardContainer = findViewById(R.id.reservationCards)
        searchTbox = findViewById(R.id.tboxSearch)
        trimmer = TrimTexts()
        setIntentValues()
        getReservations()
        //endregion

        //region get today's date
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy")
        val formattedToday = today.format(formatter)
        //endregion

        //region set text change event for the search box
        searchTbox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called before the text is changed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called when the text is being changed
            }

            override fun afterTextChanged(s: Editable?) {
                // Delete all the whitespaces from the search text
                val searchText = trimmer.trimText(searchTbox.text.toString())
                performSearch(searchText)
            }
        })
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

        // Call super.onBackPressed() to allow the default back button behavior (e.g., finish the activity)
        super.onBackPressed()
    }

    private fun setIntentValues() {
        currentUser = intent.getStringExtra("currentUser")
        fullName = intent.getStringExtra("name").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
    }

    private fun createReservationCard(
        title: String,
        date: String,
        time: String,
        description: String,
        room: String,
        context: Context,
        id: Int,
        parent: LinearLayout
    ) {
        val ID: Int = id
        //region Create the LinearLayout
        val reservationCard = LinearLayout(context) // context parameter
        reservationCard.id = R.id.reservationCard + ID // id parameter
        reservationCard.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reservationCard.setPadding(20, 10, 20, 10)
        reservationCard.setBackgroundResource(R.drawable.card_style)
        reservationCard.orientation = LinearLayout.VERTICAL
        reservationCard.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        if (reservationCard.layoutParams is LinearLayout.LayoutParams) {
            (reservationCard.layoutParams as LinearLayout.LayoutParams).setMargins(10, 25, 10, 25)
        }
        //endregion

        //region Create the LinearLayout for the title, separator, and room
        val titleRoomLayout = LinearLayout(context)
        titleRoomLayout.gravity = Gravity.CENTER
        titleRoomLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            11f
        )
        titleRoomLayout.setPadding(0, 0, 0, 10)
        //endregion

        //region Create the TextView for the title
        val reservationTitle = TextView(context)
        reservationTitle.id = R.id.reservationTitle + ID // id parameter
        reservationTitle.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reservationTitle.text = title // title parameter
        reservationTitle.textAlignment = View.TEXT_ALIGNMENT_CENTER
        reservationTitle.textSize = 16f
        reservationTitle.setTypeface(null, Typeface.BOLD)
        reservationTitle.setPadding(0, 0, 5, 0)
        //endregion

        //region Create the TextView for the Separator
        val separatorTextView = TextView(context)
        separatorTextView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        separatorTextView.text = " - "
        separatorTextView.textSize = 16f
        separatorTextView.setTypeface(null, Typeface.BOLD)
        //endregion

        //region Create the TextView for the room
        val lblRoom = TextView(context)
        lblRoom.id = R.id.lblRoom + ID // id parameter
        lblRoom.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lblRoom.text = room // room parameter
        lblRoom.textAlignment = View.TEXT_ALIGNMENT_CENTER
        lblRoom.textSize = 16f
        lblRoom.setTypeface(null, Typeface.BOLD)
        lblRoom.setPadding(5, 0, 0, 0)

        titleRoomLayout.addView(reservationTitle)
        titleRoomLayout.addView(separatorTextView)
        titleRoomLayout.addView(lblRoom)
        //endregion

        //region Create the LinearLayout for the localDate and time
        val dateTimeLayout = LinearLayout(context) // context parameter
        dateTimeLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dateTimeLayout.gravity = Gravity.CENTER
        dateTimeLayout.orientation = LinearLayout.HORIZONTAL
        //endregion

        //region Create the ImageView and TextView for the localDate
        val datePickerIcon = ImageView(context) // context parameter
        datePickerIcon.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        datePickerIcon.setImageResource(R.drawable.icon_date_picker)
        datePickerIcon.contentDescription = "Date picker icon"

        val reservationDate = TextView(context) // context parameter
        reservationDate.id = R.id.reservationDate // id parameter
        reservationDate.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reservationDate.text = date // localDate parameter
        reservationDate.textSize = 12.0f
        reservationDate.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        if (reservationDate.layoutParams is LinearLayout.LayoutParams) {
            (reservationDate.layoutParams as LinearLayout.LayoutParams).setMargins(20, 0, 20, 0)
        }
        //endregion

        //region Create the ImageView and TextView for the time
        val timeIcon = ImageView(context) // context parameter
        timeIcon.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        timeIcon.setImageResource(R.drawable.time_icon)
        timeIcon.contentDescription = "Time icon"

        val reservationTime = TextView(context) // context parameter
        reservationTime.id = R.id.reservationTime // id parameter
        reservationTime.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reservationTime.text = time // time parameter
        reservationTime.textSize = 12.0f
        reservationTime.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        if (reservationTime.layoutParams is LinearLayout.LayoutParams) {
            (reservationTime.layoutParams as LinearLayout.LayoutParams).setMargins(20, 0, 20, 0)
        }
        //endregion

        //region Create the TextView for the description
        val cardDescription = TextView(context) // context parameter
        cardDescription.id = R.id.cardDescription // id parameter
        cardDescription.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardDescription.text = description // description parameter
        cardDescription.textSize = 14.0f
        if (cardDescription.layoutParams is LinearLayout.LayoutParams) {
            (cardDescription.layoutParams as LinearLayout.LayoutParams).setMargins(0, 10, 0, 20)
        }
        cardDescription.visibility = View.GONE
        //endregion

        //region Add the localDate and time controls to the LinearLayout
        dateTimeLayout.addView(datePickerIcon)
        dateTimeLayout.addView(reservationDate)
        dateTimeLayout.addView(timeIcon)
        dateTimeLayout.addView(reservationTime)
        //endregion

        //region Add all the controls to the LinearLayout
        reservationCard.addView(titleRoomLayout)
        reservationCard.addView(dateTimeLayout)
        reservationCard.addView(cardDescription)
        parent.addView(reservationCard)
        //endregion

        //region Add the card click event
        reservationCard.setOnClickListener {
            if (cardDescription.visibility == View.VISIBLE) {
                cardDescription.animate().alpha(0f).setDuration(300).withEndAction {
                    cardDescription.visibility = View.GONE
                }.start()
            } else {
                cardDescription.visibility = View.VISIBLE
                cardDescription.animate().alpha(1f).setDuration(300).start()
            }
        }
        //endregion
    }

    fun onSelectDate(view: View) {
        val dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                localDate = LocalDate.of(year, monthOfYear + 1, dayOfMonth)

                formattedDate = localDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                selectDateBtn.text = formattedDate

                if (selectDateBtn.text != "Date") {
                    cardContainer.removeAllViews()
                    val colRef = db.collection("meetings")
                    val query =
                        colRef.whereEqualTo("user", currentUser)
                            .whereEqualTo("date", selectDateBtn.text)
                    query.get().addOnSuccessListener { documents ->
                        if (documents.size() > 0){
                            for (document in documents) {
                                val data = document.data
                                val docId = document.id.toInt()
                                title = data["title"].toString()
                                room = data["room"].toString()
                                date = data["date"].toString()
                                startTime = data["start_time"].toString()
                                endTime = data["end_time"].toString()
                                description = data["description"].toString()
                                createReservationCard(
                                    title,
                                    date,
                                    "$startTime $endTime",
                                    description,
                                    room,
                                    this,
                                    docId,
                                    cardContainer
                                )
                            }
                        } else {
                            noResultLbl.visibility = View.VISIBLE
                        }
                    }.addOnFailureListener { println("Error retrieving documents (373)") }
                }
            }

        val currentDate = LocalDate.now()
        val datePickerDialog = DatePickerDialog(
            this,
            dateSetListener,
            currentDate.year,
            currentDate.monthValue - 1,
            currentDate.dayOfMonth
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }


    fun performSearch(searchWord: String) {
        if (trimmer.trimText(searchWord).isNotEmpty()) {
            // clear existing card views
            cardContainer.removeAllViews()

            // define the end value for the search range
            val endValue = searchWord + "\uf8ff"

            // get the collection reference for "meetings"
            val colRef = db.collection("meetings")

            //region perform the search query
            val titleSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereEqualTo("status", "finished")
                    .whereGreaterThanOrEqualTo("title", searchWord)
                    .whereLessThanOrEqualTo("title", endValue)

            val nameSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereEqualTo("status", "finished")
                    .whereGreaterThanOrEqualTo("name", searchWord)
                    .whereLessThanOrEqualTo("name", endValue)

            val dateSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereEqualTo("status", "finished")
                    .whereGreaterThanOrEqualTo("date", searchWord)
                    .whereLessThanOrEqualTo("date", endValue)

            val descSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereEqualTo("status", "finished")
                    .whereGreaterThanOrEqualTo("description", searchWord)
                    .whereLessThanOrEqualTo("description", endValue)
            //endregion

            //region get the result of the search query
            val titleTask = titleSearch.get()
            val nameTask = nameSearch.get()
            val dateTask = dateSearch.get()
            val descTask = descSearch.get()
            //endregion

            //region wait for all tasks to complete
            Tasks.whenAllSuccess<QuerySnapshot>(titleTask, nameTask, dateTask, descTask)
                .addOnSuccessListener { results ->
                    // get the documents from the search results
                    val titleDocuments = results[0]
                    val nameDocuments = results[1]
                    val dateDocuments = results[2]
                    val descDocuments = results[3]

                    //region loop over the documents
                    //title loop
                    if (titleDocuments.size() > 0) {
                        noResultLbl.visibility = View.GONE
                        for (document in titleDocuments) {
                            val data = document.data
                            val docId = document.id.toInt()
                            title = data["title"].toString()
                            room = data["room"].toString()
                            date = data["date"].toString()
                            startTime = data["start_time"].toString()
                            endTime = data["end_time"].toString()
                            description = data["description"].toString()
                            createReservationCard(
                                title,
                                date,
                                "$startTime $endTime",
                                description,
                                room,
                                this,
                                docId,
                                cardContainer
                            )
                        }
                    }
                    //name loop
                    else if (nameDocuments.size() > 0) {
                        noResultLbl.visibility = View.GONE
                        for (document in nameDocuments) {
                            val data = document.data
                            val docId = document.id.toInt()
                            title = data["title"].toString()
                            room = data["room"].toString()
                            date = data["date"].toString()
                            startTime = data["start_time"].toString()
                            endTime = data["end_time"].toString()
                            description = data["description"].toString()
                            createReservationCard(
                                title,
                                date,
                                "$startTime $endTime",
                                description,
                                room,
                                this,
                                docId,
                                cardContainer
                            )
                        }
                    }
                    //date loop
                    else if (dateDocuments.size() > 0) {
                        noResultLbl.visibility = View.GONE
                        for (document in dateDocuments) {
                            val data = document.data
                            val docId = document.id.toInt()
                            title = data["title"].toString()
                            room = data["room"].toString()
                            date = data["date"].toString()
                            startTime = data["start_time"].toString()
                            endTime = data["end_time"].toString()
                            description = data["description"].toString()
                            createReservationCard(
                                title,
                                date,
                                "$startTime $endTime",
                                description,
                                room,
                                this,
                                docId,
                                cardContainer
                            )
                        }
                    }
                    //description loop
                    else if (descDocuments.size() > 0) {
                        noResultLbl.visibility = View.GONE
                        for (document in descDocuments) {
                            val data = document.data
                            val docId = document.id.toInt()
                            title = data["title"].toString()
                            room = data["room"].toString()
                            date = data["date"].toString()
                            startTime = data["start_time"].toString()
                            endTime = data["end_time"].toString()
                            description = data["description"].toString()
                            createReservationCard(
                                title,
                                date,
                                "$startTime $endTime",
                                description,
                                room,
                                this,
                                docId,
                                cardContainer
                            )
                        }
                    } else {
                        noResultLbl.visibility = View.VISIBLE
                    }
                    //endregion
                }.addOnFailureListener {
                    println("Error retrieving documents (373)")
                }
        } else {
            // clear existing card views
            cardContainer.removeAllViews()

            // hide no result label
            noResultLbl.visibility = View.GONE

            // retrieve all reservations
            getReservations()
        }
    }

    private fun getReservations() {
        // Firestore collection reference for "users"
        val usersColRef = db.collection("users")

        // Query to find documents in "users" collection where "email" field matches the currentUser
        val usersQuery = usersColRef.whereEqualTo("email", currentUser)

        // Retrieve user documents based on the query
        usersQuery.get().addOnSuccessListener { userDocuments ->
            for (userDocument in userDocuments) {
                val userData = userDocument.data

                // Extracting user data from the document
                fullName = userData["name"].toString()
                company = userData["company"].toString()
                userType = userData["userType"].toString()

                // Firestore collection reference for "meetings"
                val reservationsQuery = meetingColRef
                    .whereEqualTo("user", currentUser)
                    .whereEqualTo("status", "finished")

                // Retrieve "meeting" documents based on the query
                reservationsQuery.get().addOnSuccessListener { reservationDocuments ->
                    val docCount = reservationDocuments.size()
                    if (docCount > 0) {
                        for (reservationDocument in reservationDocuments) {
                            val reservationData = reservationDocument.data

                            // Extracting reservation data from the document
                            val docId = reservationDocument.id.toInt()
                            title = reservationData["title"].toString()
                            room = reservationData["room"].toString()
                            date = reservationData["date"].toString()
                            startTime = reservationData["start_time"].toString()
                            endTime = reservationData["end_time"].toString()
                            description = reservationData["description"].toString()

                            // Create a reservation card using the extracted data
                            createReservationCard(
                                title,
                                date,
                                "$startTime $endTime",
                                description,
                                room,
                                this,
                                docId,
                                findViewById(R.id.reservationCards)
                            )
                        }
                    } else {
                        findViewById<TextView>(R.id.noResultLbl).visibility = View.VISIBLE
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(this, "Error getting your reservation data! (377-1)", Toast.LENGTH_SHORT)
                        .show()
                    println("Error getting reservations' data: $e (377-1)")
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error getting the user data! (378)", Toast.LENGTH_SHORT).show()
            println("Error getting user data: $e (378)")
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
        Snackbar.make(rootView, "Internet connection lost (425)", Snackbar.LENGTH_LONG).show()
    }
    //endregion
}