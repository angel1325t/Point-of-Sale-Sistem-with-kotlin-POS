package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineProductStockEntity

@Dao
interface OfflineStockDao {

    @Query("SELECT currentStock FROM offline_product_stock WHERE productId = :id")
    suspend fun getStock(id: Int): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(stock: OfflineProductStockEntity)

    @Query("UPDATE offline_product_stock SET currentStock = currentStock - :qty WHERE productId = :id")
    suspend fun reduceStock(id: Int, qty: Int)
}
