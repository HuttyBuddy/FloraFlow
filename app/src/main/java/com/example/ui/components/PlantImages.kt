package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import com.example.R

/**
 * Real botanical photography for every species in [com.example.data.model.ClimatePlants].
 * Photos are bundled in drawable-nodpi (sourced from Wikipedia / Wikimedia Commons)
 * so they render instantly and work fully offline.
 */
object PlantImages {

    private val byName: Map<String, Int> = mapOf(
        "Snake Plant" to R.drawable.plant_snake_plant,
        "Peace Lily" to R.drawable.plant_peace_lily,
        "ZZ Plant" to R.drawable.plant_zz_plant,
        "Cast Iron Plant" to R.drawable.plant_cast_iron_plant,
        "Fiddle Leaf Fig" to R.drawable.plant_fiddle_leaf_fig,
        "Swiss Cheese Plant" to R.drawable.plant_swiss_cheese_plant,
        "Spider Plant" to R.drawable.plant_spider_plant,
        "Jade Plant" to R.drawable.plant_jade_plant,
        "Chinese Money Plant" to R.drawable.plant_chinese_money_plant,
        "String of Pearls" to R.drawable.plant_string_of_pearls,
        "Rubber Plant" to R.drawable.plant_rubber_plant,
        "Calathea Ornata" to R.drawable.plant_calathea_ornata,
        "Parlor Palm" to R.drawable.plant_parlor_palm,
        "Prayer Plant" to R.drawable.plant_prayer_plant,
        "Aloe Vera" to R.drawable.plant_aloe_vera,
        "Golden Pothos" to R.drawable.plant_golden_pothos,
        "Boston Fern" to R.drawable.plant_boston_fern,
        "Saguaro Cactus" to R.drawable.plant_saguaro_cactus,
        "Desert Marigold" to R.drawable.plant_desert_marigold,
        "Rosemary" to R.drawable.plant_rosemary,
        "Prickly Pear Cactus" to R.drawable.plant_prickly_pear_cactus,
        "Agave Americana" to R.drawable.plant_agave_americana,
        "Bird of Paradise" to R.drawable.plant_bird_of_paradise,
        "Monstera Deliciosa" to R.drawable.plant_monstera_deliciosa,
        "Red Ginger" to R.drawable.plant_red_ginger,
        "Hibiscus" to R.drawable.plant_hibiscus,
        "Plumeria" to R.drawable.plant_plumeria,
        "Cypress Tree" to R.drawable.plant_cypress_tree,
        "Lavender" to R.drawable.plant_lavender,
        "English Lavender" to R.drawable.plant_lavender,
        "Bougainvillea" to R.drawable.plant_bougainvillea,
        "Jacaranda Tree" to R.drawable.plant_jacaranda_tree,
        "Mealy Cup Sage" to R.drawable.plant_mealy_cup_sage,
        "Olive Tree" to R.drawable.plant_olive_tree,
        "Sweet Fig Tree" to R.drawable.plant_sweet_fig_tree,
        "California Poppy" to R.drawable.plant_california_poppy,
        "Columbine" to R.drawable.plant_columbine,
        "Alpine Aster" to R.drawable.plant_alpine_aster,
        "Creeping Thyme" to R.drawable.plant_creeping_thyme,
        "Edelweiss" to R.drawable.plant_edelweiss,
        "Alpine Gentian" to R.drawable.plant_alpine_gentian,
        "Snowdrop" to R.drawable.plant_snowdrop,
        "Heather" to R.drawable.plant_heather,
        "Fuchsia" to R.drawable.plant_fuchsia,
        "English Rose" to R.drawable.plant_english_rose,
        "Japanese Maple" to R.drawable.plant_japanese_maple,
        "English Ivy" to R.drawable.plant_english_ivy,
        "Bonsai Juniper" to R.drawable.plant_bonsai_juniper,
        "Hydrangea" to R.drawable.plant_hydrangea,
        "Peonies" to R.drawable.plant_peonies,
        "Sunflower" to R.drawable.plant_sunflower,
        "Marigold" to R.drawable.plant_marigold,
        "Snapdragon" to R.drawable.plant_snapdragon,
        "Hostas" to R.drawable.plant_hostas
    )

    /**
     * Resolves a species photo for [name]. Falls back to a substring match so
     * custom plants named after a known species (e.g. "My Lavender Patch")
     * still get real imagery. Returns null when nothing matches.
     */
    @DrawableRes
    fun forPlantName(name: String): Int? {
        if (name.isBlank()) return null
        byName.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }
            ?.let { return it.value }
        return byName.entries.firstOrNull {
            name.contains(it.key, ignoreCase = true) || it.key.contains(name, ignoreCase = true)
        }?.value
    }

    /**
     * Maps plant name or fallback emoji to an appropriate realistic photo fallback.
     */
    @DrawableRes
    fun getFallbackImage(name: String, emoji: String): Int {
        val lowName = name.lowercase()
        return when {
            lowName.contains("fruit") || lowName.contains("tree") || lowName.contains("palm") || 
            emoji == "🌳" || emoji == "🌴" || emoji == "🌲" -> {
                R.drawable.plant_sweet_fig_tree
            }
            lowName.contains("succulent") || lowName.contains("cactus") || lowName.contains("aloe") || lowName.contains("jade") || 
            emoji == "🌵" || emoji == "🟢" || emoji == "🪴" -> {
                R.drawable.plant_jade_plant
            }
            lowName.contains("fern") || lowName.contains("ivy") || lowName.contains("pothos") || 
            emoji == "🌿" || emoji == "🍃" -> {
                R.drawable.plant_boston_fern
            }
            lowName.contains("rose") || lowName.contains("flower") || lowName.contains("lily") || lowName.contains("perennial") || lowName.contains("houseplant") || 
            emoji == "🌹" || emoji == "🌺" || emoji == "🌸" || emoji == "🌼" || emoji == "🕊️" || emoji == "🪻" || emoji == "🩵" || emoji == "🧡" || emoji == "💙" || emoji == "🤍" -> {
                R.drawable.plant_english_rose
            }
            lowName.contains("herb") || lowName.contains("rosemary") || lowName.contains("lavender") || lowName.contains("thyme") || 
            emoji == "🌱" -> {
                R.drawable.plant_rosemary
            }
            else -> R.drawable.plant_golden_pothos
        }
    }
}

/**
 * Square photo tile for a plant: shows the real species photo when available,
 * otherwise the realistic fallback photo on a tinted surface.
 */
@Composable
fun PlantPhoto(
    plantName: String,
    fallbackEmoji: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(10.dp),
    emojiFontSize: TextUnit = 24.sp
) {
    val imageRes = remember(plantName) { PlantImages.forPlantName(plantName) }
    val fallbackImageRes = remember(plantName, fallbackEmoji) { PlantImages.getFallbackImage(plantName, fallbackEmoji) }
    Box(
        modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        val finalImageRes = imageRes ?: fallbackImageRes
        Image(
            painter = painterResource(id = finalImageRes),
            contentDescription = "$plantName photo",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
