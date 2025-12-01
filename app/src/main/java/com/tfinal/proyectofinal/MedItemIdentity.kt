package com.tfinal.proyectofinal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicamentos")
data class MedItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val dose: String,
    val desc: String,
    val days: Int,
    val hours: Int,
    var status: String = "Pendiente"
)
