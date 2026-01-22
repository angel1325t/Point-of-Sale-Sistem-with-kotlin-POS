package com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.categories

import android.util.Log
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.Category
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryError
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryInsertDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.models.admin.categories.CategoryUpdateDTO
import com.dev.point_of_sale_sistem_with_kotlin_pos.repository.admin.branches.SessionPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Objects.isNull

class CategoryRepository(
    private val supabase: SupabaseClient,
    private val sessionPreferences: SessionPreferences
) {

    companion object {
        private const val TABLE_NAME = "categories"
    }

    private suspend fun getBranchId(): String =
        sessionPreferences.getBranchId()
            ?: throw IllegalStateException("BRANCH_ID_NOT_FOUND")

    private suspend fun getCompanyId(): String =
        sessionPreferences.getCompanyId()
            ?: throw IllegalStateException("COMPANY_ID_NOT_FOUND")

    suspend fun getAllCategories(): Result<List<Category>> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()

            val response = supabase.from(TABLE_NAME)
                .select {
                    filter { eq("branch_id", branchId) }
                }
                .decodeList<CategoryDTO>()

            Result.success(response.map { it.toCategory() })
        } catch (e: Exception) {
            Result.failure(handleException(e))
        }
    }

    suspend fun getCategoryById(categoryId: Int): Result<Category> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()

                val response = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("category_id", categoryId)
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeSingleOrNull<CategoryDTO>()

                response?.let {
                    Result.success(it.toCategory())
                } ?: Result.failure(CategoryError.RecordNotFound())
            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    suspend fun searchCategories(query: String): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()

                val response = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            ilike("name", "%$query%")
                            eq("branch_id", branchId)
                        }
                    }
                    .decodeList<CategoryDTO>()

                Result.success(response.map { it.toCategory() })
            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    suspend fun getCategoriesByParentId(parentId: Int?): Result<List<Category>> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()

                val response = supabase.from(TABLE_NAME)
                    .select {
                        filter {
                            eq("branch_id", branchId)
                            parentId?.let { eq("parent_id", it) } ?: isNull("parent_id")
                        }
                    }
                    .decodeList<CategoryDTO>()

                Result.success(response.map { it.toCategory() })
            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    suspend fun createCategory(
        name: String,
        description: String?,
        parentId: Int?
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()
            val companyId = getCompanyId()

            val insert = CategoryInsertDTO(
                name = name.trim(),
                description = description?.trim(),
                parentId = parentId,
                companyId = companyId,
                branchId = branchId
            )

            val response = supabase.from(TABLE_NAME)
                .insert(insert) { select() }
                .decodeSingle<CategoryDTO>()

            Result.success(response.toCategory())
        } catch (e: Exception) {
            Log.e("CREATE_CATEGORY", e.message ?: "")
            Result.failure(handleException(e))
        }
    }

    suspend fun updateCategory(
        categoryId: Int,
        name: String,
        description: String?,
        parentId: Int?
    ): Result<Category> = withContext(Dispatchers.IO) {
        try {
            val branchId = getBranchId()

            val update = CategoryUpdateDTO(
                name = name.trim(),
                description = description?.trim(),
                parentId = parentId
            )

            val response = supabase.from(TABLE_NAME)
                .update(update) {
                    filter {
                        eq("category_id", categoryId)
                        eq("branch_id", branchId)
                    }
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

    suspend fun deleteCategory(categoryId: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val branchId = getBranchId()

                supabase.from(TABLE_NAME)
                    .delete {
                        filter {
                            eq("category_id", categoryId)
                            eq("branch_id", branchId)
                        }
                    }

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(handleException(e))
            }
        }

    suspend fun deleteMultipleCategories(categoryIds: List<Int>): Result<Int> =
        withContext(Dispatchers.IO) {
            var count = 0
            categoryIds.forEach {
                if (deleteCategory(it).isSuccess) count++
            }
            Result.success(count)
        }

    private fun handleException(e: Exception): CategoryError =
        CategoryError.UnknownError(exception = e)

    private fun CategoryDTO.toCategory() = Category(
        categoryId = categoryId,
        name = name,
        description = description,
        parentId = parentId,
        parentName = null,
        companyId = companyId,
        branchId = branchId
    )
}
