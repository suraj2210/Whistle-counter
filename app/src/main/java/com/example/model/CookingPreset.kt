package com.example.model

data class CookingPreset(
    val id: String,
    val name: String,
    val category: String,
    val defaultWhistles: Int,
    val flameAdvice: String,
    val tips: String
)

object DefaultPresets {
    val list = listOf(
        CookingPreset(
            id = "white_rice",
            name = "White Rice / Basmati",
            category = "Rice & Grains",
            defaultWhistles = 3,
            flameAdvice = "High flame for 1st whistle, medium for next 2",
            tips = "Rest for 10-15 minutes for natural pressure release"
        ),
        CookingPreset(
            id = "toor_dal",
            name = "Toor / Moong Dal",
            category = "Lentils",
            defaultWhistles = 4,
            flameAdvice = "Medium heat throughout to avoid froth overflow",
            tips = "Add a drop of oil and turmeric to prevent whistling spout clogging"
        ),
        CookingPreset(
            id = "chana_chickpeas",
            name = "Soaked Chickpeas (Chana)",
            category = "Beans & Pulses",
            defaultWhistles = 6,
            flameAdvice = "High heat until 1st whistle, then low flame",
            tips = "Must soak overnight (8 hrs) with a pinch of baking soda for tenderness"
        ),
        CookingPreset(
            id = "potatoes_boiling",
            name = "Boiled Potatoes",
            category = "Vegetables",
            defaultWhistles = 3,
            flameAdvice = "Medium-high heat",
            tips = "Use medium sized halved potatoes with water barely covering them"
        ),
        CookingPreset(
            id = "chicken_curry",
            name = "Chicken Curry",
            category = "Meat & Poultry",
            defaultWhistles = 2,
            flameAdvice = "High heat; do not overcook chicken",
            tips = "Release pressure manually after 5 minutes to keep meat juicy"
        ),
        CookingPreset(
            id = "mutton_curry",
            name = "Mutton / Lamb",
            category = "Meat & Poultry",
            defaultWhistles = 6,
            flameAdvice = "2 whistles on high flame, 4 whistles on low simmer",
            tips = "Ensure meat is marinated with ginger-garlic and curd for tenderness"
        ),
        CookingPreset(
            id = "rajma",
            name = "Rajma (Kidney Beans)",
            category = "Beans & Pulses",
            defaultWhistles = 7,
            flameAdvice = "1 whistle on high, remaining 6 on low flame",
            tips = "Cook with whole spices (bay leaf, black cardamom) for restaurant aroma"
        ),
        CookingPreset(
            id = "veg_biryani",
            name = "Vegetable Biryani / Pulao",
            category = "Rice & Grains",
            defaultWhistles = 2,
            flameAdvice = "Medium flame throughout",
            tips = "Turn off heat after 2nd whistle and leave closed until completely cool"
        )
    )
}
