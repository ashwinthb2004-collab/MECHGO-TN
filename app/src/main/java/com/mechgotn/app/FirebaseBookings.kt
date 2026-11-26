package com.mechgotn.app

import com.google.firebase.firestore.DocumentSnapshot

data class CloudBooking(
    val id: String,
    val ownerUid: String,
    val serviceMode: String,
    val brand: String,
    val model: String,
    val serviceType: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val bookingDate: String,
    val bookingTime: String,
    val status: String
)

fun DocumentSnapshot.toCloudBooking(): CloudBooking? {
    val uid = getString("ownerUid") ?: return null
    return CloudBooking(
        id = id,
        ownerUid = uid,
        serviceMode = getString("serviceMode") ?: "",
        brand = getString("brand") ?: "",
        model = getString("model") ?: "",
        serviceType = getString("serviceType") ?: "",
        customerName = getString("customerName") ?: "",
        phone = getString("phone") ?: "",
        address = getString("address") ?: "",
        bookingDate = getString("bookingDate") ?: "",
        bookingTime = getString("bookingTime") ?: "",
        status = getString("status") ?: "Pending"
    )
}
