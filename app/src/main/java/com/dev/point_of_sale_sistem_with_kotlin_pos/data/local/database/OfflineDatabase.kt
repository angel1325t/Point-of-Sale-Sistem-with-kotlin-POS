package com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao.OfflineCashRegisterDao
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao.OfflineProductCacheDao
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao.OfflineSalesDao
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao.OfflineStockDao
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao.SyncQueueDao
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.dao.BranchCacheDao
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineCashRegisterHistoryEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineProductCacheEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineProductStockEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleDetailEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineSaleEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.OfflineBranchEntity
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.entities.SyncQueueEntity

@Database(
    entities = [
        OfflineCashRegisterHistoryEntity::class,
        OfflineSaleEntity::class,
        OfflineSaleDetailEntity::class,
        OfflineProductCacheEntity::class,
        OfflineProductStockEntity::class,
        OfflineBranchEntity::class,
        SyncQueueEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class OfflineDatabase : RoomDatabase() {

    abstract fun cashRegisterDao(): OfflineCashRegisterDao
    abstract fun salesDao(): OfflineSalesDao
    abstract fun productCacheDao(): OfflineProductCacheDao
    abstract fun stockDao(): OfflineStockDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun branchCacheDao(): BranchCacheDao

    companion object {
        @Volatile
        private var INSTANCE: OfflineDatabase? = null

        fun getInstance(context: Context): OfflineDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfflineDatabase::class.java,
                    "offline_pos_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
