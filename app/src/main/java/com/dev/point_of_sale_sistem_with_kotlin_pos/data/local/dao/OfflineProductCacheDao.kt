package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineProductCacheEntity

@Dao
interface OfflineProductCacheDao {

    @Query("SELECT * FROM offline_products_cache WHERE branchId = :branchId AND name LIKE '%' || :query || '%'")
    suspend fun searchByName(branchId: String, query: String): List<OfflineProductCacheEntity>

    @Query("SELECT * FROM offline_products_cache WHERE branchId = :branchId AND barcode = :barcode LIMIT 1")
    suspend fun getByBarcode(branchId: String, barcode: String): OfflineProductCacheEntity?

    @Query("SELECT * FROM offline_products_cache WHERE productId = :productId LIMIT 1")
    suspend fun getById(productId: Int): OfflineProductCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: OfflineProductCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<OfflineProductCacheEntity>)

    @Query("UPDATE offline_products_cache SET currentStock = currentStock - :quantity WHERE productId = :productId")
    suspend fun reduceStock(productId: Int, quantity: Int)

    @Query("DELETE FROM offline_products_cache WHERE branchId = :branchId")
    suspend fun deleteByBranch(branchId: String)

    @Query("SELECT COUNT(*) FROM offline_products_cache WHERE branchId = :branchId")
    suspend fun getProductCountByBranch(branchId: String): Int
}
