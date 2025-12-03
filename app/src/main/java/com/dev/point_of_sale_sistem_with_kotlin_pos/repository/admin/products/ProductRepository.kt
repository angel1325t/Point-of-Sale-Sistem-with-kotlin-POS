package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.products

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.set
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.products.*
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
    private val context: Context,
) {

    companion object {
        private const val TAG = "ProductRepository"
    }

    // ====================== CREAR =====================================
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
            Log.d(TAG, """
                createProduct: Iniciando creación
                - name: $name
                - price: $price
                - categoryId: $categoryId
                - currentStock: $currentStock
                - minimumStock: $minimumStock
                - discountType: $discountType
                - discountValue: $discountValue
            """.trimIndent())

            val barcode = generateEAN13Barcode()
            Log.d(TAG, "createProduct: Barcode generado: $barcode")

            val barcodeBitmap = generateBarcodeBitmap(barcode)
            Log.d(TAG, "createProduct: Bitmap generado")

            val barcodeImageUrl = uploadBarcodeImage(barcode, barcodeBitmap)
            Log.d(TAG, "createProduct: Imagen subida: $barcodeImageUrl")

            // ✅ USAR ProductInsertDTO EN LUGAR DE Map
            val insertData = ProductInsertDTO(
                name = name,
                description = description,
                price = price,
                barcode = barcode,
                categoryId = categoryId,
                image = barcodeImageUrl,
                currentStock = currentStock,
                minimumStock = minimumStock,
                discountType = discountType.name.lowercase(),
                discountValue = discountValue
            )

            Log.d(TAG, "createProduct: Insertando en base de datos: $insertData")

            // ✅ IMPORTANTE: Usar .select() después de .insert() para obtener el registro creado
            val product = supabase.from("products")
                .insert(insertData) {
                    select()
                }
                .decodeSingle<ProductDTO>()

            Log.d(TAG, "createProduct: Producto creado exitosamente: $product")
            Result.success(product)

        } catch (e: Exception) {
            Log.e(TAG, "createProduct: Error al crear producto", e)
            Result.failure(e)
        }
    }

    // ====================== UPDATE GENERAL ======================
    suspend fun updateProduct(productId: Int, updates: ProductUpdateDTO): Result<ProductDTO> {
        return try {
            Log.d(TAG, "updateProduct: Actualizando producto ID $productId con: $updates")

            // ✅ IMPORTANTE: Usar .select() después de .update() para obtener el registro actualizado
            val product = supabase.from("products")
                .update(updates) {
                    filter { eq("product_id", productId) }
                    select()
                }
                .decodeSingle<ProductDTO>()

            Log.d(TAG, "updateProduct: Producto actualizado exitosamente: $product")
            Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "updateProduct: Error al actualizar producto ID $productId", e)
            Result.failure(e)
        }
    }

    // ====================== GET ALL ======================
    suspend fun getProducts(): Result<List<ProductDTO>> {
        return try {
            Log.d(TAG, "getProducts: Obteniendo todos los productos")

            val products = supabase.from("products")
                .select()
                .decodeList<ProductDTO>()

            Log.d(TAG, "getProducts: ${products.size} productos obtenidos")
            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "getProducts: Error al obtener productos", e)
            Result.failure(e)
        }
    }

    // ====================== GET BY ID ======================
    suspend fun getProductById(productId: Int): Result<ProductDTO> {
        return try {
            Log.d(TAG, "getProductById: Buscando producto ID: $productId")

            val product = supabase.from("products")
                .select { filter { eq("product_id", productId) } }
                .decodeSingle<ProductDTO>()

            Log.d(TAG, "getProductById: Producto encontrado: $product")
            Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "getProductById: Error al buscar producto ID $productId", e)
            Result.failure(e)
        }
    }

    // ====================== BUSCAR POR NAME ======================
    suspend fun searchProductsByName(query: String): Result<List<ProductDTO>> {
        return try {
            Log.d(TAG, "searchProductsByName: Buscando con query: '$query'")

            val products = supabase.from("products")
                .select { filter { ilike("name", "%$query%") } }
                .decodeList<ProductDTO>()

            Log.d(TAG, "searchProductsByName: ${products.size} productos encontrados")
            Result.success(products)
        } catch (e: Exception) {
            Log.e(TAG, "searchProductsByName: Error en búsqueda", e)
            Result.failure(e)
        }
    }

    // ====================== BUSCAR POR BARCODE ======================
    suspend fun getProductByBarcode(barcode: String): Result<ProductDTO?> {
        return try {
            Log.d(TAG, "getProductByBarcode: Buscando barcode: $barcode")

            val products = supabase.from("products")
                .select { filter { eq("barcode", barcode) } }
                .decodeList<ProductDTO>()

            Log.d(TAG, "getProductByBarcode: Resultado: ${products.firstOrNull()}")
            Result.success(products.firstOrNull())
        } catch (e: Exception) {
            Log.e(TAG, "getProductByBarcode: Error al buscar barcode", e)
            Result.failure(e)
        }
    }

    // ====================== DELETE ======================
    suspend fun deleteProduct(productId: Int): Result<Unit> {
        return try {
            Log.d(TAG, "deleteProduct: Eliminando producto ID: $productId")

            supabase.from("products")
                .delete { filter { eq("product_id", productId) } }

            Log.d(TAG, "deleteProduct: Producto eliminado exitosamente")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteProduct: Error al eliminar producto ID $productId", e)
            Result.failure(e)
        }
    }

    // ====================== UPDATE STOCK ======================
    suspend fun updateStock(productId: Int, newStock: Int): Result<ProductDTO> {
        return try {
            Log.d(TAG, "updateStock: Actualizando stock del producto ID $productId a $newStock")

            // ✅ USAR DTO EN LUGAR DE Map
            val updates = ProductUpdateDTO(currentStock = newStock)

            // ✅ IMPORTANTE: Usar .select() después de .update()
            val product = supabase.from("products")
                .update(updates) {
                    filter { eq("product_id", productId) }
                    select()
                }
                .decodeSingle<ProductDTO>()

            Log.d(TAG, "updateStock: Stock actualizado exitosamente")
            Result.success(product)
        } catch (e: Exception) {
            Log.e(TAG, "updateStock: Error al actualizar stock", e)
            Result.failure(e)
        }
    }

    // ====================== REGENERAR BARCODE ======================
    suspend fun regenerateBarcode(productId: Int): Result<ProductDTO> {
        return try {
            Log.d(TAG, "regenerateBarcode: Regenerando barcode para producto ID: $productId")

            val barcode = generateEAN13Barcode()
            val barcodeBitmap = generateBarcodeBitmap(barcode)
            val barcodeImageUrl = uploadBarcodeImage(barcode, barcodeBitmap)

            val updates = ProductUpdateDTO(
                barcode = barcode,
                image = barcodeImageUrl
            )

            Log.d(TAG, "regenerateBarcode: Nuevo barcode generado: $barcode")
            updateProduct(productId, updates)

        } catch (e: Exception) {
            Log.e(TAG, "regenerateBarcode: Error al regenerar barcode", e)
            Result.failure(e)
        }
    }

    // ====================== GENERAR BARCODE ======================
    private fun generateEAN13Barcode(): String {
        val randomDigits = (1..12).map { (0..9).random() }
        val sumOdd = randomDigits.filterIndexed { index, _ -> index % 2 == 0 }.sum()
        val sumEven = randomDigits.filterIndexed { index, _ -> index % 2 != 0 }.sum() * 3
        val checkDigit = (10 - (sumOdd + sumEven) % 10) % 10
        return (randomDigits + checkDigit).joinToString("")
    }

    // ====================== CREAR BITMAP ======================
    private suspend fun generateBarcodeBitmap(barcode: String): Bitmap = withContext(Dispatchers.Default) {
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
                bitmap[x, y] = if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
        }

        bitmap
    }

    // ====================== UPLOAD STORAGE ======================
    private suspend fun uploadBarcodeImage(barcode: String, bitmap: Bitmap): String {
        return withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            val fileName = "barcode_${barcode}_${System.currentTimeMillis()}.png"

            val bucket = supabase.storage.from("barcodes")

            bucket.upload(fileName, byteArray) {
                contentType = ContentType.Image.PNG
            }

            bucket.publicUrl(fileName)
        }
    }
}