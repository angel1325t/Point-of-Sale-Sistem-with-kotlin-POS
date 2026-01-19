package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineBranchEntity

@Dao
interface BranchCacheDao {

    @Query("SELECT * FROM offline_branches_cache WHERE companyId = :companyId ORDER BY createdAt DESC")
    suspend fun getBranchesByCompany(companyId: String): List<OfflineBranchEntity>

    @Query("SELECT * FROM offline_branches_cache WHERE branchId = :branchId LIMIT 1")
    suspend fun getById(branchId: String): OfflineBranchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(branch: OfflineBranchEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(branches: List<OfflineBranchEntity>)

    @Query("DELETE FROM offline_branches_cache WHERE companyId = :companyId")
    suspend fun deleteByCompany(companyId: String)

    @Query("DELETE FROM offline_branches_cache")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM offline_branches_cache WHERE companyId = :companyId")
    suspend fun getBranchCountByCompany(companyId: String): Int
}
