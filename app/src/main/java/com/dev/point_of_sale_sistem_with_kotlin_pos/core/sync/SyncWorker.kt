package com.dev.point_of_sale_sistem_with_kotlin_pos.core.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.SupabaseClientProvider
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {

            val db = OfflineDatabase.getInstance(applicationContext)

            val supabase = SupabaseClientProvider.client

            val salesRepo = SalesRepository(supabase)
            val cashRepo = CashRegisterRepository(supabase)

            val syncManager = SyncManager(
                offlineDb = db,
                salesRepository = salesRepo,
                cashRegisterRepository = cashRepo
            )

            syncManager.syncAll()

            Result.success()

        } catch (e: Exception) {
            Result.retry()
        }
    }
}