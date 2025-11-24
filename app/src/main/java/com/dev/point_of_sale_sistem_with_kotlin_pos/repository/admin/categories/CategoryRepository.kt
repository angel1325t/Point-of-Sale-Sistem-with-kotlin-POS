package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.categories

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryUpdateDTO
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Objects.isNull

class CategoryRepository(private val supabase: SupabaseClient) {

    companion object {
        private const val TABLE_NAME = "categories"
    }

    /**
     * Obtiene todas las categorías
     */
    suspend fun getAllCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select()
                .decodeList<CategoryDTO>()

            Result.success(response.map { it.toCategory() })
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Obtiene una categoría por su ID
     */
    suspend fun getCategoryById(categoryId: Int): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("category_id", categoryId) }
                }
                .decodeSingleOrNull<CategoryDTO>()

            response?.let {
                Result.success(it.toCategory())
            } ?: Result.failure(Exception(CategoryError.RecordNotFound().message))

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Busca categorías por nombre
     */
    suspend fun searchCategories(query: String): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val response = supabase.from(TABLE_NAME)
                .select {
                    filter {
                        ilike("name", "%$query%")
                    }
                }
                .decodeList<CategoryDTO>()

            Result.success(response.map { it.toCategory() })
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Obtiene categorías por ID del padre
     */
    suspend fun getCategoriesByParentId(parentId: Int?): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                val response = if (parentId == null) {
                    supabase.from(TABLE_NAME)
                        .select {
                            filter { isNull("parent_id") }
                        }
                        .decodeList<CategoryDTO>()
                } else {
                    supabase.from(TABLE_NAME)
                        .select {
                            filter { eq("parent_id", parentId) }
                        }
                        .decodeList<CategoryDTO>()
                }

                Result.success(response.map { it.toCategory() })
            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Crea una nueva categoría
     */
    suspend fun createCategory(
        name: String,
        description: String?,
        parentId: Int?
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(
                    CategoryError.ValidationError(
                        message = "El nombre de la categoría no puede estar vacío",
                        field = "name"
                    )
                )
            }

            // Validar duplicado
            val existingCategory = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("name", name.trim()) }
                }
                .decodeSingleOrNull<CategoryDTO>()

            if (existingCategory != null) {
                return@withContext Result.failure(CategoryError.DuplicateNameError())
            }

            val newCategory = CategoryInsertDTO(
                name = name.trim(),
                description = description?.trim(),
                parentId = parentId
            )

            val response = supabase.from(TABLE_NAME)
                .insert(newCategory) {
                    select()
                }
                .decodeSingle<CategoryDTO>()

            Result.success(response.toCategory())

        } catch (e: Exception) {
            Log.e("CREATE_CATEGORY", e.toString())
            Result.failure(handleException(e))
        }
    }

    /**
     * Actualiza una categoría existente
     */
    suspend fun updateCategory(
        categoryId: Int,
        name: String,
        description: String?,
        parentId: Int?
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            if (name.isBlank()) {
                return@withContext Result.failure(
                    CategoryError.ValidationError(
                        message = "El nombre de la categoría no puede estar vacío",
                        field = "name"
                    )
                )
            }

            if (parentId == categoryId) {
                return@withContext Result.failure(CategoryError.CircularReferenceError())
            }

            // Verificar duplicado (excluyendo la propia categoría)
            val existingCategories = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("name", name.trim()) }
                }
                .decodeList<CategoryDTO>()

            val duplicate = existingCategories.firstOrNull { it.categoryId != categoryId }
            if (duplicate != null) {
                return@withContext Result.failure(CategoryError.DuplicateNameError())
            }

            val updatedData = CategoryUpdateDTO(
                name = name.trim(),
                description = description?.trim(),
                parentId = parentId
            )

            val response = supabase.from(TABLE_NAME)
                .update(updatedData) {
                    filter { eq("category_id", categoryId) }
                    select()
                }
                .decodeSingleOrNull<CategoryDTO>()

            response?.let {
                Result.success(it.toCategory())
            } ?: Result.failure(CategoryError.RecordNotFound())

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Elimina una categoría
     */
    suspend fun deleteCategory(categoryId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val children = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("parent_id", categoryId) }
                }
                .decodeList<CategoryDTO>()

            if (children.isNotEmpty()) {
                return@withContext Result.failure(
                    CategoryError.CannotDeleteParentCategory(
                        childCount = children.size
                    )
                )
            }


            supabase.from(TABLE_NAME)
                .delete {
                    filter { eq("category_id", categoryId) }
                }

            Result.success(true)

        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    /**
     * Elimina varias categorías a la vez
     */
    suspend fun deleteMultipleCategories(categoryIds: List<Int>): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                var deletedCount = 0

                categoryIds.forEach { id ->
                    val result = deleteCategory(id)
                    if (result.isSuccess) deletedCount++
                }

                Result.success(deletedCount)

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Conteo de subcategorías
     */
    suspend fun getChildrenCount(categoryId: Int): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val children = supabase.from(TABLE_NAME)
                    .select {
                        filter { eq("parent_id", categoryId) }
                    }
                    .decodeList<CategoryDTO>()

                Result.success(children.size)

            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    /**
     * Manejo centralizado de excepciones
     */
    private fun handleException(e: Exception): CategoryError {
        return when {
            e.message?.contains("network", ignoreCase = true) == true ->
                CategoryError.NetworkError()
            e.message?.contains("timeout", ignoreCase = true) == true ->
                CategoryError.TimeoutError()
            e.message?.contains("unauthorized", ignoreCase = true) == true ->
                CategoryError.UnauthorizedError()
            e.message?.contains("not found", ignoreCase = true) == true ->
                CategoryError.RecordNotFound()
            else -> CategoryError.UnknownError(exception = e)
        }
    }

    /**
     * Convertir DTO a modelo interno
     */
    private fun CategoryDTO.toCategory() = Category(
        categoryId = categoryId,
        name = name,
        description = description,
        parentId = parentId,
        parentName = null
    )
}
