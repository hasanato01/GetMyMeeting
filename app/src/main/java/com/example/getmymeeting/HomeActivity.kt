package com.example.getmymeeting

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    //region Create variables
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var meetingColRef: CollectionReference
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var bottomNavbar: BottomNavigationView
    private lateinit var searchTbox: EditText
    private lateinit var cardContainer: LinearLayout
    private lateinit var startTime: String
    private lateinit var endTime: String
    private lateinit var title: String
    private lateinit var name: String
    private lateinit var room: String
    private lateinit var date: String
    private lateinit var description: String
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var profilePicture: ImageView
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private lateinit var trimmer: TrimTexts
    private lateinit var profilePictureUrl: String
    private var currentUser: String? = null
    private var isInternetConnected = true
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initialize variables
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        meetingColRef = db.collection("meetings")
        bottomNavbar = findViewById(R.id.bottomNav)
        searchTbox = findViewById(R.id.tboxSearch)
        cardContainer = findViewById(R.id.cardContainer)
        profilePicture = findViewById(R.id.profilePicture)
        fullName = ""
        company = ""
        userType = ""
        startTime = ""
        endTime = ""
        title = ""
        name = ""
        description = ""
        room = ""
        date = ""
        trimmer = TrimTexts()
        //endregion

        //region check if the user is logged in, if it's not, redirect to Login
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            //call the primary functions
            setFinishedReservations()
            getIntentValues()
            getReservations()
            setBottomNavbar()
            val currentUserEmail = currentUser.toString().replace("@", "_").replace(".", "_")
            profilePictureUrl =
                FirebaseStorage.getInstance().reference.child("images/$currentUserEmail.jpg")
                    .toString()
            println(profilePictureUrl)
            //saveUserInfo(fullName, company, userType, profilePictureUrl)
        }
        //endregion

        //set click event for the profile picture
        findViewById<ImageView>(R.id.profilePicture).setOnClickListener {
            val editProfileIntent = Intent(this, EditProfileActivity::class.java)
            editProfileIntent.putExtra("source", "home")
            editProfileIntent.putExtra("fullName", fullName)
            editProfileIntent.putExtra("currentUser", currentUser)
            editProfileIntent.putExtra("company", company)
            editProfileIntent.putExtra("userType", userType)
            startActivity(editProfileIntent)
        }

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

    private fun setBottomNavbar(){
        bottomNavbar = findViewById(R.id.bottomNav)
        bottomNavbar.selectedItemId = R.id.profile
        bottomNavbar.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    val homeIntent = Intent(this, HomeActivity::class.java)
                    homeIntent.putExtra("fullname", fullName)
                    homeIntent.putExtra("company", company)
                    homeIntent.putExtra("currentUser", currentUser)
                    homeIntent.putExtra("userType", userType)
                    startActivity(homeIntent)
                    true
                }
                R.id.profile -> {
                    true
                }
                R.id.newReservation -> {
                    val newReservationIntent = Intent(this, MakeReservationActivity::class.java)
                    newReservationIntent.putExtra("source", "profile")
                    newReservationIntent.putExtra("HPfullname", fullName)
                    newReservationIntent.putExtra("HPcompany", company)
                    newReservationIntent.putExtra("HPcurrentUser", currentUser)
                    newReservationIntent.putExtra("HPuserType", userType)
                    startActivity(newReservationIntent)
                    false
                }
                R.id.logout -> {
                    currentUser = null
                    firebaseAuth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun saveUserInfo(
        fullname: String,
        company: String,
        userType: String,
        profilePictureUrl: String
    ) {
        val sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("name", fullname)
        editor.putString("company", company)
        editor.putString("userType", userType)
        editor.putString("profilePictureUrl", profilePictureUrl)
        editor.apply()
    }

    private fun setFinishedReservations() {
        // Create a date format to format the current date
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        // Get the current date as a formatted string
        val today = dateFormat.format(Date())

        // Query reservations where the date is less than today and the status is "coming"
        val query = meetingColRef.whereLessThan("date", today).whereEqualTo("status", "coming")

        // Execute the query
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                // Get a reference to the document that needs to be updated
                val documentRef = meetingColRef.document(document.id)

                // Update the "status" field to "finished"
                documentRef.update("status", "finished")
                    .addOnSuccessListener {
                        // Update successful, print a success message
                        println("Reservation status updated for document: ${document.id} (778)") // 778 = RSU = Reservation Status Updated
                    }
                    .addOnFailureListener { e ->
                        // Update failed, print an error message
                        println("Reservation update failed for document: ${document.id}, Error: $e (773)") // 773 = RSF = Reservation Status Failed
                    }
            }
        }
    }

    private fun getIntentValues() {
        fullName = intent.getStringExtra("fullName").toString()
        currentUser = intent.getStringExtra("currentUser").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
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

                // Update the TextView with the trimmed and uppercase full name
                findViewById<TextView>(R.id.userName).text = trimmer.trimText(fullName).uppercase()

                // Firestore collection reference for "meetings"
                val reservationsQuery = meetingColRef
                    .whereEqualTo("user", currentUser)
                    .whereEqualTo("status", "coming")

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
                                findViewById(R.id.cardContainer)
                            )
                        }
                    } else {
                        findViewById<TextView>(R.id.noResLbl).visibility = View.VISIBLE
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Error getting your reservation data! (373)",
                        Toast.LENGTH_SHORT
                    ) // 373 = ERD = Error Retrieve Document
                        .show()
                    println("Error getting reservations' data: $e (373)") // 373 = ERD = Error Retrieve Document
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Error getting the user data! (378)", Toast.LENGTH_SHORT)
                .show() // 378 = ERU = Error Retrieve User
            println("Error getting user data: $e (378)") // 378 = ERU = Error Retrieve User
        }

        // Firebase Storage reference for the user's profile image
        val storageRef =
            FirebaseStorage.getInstance().reference.child(
                "images/profile_${
                    currentUser.toString().replace("@", "_").replace(".", "_")
                }.jpg"
            )

        // Create a temporary local file to store the profile image
        val localFile = File.createTempFile("tempImage", "jpg")

        // Retrieve the profile image file from Firebase Storage and load it into an ImageView
        storageRef.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            profilePicture.setImageBitmap(bitmap)
        }.addOnFailureListener {
            //Toast.makeText(this, "Failed to load the image", Toast.LENGTH_SHORT).show()
        }
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
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reservationCard.setPadding(20, 10, 20, 10)
        reservationCard.setBackgroundResource(R.drawable.card_style)
        reservationCard.orientation = LinearLayout.VERTICAL
        reservationCard.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        if (reservationCard.layoutParams is LinearLayout.LayoutParams) {
            (reservationCard.layoutParams as LinearLayout.LayoutParams).setMargins(10, 25, 10, 25)
        }
        //endregion

        //region Create the LinearLayout for the title, separator, and room
        val titleRoomLayout = LinearLayout(context)
        titleRoomLayout.gravity = Gravity.CENTER
        titleRoomLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 11f
        )
        titleRoomLayout.setPadding(0, 0, 0, 10)
        //endregion

        //region Create the TextView for the title
        val reservationTitle = TextView(context)
        reservationTitle.id = R.id.reservationTitle + ID // id parameter
        reservationTitle.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
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
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        separatorTextView.text = " - "
        separatorTextView.textSize = 16f
        separatorTextView.setTypeface(null, Typeface.BOLD)
        //endregion

        //region Create the TextView for the room
        val lblRoom = TextView(context)
        lblRoom.id = R.id.lblRoom + ID // id parameter
        lblRoom.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
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

        //region Create the LinearLayout for the date and time
        val dateTimeLayout = LinearLayout(context) // context parameter
        dateTimeLayout.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dateTimeLayout.gravity = Gravity.CENTER
        dateTimeLayout.orientation = LinearLayout.HORIZONTAL
        //endregion

        //region Create the ImageView and TextView for the date
        val datePickerIcon = ImageView(context) // context parameter
        datePickerIcon.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        datePickerIcon.setImageResource(R.drawable.icon_date_picker)
        datePickerIcon.contentDescription = "Date picker icon"

        val reservationDate = TextView(context) // context parameter
        reservationDate.id = R.id.reservationDate // id parameter
        reservationDate.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        reservationDate.text = date // date parameter
        reservationDate.textSize = 12.0f
        reservationDate.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        if (reservationDate.layoutParams is LinearLayout.LayoutParams) {
            (reservationDate.layoutParams as LinearLayout.LayoutParams).setMargins(20, 0, 20, 0)
        }
        //endregion

        //region Create the ImageView and TextView for the time
        val timeIcon = ImageView(context) // context parameter
        timeIcon.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        timeIcon.setImageResource(R.drawable.time_icon)
        timeIcon.contentDescription = "Time icon"

        val reservationTime = TextView(context) // context parameter
        reservationTime.id = R.id.reservationTime // id parameter
        reservationTime.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
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
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        )
        cardDescription.text = description // description parameter
        cardDescription.textSize = 14.0f
        if (cardDescription.layoutParams is LinearLayout.LayoutParams) {
            (cardDescription.layoutParams as LinearLayout.LayoutParams).setMargins(0, 10, 0, 20)
        }
        cardDescription.visibility = View.GONE
        //endregion

        //region Add the date and time controls to the LinearLayout
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
            val cardIntent = Intent(this, ReservationDetailsActivity::class.java)
            cardIntent.putExtra("reservationId", ID)
            cardIntent.putExtra("source", "home")
            cardIntent.putExtra("fullName", fullName)
            cardIntent.putExtra("currentUser", currentUser)
            cardIntent.putExtra("company", company)
            cardIntent.putExtra("userType", userType)
            println(ID)
            startActivity(cardIntent)
        }
        //endregion
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
                    .whereGreaterThanOrEqualTo("title", searchWord)
                    .whereLessThanOrEqualTo("title", endValue)
                    .whereEqualTo("status", "coming")

            val nameSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereGreaterThanOrEqualTo("name", searchWord)
                    .whereLessThanOrEqualTo("name", endValue)
                    .whereEqualTo("status", "coming")

            val dateSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereGreaterThanOrEqualTo("date", searchWord)
                    .whereLessThanOrEqualTo("date", endValue)
                    .whereEqualTo("status", "coming")

            val descSearch =
                colRef.whereEqualTo("user", currentUser)
                    .whereGreaterThanOrEqualTo("description", searchWord)
                    .whereLessThanOrEqualTo("description", endValue)
                    .whereEqualTo("status", "coming")
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
                        // clear existing card views
                        cardContainer.removeAllViews()
                        findViewById<TextView>(R.id.noResLbl).visibility = View.GONE
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
                        // clear existing card views
                        cardContainer.removeAllViews()
                        findViewById<TextView>(R.id.noResLbl).visibility = View.GONE
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
                        // clear existing card views
                        cardContainer.removeAllViews()
                        findViewById<TextView>(R.id.noResLbl).visibility = View.GONE
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
                        // clear existing card views
                        cardContainer.removeAllViews()
                        findViewById<TextView>(R.id.noResLbl).visibility = View.GONE
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
                        findViewById<TextView>(R.id.noResLbl).visibility = View.VISIBLE
                    }
                    //endregion
                }.addOnFailureListener { error ->
                    println("Error retrieving reservations $error (377-1)") // 377-1 = ERR = Error Retreive Reservations
                    Toast.makeText(
                        this,
                        "Error retrieving reservations (377-1)",
                        Toast.LENGTH_SHORT
                    ).show() // 377-1 = ERR = Error Retreive Reservations
                }
        } else {
            // clear existing card views
            cardContainer.removeAllViews()

            // hide no result label
            findViewById<TextView>(R.id.noResLbl).visibility = View.GONE

            // retrieve all reservations
            getReservations()
        }
    }

    fun createResClicked(view: View) {
        //set click event for the "no reservation" label
        val newReservationIntent = Intent(this, MakeReservationActivity::class.java)
        newReservationIntent.putExtra("source", "home")
        newReservationIntent.putExtra("HPfullname", fullName)
        newReservationIntent.putExtra("HPcompany", company)
        newReservationIntent.putExtra("HPcurrentUser", currentUser)
        newReservationIntent.putExtra("HPuserType", userType)
        startActivity(newReservationIntent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Show a dialog to confirm the exit
        AlertDialog.Builder(this)
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                // User confirmed, exit the application
                finishAffinity()
            }
            .setNegativeButton("No", null)
            .show()
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