package com.example.getmymeeting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class AddRoomActivity : AppCompatActivity() {
    //region Creating variables
    private var currentUser: String? = null
    private var isInternetConnected = true
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var spinnerLocation: Spinner
    private lateinit var nameTbox: EditText
    private lateinit var seatCountTbox: EditText
    private lateinit var selectedLocation: String
    private lateinit var roomColRef: CollectionReference
    private lateinit var db: FirebaseFirestore
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_room)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region user variables
        fullName = ""
        company = ""
        userType = ""
        trimmer = TrimTexts()
        setIntentValues()
        //endregion

        //region Checking the user type
        //checking whether the user is an admin or not
        if (userType != "admin") {
            // if user is not an admin, show unauthorized message and redirect to home page
            println("Add Room: User not authorized! Error Code: (628)")
            Toast.makeText(
                this,
                "You are not authorized to enter this page! (628)",
                Toast.LENGTH_SHORT
            )
                .show() // 628 = NAT = Not Authorized
            startActivity(Intent(this, HomeActivity::class.java))
        }
        //endregion

        //region Setting location spinner items
        spinnerLocation = findViewById(R.id.spnrLocation)
        //setting the items source
        val roomItems = resources.getStringArray(R.array.location_spinner_items)
        //setting the selected index
        selectedLocation = roomItems[0]
        //creating adapter to insert items to spinner
        val adapter = ArrayAdapter(
            this, //context
            R.layout.spinner_item, //setting the resource
            roomItems //choosing the spinner objects
        )
        //setting the dropdown menu items layout
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        //applying the adapter to the spinner
        spinnerLocation.adapter = adapter
        //endregion

        //region Initializing variables
        nameTbox = findViewById(R.id.tboxName)
        seatCountTbox = findViewById(R.id.tboxSeatCount)
        db = FirebaseFirestore.getInstance()
        roomColRef = db.collection("rooms")
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
        //region Getting user details
        currentUser = intent.getStringExtra("currentUser")
        fullName = intent.getStringExtra("fullname").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
        //endregion
    }

    fun onSaveRoomClicked(view: View) {
        //checking for empty fields after clearing the whitespaces
        when {
            trimmer.trimText(nameTbox.text.toString()).none { !it.isWhitespace() } -> {
                Toast.makeText(this, "Please enter the room name. (683)", Toast.LENGTH_SHORT).show() // 683 = MTF = Empty Field
                nameTbox.requestFocus()
            }
            selectedLocation.isEmpty() -> {
                Toast.makeText(this, "Please choose the location of the room. (683)", Toast.LENGTH_SHORT) // 683 = MTF = Empty Field
                    .show()
                spinnerLocation.requestFocus()
            }
            trimmer.trimText(seatCountTbox.text.toString()).none { !it.isWhitespace() } -> {
                Toast.makeText(this, "Please enter the seat count of the room. (683)", Toast.LENGTH_SHORT) // 683 = MTF = Empty Field
                    .show()
                seatCountTbox.requestFocus()
            }
            else -> {
                //if none of the fields is empty, do the following

                //region Creating room document
                //if task succeed, do the following
                roomColRef.get().addOnSuccessListener { querySnapshot ->
                    //get the document size/count
                    val docCount = querySnapshot.size()

                    //creating room ID
                    val roomID = "room${docCount.plus(1)}"
                    //creating a hash map to get and set the fields with their values
                    val room = HashMap<String, Any>()
                    room["room_name"] = trimmer.trimText(nameTbox.text.toString())
                    room["location"] = selectedLocation
                    room["seat_count"] = trimmer.trimText(seatCountTbox.text.toString())
                    //the following line adds a new document if the given roomID doesn't exist
                    //if the roomID exists, it overwrites it with the new values
                    val roomsColRef = roomColRef.document(roomID)

                    //if the task succeed, do the following
                    roomsColRef.set(room).addOnSuccessListener {
                        //display success message and redirect to profile activity by calling its function
                        println("Add Room: Room add success. (727)") // 727 = RAS = Room Add Success
                        Toast.makeText(
                            this,
                            "Room has been added successfully! (727)", // 727 = RAS = Room Add Success
                            Toast.LENGTH_SHORT
                        ).show()
                        startProfileActivity()
                    }
                        //if the task failed, do the following
                        .addOnFailureListener { error ->
                            //display error message with the error code
                            println("Add Room: Error creating document: $error (323)") // 323 = ECD = Error Creating Document
                            Toast.makeText(
                                this,
                                "Error saving room: ${error.message} (323)", // 323 = ECD = Error Creating Document
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
                    //if task failed, do the following
                    .addOnFailureListener { e ->
                        //display error message with the error code.
                        println("Add Room: Error getting document count: $e (332)") // 332 = EDC = Error Document Count
                        Toast.makeText(
                            this,
                            "Error getting document count: ${e.message} (332)", // 332 = EDC = Error Document Count
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
        }
    }

    private fun startProfileActivity() {
        //create intent
        val intentSaveRoom = Intent(this, ProfileActivity::class.java)
        intentSaveRoom.putExtra("currentUser", currentUser)
        intentSaveRoom.putExtra("fullname", intent.getStringExtra("fullname"))
        intentSaveRoom.putExtra("company", intent.getStringExtra("company"))
        intentSaveRoom.putExtra("userType", intent.getStringExtra("userType"))
        //start the intent activity
        startActivity(intentSaveRoom)
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
        Snackbar.make(rootView, "Internet connection lost (425)", Snackbar.LENGTH_LONG).show() // 425 = ICL = Error Connection Lost
    }
    //endregion
}