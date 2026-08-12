package com.uvg.cc3087.eztunez

data class TuningPreset(
    val id: Int,
    val name: String,
    val tuning: String,
    val description: String,
    val category: PresetCategory,

)

enum class PresetCategory {
    GUITAR,
    CUSTOM
}