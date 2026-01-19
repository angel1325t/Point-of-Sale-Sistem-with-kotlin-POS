package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineCashRegisterHistoryEntity
@Dao
interface OfflineCashRegisterDao {

    @Query("SELECT * FROM offline_cash_register_history WHERE isOpen = 1 LIMIT 1")
    suspend fun getOpenCashRegister(): OfflineCashRegisterHistoryEntity?

    @Insert
    suspend fun insert(history: OfflineCashRegisterHistoryEntity)

    @Update
    suspend fun update(history: OfflineCashRegisterHistoryEntity)

    @Query("SELECT * FROM offline_cash_register_history WHERE localHistoryId = :localHistoryId")
    suspend fun getById(localHistoryId: String): OfflineCashRegisterHistoryEntity?
}

