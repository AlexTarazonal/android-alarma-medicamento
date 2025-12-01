package com.tfinal.proyectofinal

import androidx.room.*

@Dao
interface MedItemDao {

    @Query("SELECT * FROM medicamentos")
    fun getAll(): List<MedItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(item: MedItemEntity)

    @Update
    fun update(item: MedItemEntity)

    @Query("SELECT * FROM medicamentos WHERE status != 'Pendiente' ORDER BY id DESC")
    fun getHistory(): List<MedItemEntity>

    @Query("SELECT * FROM medicamentos WHERE status = 'Pendiente' ORDER BY id DESC")
    fun getActive(): List<MedItemEntity>

    @Delete
    fun delete(item: MedItemEntity)

    @Query("SELECT * FROM medicamentos WHERE id = :id LIMIT 1")
    fun getById(id: Int): MedItemEntity?
}
