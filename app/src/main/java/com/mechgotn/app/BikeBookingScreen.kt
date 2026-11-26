
package com.mechgotn.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun BikeBookingScreen(
    onBack: () -> Unit,
    onConfirm: suspend (BikeBooking) -> Unit
) {
    var serviceMode by remember { mutableStateOf("Workshop") }
    var brand by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var serviceType by remember { mutableStateOf("General Service") }
    var customerName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var bookingDate by remember { mutableStateOf("") }
    var bookingTime by remember { mutableStateOf("") }

    var confirmed by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val navy = Color(0xFF10243A)
    val orange = Color(0xFFFF8A00)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        TextButton(onClick = onBack) {
            Text("← Back to Home", color = orange)
        }

        Text(
            "🏍️ Bike Service",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold,
            color = navy
        )

        Text(
            "Book your bike service with MECHGO TN",
            color = Color.Gray
        )

        Spacer(Modifier.height(22.dp))

        Text("1. Choose Service Location", fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = serviceMode == "Workshop",
                onClick = { serviceMode = "Workshop" }
            )
            Text("🏢 Workshop")
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = serviceMode == "Home Service",
                onClick = { serviceMode = "Home Service" }
            )
            Text("🏠 Home Service")
        }

        HorizontalDivider()
        Spacer(Modifier.height(15.dp))

        Text("2. Bike Details", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = brand,
            onValueChange = { brand = it },
            label = { Text("Bike Brand (TVS, Honda, etc.)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = model,
            onValueChange = { model = it },
            label = { Text("Bike Model") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(15.dp))

        Text("3. Select Service", fontWeight = FontWeight.Bold)

        listOf(
            "General Service",
            "Oil Change",
            "Engine Repair",
            "Brake Repair",
            "Tyre Service",
            "Battery Service",
            "Other Repair"
        ).forEach { option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = serviceType == option,
                    onClick = { serviceType = option }
                )
                Text(option)
            }
        }

        HorizontalDivider()
        Spacer(Modifier.height(15.dp))

        Text("4. Customer Details", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = customerName,
            onValueChange = { customerName = it },
            label = { Text("Customer Name") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Mobile Number") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = {
                Text(
                    if (serviceMode == "Home Service")
                        "Home Address"
                    else
                        "Preferred Workshop Location"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(15.dp))

        Text("5. Booking Schedule", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = bookingDate,
            onValueChange = { bookingDate = it },
            label = { Text("Date (DD/MM/YYYY)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = bookingTime,
            onValueChange = { bookingTime = it },
            label = { Text("Preferred Time") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = {
                scope.launch {
                    saving = true
                    errorMessage = ""

                    try {
                        val booking = BikeBooking(
                            serviceMode = serviceMode,
                            brand = brand,
                            model = model,
                            serviceType = serviceType,
                            customerName = customerName,
                            phone = phone,
                            address = address,
                            bookingDate = bookingDate,
                            bookingTime = bookingTime
                        )

                        onConfirm(booking)
                        confirmed = true
                    } catch (e: Exception) {
                        errorMessage = "Unable to save booking. Please try again."
                    } finally {
                        saving = false
                    }
                }
            },
            enabled = !confirmed &&
                    !saving &&
                    brand.isNotBlank() &&
                    model.isNotBlank() &&
                    customerName.isNotBlank() &&
                    phone.length == 10 &&
                    address.isNotBlank() &&
                    bookingDate.isNotBlank() &&
                    bookingTime.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = orange
            )
        ) {
            Text(
                if (saving) "SAVING BOOKING..." else "CONFIRM BOOKING",
                color = Color.White
            )
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage, color = Color.Red)
        }

        if (confirmed) {
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE5F5E9)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Booking saved successfully!",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF167A35)
                    )

                    Text("Service: $serviceType")
                    Text("Mode: $serviceMode")
                    Text("Bike: $brand $model")
                    Text("Customer: $customerName")
                    Text("Phone: $phone")
                    Text("Address: $address")
                    Text("Date: $bookingDate")
                    Text("Time: $bookingTime")

                    Spacer(Modifier.height(8.dp))
                    Text("Your booking is saved on this device.")
                    Text("Open My Bookings to view it.")
                }
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}
