package com.tfinal.proyectofinal

import androidx.room.*

@Dao
interface MedItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: MedItemEntity)

    @Update
    fun update(item: MedItemEntity)

    @Delete
    fun delete(item: MedItemEntity)

    @Query("SELECT * FROM medicamentos WHERE userId = :userId AND status != 'Pendiente' ORDER BY id DESC")
    fun getHistory(userId: String): List<MedItemEntity>

    @Query("SELECT * FROM medicamentos WHERE userId = :userId AND status = 'Pendiente' ORDER BY id DESC")
    fun getActive(userId: String): List<MedItemEntity>

    @Query("SELECT * FROM medicamentos WHERE id = :id LIMIT 1")
    fun getById(id: Int): MedItemEntity?
}
