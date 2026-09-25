package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String,
    val lastName: String,
    val dateOfBirth: Long, // Epoch millis
    val gender: String = "Male", // "Male", "Female", "Others"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun getHonorific(): String {
        return when (gender.trim().lowercase()) {
            "male" -> "Sir"
            "female" -> "Mam"
            else -> firstName.trim().ifEmpty { "there" }
        }
    }
}
