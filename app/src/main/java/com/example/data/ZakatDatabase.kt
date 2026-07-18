package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val sender: String, // "user" or "bot"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "zakat_calculations")
data class ZakatCalculation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val zakatType: String, // "Fitrah", "Penghasilan", "Emas", "Perdagangan"
    val totalWealth: Double,
    val calculatedZakat: Double,
    val description: String, // Detail of inputs
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ZakatDao {
    // Chat Queries
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatHistory()

    // Calculation Queries
    @Query("SELECT * FROM zakat_calculations ORDER BY timestamp DESC")
    fun getAllCalculations(): Flow<List<ZakatCalculation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalculation(calculation: ZakatCalculation)

    @Query("DELETE FROM zakat_calculations WHERE id = :id")
    suspend fun deleteCalculationById(id: Long)
}

@Database(entities = [ChatMessage::class, ZakatCalculation::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun zakatDao(): ZakatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zavira_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
