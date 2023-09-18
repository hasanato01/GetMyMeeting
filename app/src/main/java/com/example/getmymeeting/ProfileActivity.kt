package com.example.getmymeeting

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class ProfileActivity : AppCompatActivity() {

    //region Declare variables
    private var currentUser: String? = null
    private var isInternetConnected = true
    private lateinit var bottomNavbar: BottomNavigationView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var profilePicture: ImageView
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initialize variables
        val reservationsBtn = findViewById<LinearLayout>(R.id.btnReservations)
        val addRoomBtn = findViewById<LinearLayout>(R.id.btnAddRoom)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        profilePicture = findViewById(R.id.profilePicture)
        fullName = ""
        company = ""
        userType = ""
        getIntentValues()
        setBottomNavbar()
        //endregion

        //region checking if the user is logged in, if it's not, redirect to Login
        if (firebaseAuth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        //endregion

        //region Getting user data and setting the shown elements
        val colRef = db.collection("users")
        val query = colRef.whereEqualTo("email", currentUser)
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val data = document.data
                fullName = data["name"].toString()
                company = data["company"].toString()
                userType = data["userType"].toString()
            }
            findViewById<TextView>(R.id.userName).text = fullName.uppercase()
            findViewById<TextView>(R.id.userEmail).text = currentUser.toString().lowercase()
            if (userType.lowercase() == "user") {
                reservationsBtn.visibility = View.GONE
                addRoomBtn.visibility = View.GONE
            }
        }
        //endregion

        //region set image
        val storageRef =
            FirebaseStorage.getInstance().reference.child(
                "images/profile_${
                    currentUser.toString().replace("@", "_").replace(".", "_")
                }.jpg"
            )
        //TODO: round the image
        val localFile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            profilePicture.setImageBitmap(bitmap)
        }.addOnFailureListener {
            profilePicture.setImageResource(R.drawable.profile)
        }
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

    private fun getIntentValues() {
        fullName = intent.getStringExtra("fullName").toString()
        currentUser = intent.getStringExtra("currentUser").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
    }

    //region Profile buttons
    fun onPastReservationClicked(view: View) {
        val pastReservationIntent = Intent(this, PastReservationsActivity::class.java)
        pastReservationIntent.putExtra("fullname", fullName)
        pastReservationIntent.putExtra("company", company)
        pastReservationIntent.putExtra("currentUser", currentUser)
        pastReservationIntent.putExtra("userType", userType)
        startActivity(pastReservationIntent)
    }

    fun onInviteClicked(view: View) {
        // TODO: make it in-app account link
        val inviteLink = "https://www.instagram.com/${fullName.replace(' ', '_', true)}".lowercase()
        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("label", inviteLink)
        clipboardManager.setPrimaryClip(clip)
        Toast.makeText(this, "Link copied to clipboard (527)", Toast.LENGTH_SHORT).show()
    }

    fun onEditProfileClicked(view: View) {
        val editProfileIntent = Intent(this, EditProfileActivity::class.java)
        editProfileIntent.putExtra("fullname", fullName)
        editProfileIntent.putExtra("company", company)
        editProfileIntent.putExtra("currentUser", currentUser)
        editProfileIntent.putExtra("userType", userType)
        startActivity(editProfileIntent)
    }

    fun onSettingsClicked(view: View) {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        settingsIntent.putExtra("fullname", fullName)
        settingsIntent.putExtra("company", company)
        settingsIntent.putExtra("currentUser", currentUser)
        settingsIntent.putExtra("userType", userType)
        startActivity(settingsIntent)
    }

    fun onReservationClicked(view: View) {
        val settingsIntent = Intent(this, ReservationsActivity::class.java)
        settingsIntent.putExtra("fullname", fullName)
        settingsIntent.putExtra("company", company)
        settingsIntent.putExtra("currentUser", currentUser)
        settingsIntent.putExtra("userType", userType)
        startActivity(settingsIntent)
    }

    fun onAddRoomClicked(view: View) {
        val settingsIntent = Intent(this, AddRoomActivity::class.java)
        settingsIntent.putExtra("fullname", fullName)
        settingsIntent.putExtra("company", company)
        settingsIntent.putExtra("currentUser", currentUser)
        settingsIntent.putExtra("userType", userType)
        startActivity(settingsIntent)
    }
    //endregion

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