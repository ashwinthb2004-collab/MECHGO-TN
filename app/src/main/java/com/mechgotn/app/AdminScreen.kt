package com.mechgotn.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun AdminScreen(onBack: () -> Unit) {
    val db = remember { FirebaseFirestore.getInstance() }
    var bookings by remember { mutableStateOf<List<CloudBooking>>(emptyList()) }
    var error by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val registration = db.collection("bookings").addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                error = exception.localizedMessage ?: "Could not load bookings."
            } else {
                bookings = snapshot?.documents?.mapNotNull { it.toCloudBooking() } ?: emptyList()
            }
        }
        onDispose { registration.remove() }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← Back") }
        Text("Admin - All Bookings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Total bookings: ${bookings.size}")
        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        if (bookings.isEmpty()) Text("No bookings available yet.", modifier = Modifier.padding(top = 20.dp))
        bookings.forEach { booking ->
            Card(Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("${booking.customerName} • ${booking.status}", fontWeight = FontWeight.Bold)
                    Text("Booking ID: ${booking.id}")
                    Text("Vehicle: ${booking.brand} ${booking.model}")
                    Text("Service: ${booking.serviceType} (${booking.serviceMode})")
                    Text("Phone: ${booking.phone}")
                    Text("Address: ${booking.address}")
                    Text("Date/time: ${booking.bookingDate} • ${booking.bookingTime}")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            db.collection("bookings").document(booking.id).update("status", "Accepted")
                        }) { Text("Accept") }
                        OutlinedButton(onClick = {
                            db.collection("bookings").document(booking.id).update("status", "Completed")
                        }) { Text("Complete") }
                    }
                }
            }
        }
    }
}
