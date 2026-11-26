package com.mechgotn.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.tasks.await

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { MechgoApp() } }
    }
}

@Composable
fun MechgoApp() {
    val context = LocalContext.current
    val auth = remember { com.google.firebase.auth.FirebaseAuth.getInstance() }
    val cloud = remember { com.google.firebase.firestore.FirebaseFirestore.getInstance() }
    var signedIn by remember { mutableStateOf(auth.currentUser != null) }
    var currentPage by remember { mutableStateOf("Home") }
    var isAdmin by remember { mutableStateOf(false) }
    var cloudBookings by remember { mutableStateOf<List<CloudBooking>>(emptyList()) }
    var cloudBookingsLoaded by remember { mutableStateOf(false) }
    var cloudError by remember { mutableStateOf("") }

    val database = remember(context) { BookingDatabase.getDatabase(context) }
    val bookingDao = remember(database) { database.bookingDao() }
    val localBookings by bookingDao.getAllBookings().collectAsState(initial = emptyList())
    val uid = auth.currentUser?.uid

    LaunchedEffect(signedIn, uid) {
        isAdmin = false
        if (signedIn && uid != null) {
            try {
                val profile = cloud.collection("users").document(uid).get().await()
                isAdmin = profile.getString("role") == "admin"
            } catch (_: Exception) {
                isAdmin = false
            }
        }
    }

    DisposableEffect(signedIn, uid) {
        if (signedIn && uid != null) {
            cloudBookingsLoaded = false
            val registration = cloud.collection("bookings")
                .whereEqualTo("ownerUid", uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        cloudError = error.localizedMessage ?: "Cloud sync failed."
                    } else {
                        cloudError = ""
                        cloudBookings = snapshot?.documents?.mapNotNull { it.toCloudBooking() } ?: emptyList()
                        cloudBookingsLoaded = true
                    }
                }
            onDispose { registration.remove() }
        } else {
            cloudBookings = emptyList()
            cloudBookingsLoaded = false
            onDispose { }
        }
    }

    if (!signedIn) {
        AuthScreen(onAuthenticated = {
            signedIn = true
            currentPage = "Home"
        })
        return
    }

    val services = listOf(
        "Bike Service" to "🏍️", "Car Service" to "🚗",
        "EV Service" to "⚡", "Breakdown Help" to "🆘",
        "Mechanic" to "🔧", "Towing" to "🚚",
        "Detailing" to "✨", "Spare Parts" to "⚙️"
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = currentPage == "Home", onClick = { currentPage = "Home" }, icon = { Text("🏠") }, label = { Text("Home") })
                NavigationBarItem(selected = currentPage == "My Bookings", onClick = { currentPage = "My Bookings" }, icon = { Text("📋") }, label = { Text("Bookings") })
                NavigationBarItem(selected = currentPage == "My Profile", onClick = { currentPage = "My Profile" }, icon = { Text("👤") }, label = { Text("Profile") })
                if (isAdmin) NavigationBarItem(selected = currentPage == "Admin", onClick = { currentPage = "Admin" }, icon = { Text("🛠️") }, label = { Text("Admin") })
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (currentPage) {
                "Home" -> HomeScreen(services = services, onSelect = { currentPage = it })
                "My Bookings" -> MyBookingsScreen(
                    bookings = if (cloudBookingsLoaded) cloudBookings else localBookings.map { booking ->
                        CloudBooking(
                            id = "local-${booking.id}",
                            ownerUid = uid.orEmpty(),
                            serviceMode = booking.serviceMode,
                            brand = booking.brand,
                            model = booking.model,
                            serviceType = booking.serviceType,
                            customerName = booking.customerName,
                            phone = booking.phone,
                            address = booking.address,
                            bookingDate = booking.bookingDate,
                            bookingTime = booking.bookingTime,
                            status = "Pending"
                        )
                    },
                    onBack = { currentPage = "Home" }
                )
                "My Profile" -> Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Text("My Profile", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text(auth.currentUser?.email ?: "")
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = {
                        auth.signOut()
                        signedIn = false
                        currentPage = "Home"
                    }) { Text("Logout") }
                }
                "Admin" -> if (isAdmin) AdminScreen(onBack = { currentPage = "Home" })
                           else SimplePage("Admin access denied", "🔒", "Your account is not an administrator.", { currentPage = "Home" })
                "Bike Service" -> BikeBookingScreen(
                    onBack = { currentPage = "Home" },
                    onConfirm = { booking ->
                        bookingDao.insertBooking(booking)
                        val currentUid = auth.currentUser?.uid ?: throw IllegalStateException("Please login again.")
                        cloud.collection("bookings").add(
                            mapOf(
                                "ownerUid" to currentUid,
                                "serviceMode" to booking.serviceMode,
                                "brand" to booking.brand,
                                "model" to booking.model,
                                "serviceType" to booking.serviceType,
                                "customerName" to booking.customerName,
                                "phone" to booking.phone,
                                "address" to booking.address,
                                "bookingDate" to booking.bookingDate,
                                "bookingTime" to booking.bookingTime,
                                "status" to "Pending",
                                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                )
                else -> ServiceScreen(serviceName = currentPage, onBack = { currentPage = "Home" })
            }
        }
    }
}

@Composable
fun HomeScreen(
    services: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .background(Color(0xFFF5F7FA)).padding(16.dp)
    ) {
        Text("MECHGO TN", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF123B70))
        Text("Your Vehicle. Our Responsibility.", fontSize = 14.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(22.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF123B70)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Need Emergency Help?", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Get vehicle assistance when you need it.", color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = { onSelect("Breakdown Help") }) { Text("Get Help Now") }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Our Services", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(14.dp))
        services.forEach { service ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                onClick = { onSelect(service.first) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(service.second, fontSize = 30.sp)
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(service.first, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Text("›", fontSize = 28.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun MyBookingsScreen(
    bookings: List<CloudBooking>,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))
            .verticalScroll(rememberScrollState()).padding(20.dp)
    ) {
        TextButton(onClick = onBack) { Text("← Back to Home") }
        Text("My Bookings", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Color(0xFF123B70))
        Spacer(Modifier.height(18.dp))
        if (bookings.isEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("📋", fontSize = 42.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("No bookings yet", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Your confirmed service bookings will appear here.", color = Color.Gray)
                }
            }
        } else {
            Text("Total Bookings: ${bookings.size}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(12.dp))
            bookings.forEachIndexed { index, booking ->
                val statusColor = when (booking.status) {
                    "Accepted" -> Color(0xFFE3F2FD)
                    "Completed" -> Color(0xFFE5F5E9)
                    else -> Color(0xFFFFF3E0)
                }
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Booking #${index + 1}", fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color(0xFF123B70))
                        Spacer(Modifier.height(10.dp))
                        Text("Vehicle: ${booking.brand} ${booking.model}")
                        Text("Service: ${booking.serviceType}")
                        Text("Service Mode: ${booking.serviceMode}")
                        Text("Customer: ${booking.customerName}")
                        Text("Mobile: ${booking.phone}")
                        Text("Address: ${booking.address}")
                        Text("Date: ${booking.bookingDate}")
                        Text("Time: ${booking.bookingTime}")
                        Spacer(Modifier.height(12.dp))
                        Surface(color = statusColor, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                "Booking Status: ${booking.status}",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF123B70)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimplePage(
    title: String,
    emoji: String,
    description: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)).padding(20.dp)
    ) {
        Button(onClick = onBack) { Text("← Back") }
        Spacer(modifier = Modifier.height(30.dp))
        Text(text = emoji, fontSize = 45.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = title, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = description, fontSize = 16.sp, color = Color.Gray)
    }
}

@Composable
fun ServiceScreen(
    serviceName: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA)).padding(20.dp)
    ) {
        Button(onClick = onBack) { Text("← Back") }
        Spacer(modifier = Modifier.height(30.dp))
        Text(text = serviceName, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text("This service section is coming soon.", fontSize = 16.sp, color = Color.Gray)
    }
}
