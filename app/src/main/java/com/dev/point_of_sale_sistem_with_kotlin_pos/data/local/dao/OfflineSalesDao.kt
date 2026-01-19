package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleDetailEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.relations.OfflineSaleWithDetails

@Dao
interface OfflineSalesDao {

    @Insert
    suspend fun insertSale(sale: OfflineSaleEntity)

    @Insert
    suspend fun insertDetails(details: List<OfflineSaleDetailEntity>)

    @Transaction
    @Query("SELECT * FROM offline_sales WHERE localSaleId = :localSaleId LIMIT 1")
    suspend fun getSaleById(localSaleId: String): OfflineSaleEntity?

    @Query("SELECT * FROM offline_sale_details WHERE localSaleId = :localSaleId")
    suspend fun getDetailsBySaleId(localSaleId: String): List<OfflineSaleDetailEntity>

    @Transaction
    @Query("SELECT * FROM offline_sales WHERE pendingSync = 1")
    suspend fun getPendingSalesWithDetails(): List<OfflineSaleWithDetails>

    @Query("UPDATE offline_sales SET pendingSync = 0 WHERE localSaleId = :localSaleId")
    suspend fun markAsSynced(localSaleId: String)

    @Query("SELECT COUNT(*) FROM offline_sales WHERE pendingSync = 1")
    suspend fun getPendingSalesCount(): Int

    @Query("SELECT * FROM offline_sales WHERE cashRegisterHistoryId = :historyId LIMIT 1")
    suspend fun getSaleByHistoryId(historyId: String): OfflineSaleEntity?

    @Query("SELECT * FROM offline_sales WHERE cashRegisterHistoryId = :historyId")
    suspend fun getSalesByHistoryId(historyId: String): List<OfflineSaleEntity>

    @Query("UPDATE offline_sales SET cashRegisterHistoryId = :newHistoryId WHERE localSaleId = :localSaleId")
    suspend fun updateCashRegisterHistoryId(localSaleId: String, newHistoryId: String)
}