package com.example.getmymeeting

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignupActivity : AppCompatActivity() {

    //region declare variables
    private lateinit var nameTxt: EditText
    private lateinit var emailTxt: EditText
    private lateinit var companyTxt: EditText
    private lateinit var passTxt: EditText
    private lateinit var loginLbl: TextView
    private lateinit var termsLbl: TextView
    private lateinit var signupBtn: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var databaseRef: DatabaseReference
    private lateinit var db: FirebaseFirestore
    private lateinit var trimmer: TrimTexts
    private lateinit var networkConnectivityListener: NetworkConnectivityListener
    private var isInternetConnected = true
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Create a network connectivity listener to check internet connectivity
        networkConnectivityListener = NetworkConnectivityListener(this) { isConnected ->
            isInternetConnected = isConnected
            runOnUiThread { handleControlsState() }
        }

        //region initialize variables
        nameTxt = findViewById(R.id.tboxName)
        emailTxt = findViewById(R.id.tboxEmail)
        companyTxt = findViewById(R.id.tboxCompany)
        passTxt = findViewById(R.id.tboxPass)
        loginLbl = findViewById(R.id.lblLogin)
        termsLbl = findViewById(R.id.lblTerms)
        signupBtn = findViewById(R.id.btnRegister)
        firebaseAuth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().reference
        db = Firebase.firestore
        trimmer = TrimTexts()
        //endregion

        loginLbl.setOnClickListener { startActivity(Intent(this, LoginActivity::class.java)) }
        termsLbl.setOnClickListener { startActivity(Intent(this, TermsOfUseActivity::class.java)) }
    }

    private fun isValidEmail(email: String): Boolean {
        val pattern = Regex("^([\\w\\.\\-]+)@([\\w\\-]+)((\\.(\\w){2,3})+)$")
        return pattern.matches(email)
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

    fun onSignupClicked(view: View) {
        if (trimmer.trimText(nameTxt.text.toString()).none { !it.isWhitespace() }) {
            Toast.makeText(this, "Please enter your full name", Toast.LENGTH_SHORT).show()
            nameTxt.requestFocus()
        } else if (trimmer.trimText(emailTxt.text.toString()).none { !it.isWhitespace() }) {
            Toast.makeText(this, "Please enter your email address", Toast.LENGTH_SHORT).show()
            emailTxt.requestFocus()
        } else if (trimmer.trimText(companyTxt.text.toString()).none { !it.isWhitespace() }) {
            Toast.makeText(this, "Please enter your company name", Toast.LENGTH_SHORT).show()
            companyTxt.requestFocus()
        } else if (trimmer.trimPass(passTxt.text.toString()).none { !it.isWhitespace() }) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
            passTxt.requestFocus()
        } else {
            if (isValidEmail(trimmer.trimText(emailTxt.text.toString()))) {
                if (trimmer.trimPass(passTxt.text.toString()).length < 6) {
                    Toast.makeText(
                        this,
                        "Password must be at least 6 characters length",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    firebaseAuth.createUserWithEmailAndPassword(
                        trimmer.trimText(emailTxt.text.toString()),
                        trimmer.trimPass(passTxt.text.toString())
                    ).addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Welcome ${nameTxt.text}!", Toast.LENGTH_SHORT)
                                .show()

                            val data = hashMapOf(
                                "name" to trimmer.trimText(nameTxt.text.toString()).lowercase(),
                                "email" to trimmer.trimText(emailTxt.text.toString())
                                    .lowercase(),
                                "company" to trimmer.trimText(companyTxt.text.toString())
                                    .lowercase(),
                                "userType" to "user"
                            )
                            val colRef = db.collection("users")
                            val docRef = colRef.document(
                                trimmer.trimText(emailTxt.text.toString()).lowercase()
                            )
                            docRef.set(data)
                                .addOnSuccessListener {
                                    println("Data Inserted Success")
                                }
                                .addOnFailureListener {
                                    println("Error occurred while saving data!")
                                }
                            val homeIntent = Intent(this, HomeActivity::class.java)
                            homeIntent.putExtra(
                                "currentUser",
                                trimmer.trimText(emailTxt.text.toString())
                            )
                            startActivity(homeIntent)
                        } else {
                            println("createUserWithEmail:failure ${task.exception}")
                            Toast.makeText(
                                baseContext,
                                "This email is already registered.",
                                Toast.LENGTH_SHORT,
                            ).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter valid email address", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }
}