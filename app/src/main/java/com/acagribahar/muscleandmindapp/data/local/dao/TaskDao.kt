package com.acagribahar.muscleandmindapp.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.acagribahar.muscleandmindapp.data.local.entity.Task
import kotlinx.coroutines.flow.Flow // Flow'u import et

@Dao // Bu arayüzün bir Room DAO'su olduğunu belirtir
interface TaskDao {

    // Yeni bir görev ekler. Eğer aynı ID'ye sahip görev varsa üzerine yazar.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task) // suspend: Coroutine içinde çağrılmalı

    // Mevcut bir görevi günceller.
    @Update
    suspend fun updateTask(task: Task)

    // Bir görevi siler.
    @Delete
    suspend fun deleteTask(task: Task)

    // Belirli bir tarihe ait tüm görevleri getirir.
    // Flow<> kullanarak veri değişikliklerini anlık olarak takip edebiliriz.
    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY id ASC")
    fun getTasksForDate(date: Long): Flow<List<Task>>

    // Belirli bir ID'ye sahip tek bir görevi getirir.
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskById(id: Int): Flow<Task?> // Görev bulunamazsa null dönebilir

    // Belirli bir görevin tamamlanma durumunu günceller.
    // updateTask'tan daha verimli olabilir.
    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun updateCompletionStatus(id: Int, completed: Boolean)

    // (Opsiyonel) Tüm görevleri getiren sorgu (Debug için faydalı olabilir)
    @Query("SELECT * FROM tasks ORDER BY date DESC, id ASC")
    fun getAllTasks(): Flow<List<Task>>

    // (Opsiyonel) Belirli bir tarihten önceki görevleri silme
    // @Query("DELETE FROM tasks WHERE date < :date")
    // suspend fun deleteTasksBefore(date: Long)

    // Belirli bir tarihe ait görevleri Flow olmadan, direkt liste olarak getirir.
    @Query("SELECT * FROM tasks WHERE date = :date")
    suspend fun getTasksForDateSync(date: Long): List<Task>

    @Query("SELECT * FROM tasks WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getTasksBetweenDates(startDate: Long, endDate: Long): Flow<List<Task>>
}