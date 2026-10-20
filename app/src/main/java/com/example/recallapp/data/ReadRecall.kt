package com.example.recallapp.data

/**
 * Holds a recall returned by the API.
 * Dates remain strings for display, avoiding LocalDate conversion on Android.
 * Nullable fields match the entity's currently optional database columns.
 */
data class RecallDto(
    val id: Long,
    val product: String?,
    val contaminant: String?,
    val recallDate: String?,
    val manuDate: String?,
    val batch: String?,
)

/** Holds one page of recalls and the information needed to load another page. */
data class RecallPageDto(
    val content: List<RecallDto>,
    val number: Int,
    val last: Boolean,
    val totalElements: Long,
)