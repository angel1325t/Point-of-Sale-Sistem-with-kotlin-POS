package com.dev.point_of_sale_sistem_with_kotlin_pos.core.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dev.point_of_sale_sistem_with_kotlin_pos.core.network.SupabaseClientProvider
import com.dev.point_of_sale_sistem_with_kotlin_pos.data.local.database.OfflineDatabase
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products.ProductRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.cash_register.CashRegisterRepository
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.sales.sales_orders.SalesRepository

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "SyncWorker started")
        return try {
            val db = OfflineDatabase.getInstance(applicationContext)
            val supabase = SupabaseClientProvider.client
            val sessionPreferences = SessionPreferences(applicationContext)

            val salesRepo = SalesRepository(supabase,sessionPreferences)
            val cashRepo = CashRegisterRepository(supabase)
            val productRepo = ProductRepository(supabase, sessionPreferences)

            val syncManager = SyncManager(
                offlineDb = db,
                salesRepository = salesRepo,
                cashRegisterRepository = cashRepo,
                productRepository = productRepo,
                sessionPreferences = sessionPreferences
            )

            Log.d(TAG, "Calling syncManager.syncAll()")
            syncManager.syncAll()
            Log.d(TAG, "SyncWorker completed successfully")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed", e)
            Result.retry()
        }
    }
}
