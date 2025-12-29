package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.set
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.*
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ProductRepository(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences,
) {

    companion object {
        private const val TAG = "ProductRepository"
    }

    // ====================== CREAR ======================
    suspend fun createProduct(
        name: String,
        description: String? = null,
        price: Double,
        categoryId: Int,
        currentStock: Int = 0,
        minimumStock: Int = 0,
        discountType: DiscountType = DiscountType.NONE,
        discountValue: Double = 0.0,
    ): Result<ProductDTO> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            val barcode = generateEAN13Barcode()
            val barcodeBitmap = generateBarcodeBitmap(barcode)
            val barcodeImageUrl = uploadBarcodeImage(barcode, barcodeBitmap)

            val insertData = ProductInsertDTO(
                name = name,
                description = description,
                price = price,
                barcode = barcode,
                categoryId = categoryId,
                branchId = branchId,
                image = barcodeImageUrl,
                currentStock = currentStock,
                minimumStock = minimumStock,
                discountType = discountType.name.lowercase(),
                discountValue = discountValue
            )

            val product = supabase.from("products")
                .insert(insertData) { select() }
                .decodeSingle<ProductDTO>()

            Result.success(product)

        } catch (e: Exception) {
            Log.e(TAG, "createProduct error", e)
            Result.failure(e)
        }
    }

    // ====================== UPDATE ======================
    suspend fun updateProduct(productId: Int, updates: ProductUpdateDTO): Result<ProductDTO> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            val product = supabase.from("products")
                .update(updates) {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                    select()
                }
                .decodeSingle<ProductDTO>()

            Result.success(product)

        } catch (e: Exception) {
            Log.e(TAG, "updateProduct error", e)
            Result.failure(e)
        }
    }

    // ====================== GET ALL (BRANCH) ======================
    suspend fun getProducts(): Result<List<ProductDTO>> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            val products = supabase.from("products")
                .select { filter { eq("branch_id", branchId) } }
                .decodeList<ProductDTO>()

            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "getProducts error", e)
            Result.failure(e)
        }
    }

    // ====================== GET BY ID ======================
    suspend fun getProductById(productId: Int): Result<ProductDTO> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            val product = supabase.from("products")
                .select {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                }
                .decodeSingle<ProductDTO>()

            Result.success(product)

        } catch (e: Exception) {
            Log.e(TAG, "getProductById error", e)
            Result.failure(e)
        }
    }

    // ====================== SEARCH BY NAME ======================
    suspend fun searchProductsByName(query: String): Result<List<ProductDTO>> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            val products = supabase.from("products")
                .select {
                    filter {
                        ilike("name", "%$query%")
                        eq("branch_id", branchId)
                    }
                }
                .decodeList<ProductDTO>()

            Result.success(products)

        } catch (e: Exception) {
            Log.e(TAG, "searchProductsByName error", e)
            Result.failure(e)
        }
    }


    // ====================== DELETE ======================
    suspend fun deleteProduct(productId: Int): Result<Unit> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            supabase.from("products")
                .delete {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "deleteProduct error", e)
            Result.failure(e)
        }
    }

    // ====================== UPDATE STOCK ======================
    suspend fun updateStock(productId: Int, newStock: Int): Result<ProductDTO> {
        return try {
            val branchId = sessionPreferences.getBranchId()
                ?: return Result.failure(Exception("Branch ID no encontrado en sesión"))

            val updates = ProductUpdateDTO(currentStock = newStock)

            val product = supabase.from("products")
                .update(updates) {
                    filter {
                        eq("product_id", productId)
                        eq("branch_id", branchId)
                    }
                    select()
                }
                .decodeSingle<ProductDTO>()

            Result.success(product)

        } catch (e: Exception) {
            Log.e(TAG, "updateStock error", e)
            Result.failure(e)
        }
    }

    // ====================== REGENERATE BARCODE ======================
    suspend fun regenerateBarcode(productId: Int): Result<ProductDTO> {
        return try {
            val barcode = generateEAN13Barcode()
            val bitmap = generateBarcodeBitmap(barcode)
            val imageUrl = uploadBarcodeImage(barcode, bitmap)

            val updates = ProductUpdateDTO(
                barcode = barcode,
                image = imageUrl
            )

            updateProduct(productId, updates)

        } catch (e: Exception) {
            Log.e(TAG, "regenerateBarcode error", e)
            Result.failure(e)
        }
    }

    // ====================== BARCODE UTILS ======================
    private fun generateEAN13Barcode(): String {
        val randomDigits = (1..12).map { (0..9).random() }
        val sumOdd = randomDigits.filterIndexed { i, _ -> i % 2 == 0 }.sum()
        val sumEven = randomDigits.filterIndexed { i, _ -> i % 2 != 0 }.sum() * 3
        val checkDigit = (10 - (sumOdd + sumEven) % 10) % 10
        return (randomDigits + checkDigit).joinToString("")
    }

    private suspend fun generateBarcodeBitmap(barcode: String): Bitmap =
        withContext(Dispatchers.Default) {
            val width = 600
            val height = 300

            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                barcode,
                BarcodeFormat.EAN_13,
                width,
                height
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap[x, y] =
                        if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            bitmap
        }

    private suspend fun uploadBarcodeImage(barcode: String, bitmap: Bitmap): String =
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()

            val fileName = "barcode_${barcode}_${System.currentTimeMillis()}.png"
            val bucket = supabase.storage.from("barcodes")

            bucket.upload(fileName, bytes) {
                contentType = ContentType.Image.PNG
            }

            bucket.publicUrl(fileName)
        }
}
