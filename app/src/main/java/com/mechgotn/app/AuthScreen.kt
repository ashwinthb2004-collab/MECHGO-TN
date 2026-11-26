package com.mechgotn.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AuthScreen(onAuthenticated: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var isRegister by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("MECHGO TN", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF123B70))
        Text(if (isRegister) "Create your customer account" else "Login to your account", color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        if (isRegister) {
            OutlinedTextField(name, { name = it }, label = { Text("Full name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(10.dp))
        }
        OutlinedTextField(email, { email = it.trim() }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password, { password = it }, label = { Text("Password (minimum 6 characters)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = {
                error = ""
                if (email.isBlank() || password.length < 6 || (isRegister && name.isBlank())) {
                    error = "Please enter valid details. Password must have at least 6 characters."
                } else {
                    busy = true
                    val task = if (isRegister) auth.createUserWithEmailAndPassword(email, password)
                               else auth.signInWithEmailAndPassword(email, password)
                    task.addOnCompleteListener { result ->
                        if (result.isSuccessful) {
                            val user = auth.currentUser
                            if (isRegister && user != null) {
                                db.collection("users").document(user.uid)
                                    .set(mapOf("name" to name.trim(), "email" to email, "createdAt" to System.currentTimeMillis()))
                                    .addOnCompleteListener { profile ->
                                        busy = false
                                        if (profile.isSuccessful) onAuthenticated()
                                        else error = profile.exception?.localizedMessage ?: "Could not save profile."
                                    }
                            } else {
                                busy = false
                                onAuthenticated()
                            }
                        } else {
                            busy = false
                            error = result.exception?.localizedMessage ?: "Authentication failed."
                        }
                    }
                }
            },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (busy) "PLEASE WAIT..." else if (isRegister) "CREATE ACCOUNT" else "LOGIN") }
        TextButton(onClick = { isRegister = !isRegister; error = "" }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isRegister) "Already have an account? Login" else "New customer? Create account")
        }
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
    }
}
