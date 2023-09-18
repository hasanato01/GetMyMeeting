package com.example.getmymeeting

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    //region Declaring variables
    private lateinit var emailTxt: EditText
    private lateinit var passTxt: EditText
    private lateinit var loginBtn: Button
    private lateinit var register: TextView
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var usersColRef: CollectionReference
    private var isInternetConnected = true
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initialize variables
        emailTxt = findViewById(R.id.tboxEmail)
        passTxt = findViewById(R.id.tboxPass)
        loginBtn = findViewById(R.id.btnLogin)
        register = findViewById(R.id.lblSignup)
        firebaseAuth = FirebaseAuth.getInstance()
        usersColRef = FirebaseFirestore.getInstance().collection("users")
        trimmer = TrimTexts()
        //endregion

        //region Restore saved user data from the Shared Preferences
        sharedPreferences = getSharedPreferences("userData", Context.MODE_PRIVATE)
        val userEmail = sharedPreferences.getString("user", "")
        val userPass = sharedPreferences.getString("pass", "")
        emailTxt.setText(userEmail)
        passTxt.setText(userPass)
        //endregion

        // Register label click listener to start signup activity
        register.setOnClickListener { startActivity(Intent(this, SignupActivity::class.java)) }
    }

    override fun onBackPressed() {
        // Exit the application
        finishAffinity()
    }

    private fun isValidEmail(email: String): Boolean {
        val pattern = Regex("^([\\w\\.\\-]+)@([\\w\\-]+)((\\.(\\w){2,3})+)$")
        return pattern.matches(email)
    }

    fun onLoginClick(view: View) {
        // Check if the email field is empty
        if (trimmer.trimText(emailTxt.text.toString()).none { !it.isWhitespace() }) {
            Toast.makeText(this, "Please enter your email address (683)", Toast.LENGTH_SHORT).show()
            emailTxt.requestFocus()
        }
        // Check if the password field is empty
        else if (trimmer.trimPass(passTxt.text.toString()).none { !it.isWhitespace() }) {
            Toast.makeText(this, "Please enter your password (683)", Toast.LENGTH_SHORT).show()
            passTxt.requestFocus()
        } else {
            // Validate the email address format
            if (isValidEmail(trimmer.trimText(emailTxt.text.toString()))) {
                val email = trimmer.trimText(emailTxt.text.toString())
                val password = trimmer.trimPass(passTxt.text.toString())

                // Check if the email exists in the "users" collection
                val query = FirebaseFirestore.getInstance()
                    .collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)

                query.get().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents = task.result?.documents
                        if (documents != null && documents.isNotEmpty()) {
                            // Email exists, proceed with sign in
                            signInUser(email, password)
                        } else {
                            // Email does not exist
                            Toast.makeText(this, "Email does not exist (367)", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        // Firestore query failed
                        Toast.makeText(
                            this,
                            "Failed to check email existence (233)",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            } else {
                // Invalid email address format
                Toast.makeText(this, "Please enter a valid email address (432)", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun signInUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in successful
                    Toast.makeText(this, "Welcome back", Toast.LENGTH_SHORT).show()

                    // Save the user's email and password in shared preferences
                    val editor = sharedPreferences.edit()
                    editor.putString("user", email)
                    editor.putString("pass", password)
                    editor.apply()

                    // Start the home activity and pass the current user email
                    val homeIntent = Intent(this, HomeActivity::class.java)
                    homeIntent.putExtra("currentUser", email)
                    startActivity(homeIntent)
                } else {
                    // Sign in failed, show error message
                    println("signInWithEmail:failure ${task.exception?.message}")
                    Toast.makeText(baseContext, "Incorrect password (427)", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    fun onResetPassClick(view: View) {
        // Create an AlertDialog Builder
        val builder = AlertDialog.Builder(this)

        // Set the dialog title and prompt message
        builder.setTitle("Reset Password")
        builder.setMessage("Please enter your email address:")

        // Create an EditText view to receive user input
        val inputEditText = EditText(this)
        builder.setView(inputEditText)

        // Set positive button action
        builder.setPositiveButton("Reset") { dialog, _ ->
            val email = inputEditText.text.toString()
            if (trimmer.trimText(email).isEmpty()) {
                Toast.makeText(this, "Please enter your email address. (683)", Toast.LENGTH_SHORT)
                    .show()
                inputEditText.requestFocus()
            } else {
                if (isValidEmail(email)) {
                    firebaseAuth.sendPasswordResetEmail(email).addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Password reset email sent successfully. (777)",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        dialog.dismiss() // Dismiss the dialog
                    }.addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            "Please double check the email address and try again later. (432)",
                            Toast.LENGTH_SHORT
                        ).show()
                        println(error.message)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Please enter a valid email address (432)",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }

        // Set negative button action
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel() // Cancel the dialog
        }

        // Create and show the AlertDialog
        val dialog = builder.create()
        dialog.show()
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