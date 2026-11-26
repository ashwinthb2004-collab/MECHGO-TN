package com.mechgotn.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bike_bookings")
data class BikeBooking(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val serviceMode: String,
    val brand: String,
    val model: String,
    val serviceType: String,
    val customerName: String,
    val phone: String,
    val address: String,
    val bookingDate: String,
    val bookingTime: String
)

@Dao
interface BookingDao {

    @Insert
    suspend fun insertBooking(booking: BikeBooking)

    @Query("SELECT * FROM bike_bookings ORDER BY id DESC")
    fun getAllBookings(): Flow<List<BikeBooking>>
}

@Database(
    entities = [BikeBooking::class],
    version = 1,
    exportSchema = false
)
abstract class BookingDatabase : RoomDatabase() {

    abstract fun bookingDao(): BookingDao

    companion object {
        @Volatile
        private var INSTANCE: BookingDatabase? = null

        fun getDatabase(context: Context): BookingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BookingDatabase::class.java,
                    "mechgo_booking_database"
                ).build()

                INSTANCE = instance
                instance
            }
        }
    }
}