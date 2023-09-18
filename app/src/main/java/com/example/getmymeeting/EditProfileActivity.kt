package com.example.getmymeeting

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class EditProfileActivity : AppCompatActivity() {

    //region Declaring variables
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var fullnameTbox: EditText
    private lateinit var emailTbox: EditText
    private lateinit var companyTbox: EditText
    private lateinit var fullName: String
    private lateinit var company: String
    private lateinit var userType: String
    private lateinit var profilePicture: ImageView
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private var currentUser: String? = null
    private var isInternetConnected = true
    private var previousImageUri: Uri? = null
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initializing variables
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        db = FirebaseFirestore.getInstance()
        fullnameTbox = findViewById(R.id.tboxFullName)
        emailTbox = findViewById(R.id.tboxEmail)
        companyTbox = findViewById(R.id.tboxCompany)
        profilePicture = findViewById(R.id.uploadImage)
        trimmer = TrimTexts()
        getIntentValues()
        //endregion

        //region Getting user info
        //get a reference of the users' collection
        val colRef = db.collection("users")
        //create a query to get column reference for the current user
        val query = colRef.whereEqualTo("email", currentUser)
        //if the task succeed, do the following
        query.get().addOnSuccessListener { documents ->
            //loop around the documents that matches the rules specified above
            for (document in documents) {
                //declare a data map variable to store the values to use
                val data = document.data
                //region get the values of the specified field and convert it to string
                fullName = data["name"].toString()
                company = data["company"].toString()
                //endregion
            }
            //region set the controls' text to their matching values
            fullnameTbox.setText(fullName)
            emailTbox.setText(trimmer.trimText(currentUser.toString()).lowercase())
            companyTbox.setText(company)
            //endregion
        }
        val storageRef =
            FirebaseStorage.getInstance().reference.child("images/profile_$currentUser")
        //TODO: round the image
        val localFile = File.createTempFile("tempImage", "jpg")
        storageRef.getFile(localFile).addOnSuccessListener {
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            profilePicture.setImageBitmap(bitmap)
        }.addOnFailureListener {
            //Toast.makeText(this, "Failed to load the image", Toast.LENGTH_SHORT).show()
        }
        //endregion

        //region set the click event for the discard button
        findViewById<Button>(R.id.btnDiscard).setOnClickListener {
            //create an intent to profile
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra("fullname", trimmer.trimText(fullnameTbox.text.toString()))
            intent.putExtra("company", trimmer.trimText(companyTbox.text.toString()))
            intent.putExtra("userType", userType)
            intent.putExtra("currentUser", currentUser)
            //start the activity
            startActivity(intent)
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

        // Call super.onBackPressed() to allow the default back button behavior (e.g., finish the activity)
        super.onBackPressed()
    }

    private fun getIntentValues() {
        fullName = intent.getStringExtra("fullName").toString()
        currentUser = intent.getStringExtra("currentUser").toString()
        company = intent.getStringExtra("company").toString()
        userType = intent.getStringExtra("userType").toString()
    }

    fun selectImage(view: View) {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            val newImageUri = data?.data
            if (newImageUri != previousImageUri) {
                // a new image is selected
                previousImageUri = newImageUri
                profilePicture.setImageURI(newImageUri)
                // Perform any additional actions needed
            }
        }
    }

    fun onSaveButtonClick(view: View) {
        //region check if any of name or company boxes are empty
        if (trimmer.trimText(fullnameTbox.text.toString())
                .isEmpty() || trimmer.trimText(companyTbox.text.toString()).isEmpty()
        ) {
            Toast.makeText(this, "Please don't leave empty fields (683)", Toast.LENGTH_SHORT).show() // 683 = MTF = Empty Field
        }
        //endregion

        //region if none of the fields is empty do the following
        else {
            //region update info
            //get a reference to the "users" collection, the current user document
            val userRef = db.collection("users").document(currentUser.toString())
            //create a HashMap to store the values of the fields
            val data = HashMap<String, Any>()
            //set the new values of the "name" and "company" fields
            data["name"] = trimmer.trimText(fullnameTbox.text.toString())
            data["company"] = trimmer.trimText(companyTbox.text.toString())

            //region excute the update command
            userRef.update(data).addOnSuccessListener {
                //if the update succeed, do the following
                println("Data updated successfully. (387)") // 387 = DUS = Data Update Success
                Toast.makeText(this, "Information updated successfully (387)", Toast.LENGTH_SHORT).show() // 387 = DUS = Data Update Success

                //region Profile intent
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("name", trimmer.trimText(fullnameTbox.text.toString()))
                intent.putExtra("email", trimmer.trimText(emailTbox.text.toString()))
                intent.putExtra("company", trimmer.trimText(companyTbox.text.toString()))
                intent.putExtra("currentUser", currentUser)
                startActivity(intent)
                //endregion
            }.addOnFailureListener { e ->
                //if the update failed, do the following
                println("Error updating the data $e (383)") // 383 = EUD = Error Update Data
                Toast.makeText(this, "Error updating the data. (383)", Toast.LENGTH_SHORT).show() // 383 = EUD = Error Update Data
            }
            //endregion

            //region upload image
            if (previousImageUri != null) {
                // Create a ProgressDialog to show the upload progress
                val progressDialog = ProgressDialog(this)
                progressDialog.setMessage("Uploading...")
                progressDialog.setCancelable(false)
                progressDialog.show()

                // Generate a unique file name for the profile image
                val fileName = "profile_$currentUser".replace("@","_").replace(".", "_")
                println(fileName)

                // Get the Firebase Storage reference for uploading the image
                val storageRef = FirebaseStorage.getInstance().getReference("images/profile_${
                    currentUser.toString().replace("@", "_").replace(".", "_")
                }.jpg")

                // Upload the image file to Firebase Storage
                storageRef.putFile(previousImageUri!!)
                    .addOnSuccessListener {
                        // Clear the image view to remove the previous image
                        profilePicture.setImageURI(null)

                        // Show a success message to the user
                        Toast.makeText(this, "Upload success (877)", Toast.LENGTH_SHORT).show() // 877 = UPS = UPload Success

                        // Dismiss the progress dialog
                        if (progressDialog.isShowing) progressDialog.dismiss()
                    }
                    .addOnFailureListener {
                        // Show an error message to the user
                        Toast.makeText(this, "Upload failed (873)", Toast.LENGTH_SHORT).show() // 873 = UPF = UPload Failed

                        // Dismiss the progress dialog
                        if (progressDialog.isShowing) progressDialog.dismiss()
                    }
            }
            //endregion
        }
        //endregion
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