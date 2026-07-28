package com.example.data.model

data class PlantTemplate(
    val name: String,
    val type: String,
    val careSpring: String,
    val careSummer: String,
    val careAutumn: String,
    val careWinter: String,
    val soilType: String,
    val sunlight: String,
    val iconEmoji: String,
    val matureSize: String,
    val wateringNeeds: String,
    val bloomTime: String,
    val pestsDiseases: String,
    val compatibleClimate: String,
    val funFacts: List<String> = emptyList(),
    val isIndoor: Boolean = true,
    val indoorRoom: String = "Living Room",
    val lightLevel: String = "Medium Indirect",
    val petSafe: Boolean = true,
    val airPurifying: Boolean = true
)

object ClimatePlants {
    val CLIMATES = listOf("Temperate", "Arid (Desert)", "Tropical (Humid)", "Mediterranean", "Mountainous")
    
    val STYLES = listOf("Indoor Living Room", "Work Desk Sanctuary", "Bedroom Oasis", "Sunlit Window Nook", "Biophilic Botanical Studio")
    
    val INDOOR_ROOMS = listOf("All", "Living Room", "Work Desk Sanctuary", "Bedroom Oasis", "Sunlit Window Nook", "High Humidity Bathroom")
    val INDOOR_LIGHTS = listOf("All", "Low Light", "Medium Indirect", "Bright Indirect", "Direct Window Sun")
    val INDOOR_BENEFITS = listOf("All", "Air Purifying", "Pet Safe")

    fun mapZipToClimate(zip: String): String {
        if (zip.isBlank()) return "Temperate"
        val cleanZip = zip.trim()
        if (!cleanZip.all { it.isDigit() } || cleanZip.length != 5) {
            val lower = cleanZip.lowercase()
            return when {
                lower.contains("phoenix") || lower.contains("desert") || lower.contains("arid") -> "Arid (Desert)"
                lower.contains("miami") || lower.contains("tropical") || lower.contains("humid") -> "Tropical (Humid)"
                lower.contains("seattle") || lower.contains("rain") -> "Temperate"
                lower.contains("denver") || lower.contains("bozeman") || lower.contains("mountain") -> "Mountainous"
                lower.contains("los angeles") || lower.contains("san diego") || lower.contains("mediterranean") -> "Mediterranean"
                else -> "Temperate"
            }
        }
        
        val prefix = cleanZip.substring(0, 3)
        val prefixInt = prefix.toIntOrNull() ?: return "Temperate"
        
        return when {
            // Tropical: Florida Keys / Southern Florida, Hawaii, Puerto Rico, US Virgin Islands
            prefixInt in 330..334 || prefixInt in 339..349 || prefixInt in 967..968 || prefixInt in 6..9 -> "Tropical (Humid)"
            
            // Arid (Desert): Arizona, Nevada, New Mexico, Utah, West Texas, Inland Southern California
            prefixInt in 850..865 || prefixInt in 890..898 || prefixInt in 870..884 || prefixInt in 840..847 || 
            prefixInt in 797..799 || prefixInt in 922..925 || prefixInt in 935..935 -> "Arid (Desert)"
            
            // Mediterranean: Southern & Coastal California
            prefixInt in 900..921 || prefixInt in 926..934 || prefixInt in 936..949 || prefixInt in 954..958 -> "Mediterranean"
            
            // Mountainous: Colorado, Wyoming, Montana, Idaho, parts of Utah/Oregon/Washington high altitudes
            prefixInt in 800..816 || prefixInt in 820..831 || prefixInt in 590..599 || prefixInt in 832..838 -> "Mountainous"
            
            // Temperate: Pacific Northwest, Northeast, Midwest, South
            else -> "Temperate"
        }
    }

    val ALL_TEMPLATES = listOf(
        // === INDOOR PLANTS ===
        PlantTemplate(
            name = "Snake Plant",
            type = "Succulent",
            careSpring = "Water monthly. Wipe leaves to remove dust.",
            careSummer = "Water every 3 weeks. Enjoys warm air.",
            careAutumn = "Reduce watering as temperatures cool down.",
            careWinter = "Water once in 6 weeks. Avoid cold window drafts.",
            soilType = "Sandy, highly well-drained cactus soil",
            sunlight = "Low to Bright Indirect Light",
            iconEmoji = "🪴",
            matureSize = "2 - 4 feet tall",
            wateringNeeds = "Very Low",
            bloomTime = "Rarely (Winter)",
            pestsDiseases = "Root rot in soggy soil",
            compatibleClimate = "Arid (Desert) Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Snake plants release oxygen at night, unlike most plants which do so during the day.",
                "It is also known as Mother-in-Law's Tongue due to its sharp leaf tips."
            )
        ),
        PlantTemplate(
            name = "Peace Lily",
            type = "Flower",
            careSpring = "Keep soil consistently moist. Mist leaves regularly.",
            careSummer = "Water twice a week. Appreciates high humidity.",
            careAutumn = "Clean leaves with a damp cloth. Reduce fertilizer.",
            careWinter = "Water weekly when topsoil is dry.",
            soilType = "Peat-rich, aerated potting soil",
            sunlight = "Medium to Low Indirect Light",
            iconEmoji = "🕊️",
            matureSize = "1 - 3 feet tall",
            wateringNeeds = "Moderate to High",
            bloomTime = "Spring",
            pestsDiseases = "Aphids, Leaf spot from cold water",
            compatibleClimate = "Tropical (Humid) Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Peace Lilies are not true lilies, but members of the Araceae family.",
                "They are famous for 'dramatically' drooping when they need water and rebounding quickly."
            )
        ),
        PlantTemplate(
            name = "ZZ Plant",
            type = "Succulent",
            careSpring = "Water every 3-4 weeks. Fertilize once.",
            careSummer = "Water bi-weekly if very hot. Keep out of direct sun.",
            careAutumn = "Reduce watering. Wipe glossy leaves.",
            careWinter = "Water once a month. Keep in warm room.",
            soilType = "Standard well-draining soil",
            sunlight = "Low to Bright Indirect Light",
            iconEmoji = "🌿",
            matureSize = "2 - 3 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Non-flowering",
            pestsDiseases = "Mealybugs, Leaf yellowing from overwater",
            compatibleClimate = "Arid (Desert) Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Its waxy leaves are so shiny they are often mistaken for plastic artificial leaves.",
                "It grows from potato-like rhizomes that store water underground for months."
            )
        ),
        PlantTemplate(
            name = "Cast Iron Plant",
            type = "Shrub",
            careSpring = "Water when top 2 inches of soil are dry.",
            careSummer = "Water weekly. Wipe leaves to maintain shine.",
            careAutumn = "Lessen watering. No fertilizer needed.",
            careWinter = "Keep dry and cool, water monthly.",
            soilType = "Organic, well-draining loam",
            sunlight = "Deep Shade to Low Light",
            iconEmoji = "🍃",
            matureSize = "2 - 3 feet tall",
            wateringNeeds = "Low to Moderate",
            bloomTime = "Rarely (Summer)",
            pestsDiseases = "Scale insects, Spider mites",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Earned its name because it survives extreme neglect, low light, and temperature changes.",
                "It was highly popular in Victorian England as it survived dark, drafty, gas-lit parlors."
            )
        ),
        PlantTemplate(
            name = "Fiddle Leaf Fig",
            type = "Tree",
            careSpring = "Water weekly. Rotate 90 degrees to ensure even growth.",
            careSummer = "Water deeply twice a week. Mist foliage.",
            careAutumn = "Clean large leaves. Move away from heaters.",
            careWinter = "Lessen watering, ensure maximum window light.",
            soilType = "Fast-draining soil with bark pieces",
            sunlight = "Bright Consistent Indirect Light",
            iconEmoji = "🌳",
            matureSize = "6 - 10 feet tall (indoors)",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering (indoors)",
            pestsDiseases = "Fungal leaf spot, Spider mites",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Its leaves are shaped like violins or fiddles, hence the name.",
                "Native to West African rainforests where it grows up to 50 feet tall."
            )
        ),
        PlantTemplate(
            name = "Swiss Cheese Plant",
            type = "Shrub",
            careSpring = "Water weekly. Provide a moss pole to climb.",
            careSummer = "Water twice a week. Wipe large leaves to help photosynthesis.",
            careAutumn = "Reduce watering as light decreases.",
            careWinter = "Water every 10 days, keep away from cold windows.",
            soilType = "Chunky aroid potting mix",
            sunlight = "Medium to Bright Indirect Light",
            iconEmoji = "🌿",
            matureSize = "4 - 8 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering (indoors)",
            pestsDiseases = "Thrips, Root rot",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Monstera Adansonii develops closed holes in its leaves to let light reach lower branches.",
                "It is a natural climber that uses aerial roots to hold onto tree bark."
            )
        ),
        PlantTemplate(
            name = "Spider Plant",
            type = "Flower",
            careSpring = "Water weekly. Repot if roots burst from pot.",
            careSummer = "Water deeply twice a week. Prune brown leaf tips.",
            careAutumn = "Reduce watering. Collect spiderette offsets to propagate.",
            careWinter = "Water monthly, mist weekly for humidity.",
            soilType = "Light, sandy organic mix",
            sunlight = "Bright Filtered Light",
            iconEmoji = "🪴",
            matureSize = "1 - 1.5 feet wide",
            wateringNeeds = "Moderate",
            bloomTime = "Summer",
            pestsDiseases = "Tip burn from fluoride, Scale",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Spider plants produce baby offsets called 'spiderettes' that hang down like spiders on a web.",
                "It is one of the easiest house plants to propagate in plain water."
            )
        ),
        PlantTemplate(
            name = "Jade Plant",
            type = "Succulent",
            careSpring = "Water only when leaves start to pucker slightly.",
            careSummer = "Water bi-weekly. Enjoys warm sunny windows.",
            careAutumn = "withhold watering as temperatures drop.",
            careWinter = "Keep very dry. Protect from freezing.",
            soilType = "Gritty, well-aerated succulent soil",
            sunlight = "Full Sun to Bright Light",
            iconEmoji = "🪴",
            matureSize = "1 - 3 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Late Winter (rarely)",
            pestsDiseases = "Mealybugs, Stem rot",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Often called the Money Tree or Friendship Plant, believed to bring financial luck in Asia.",
                "Can live for over 50 years, passing down through generations as a family heirloom."
            )
        ),
        PlantTemplate(
            name = "Chinese Money Plant",
            type = "Succulent",
            careSpring = "Water weekly when soil dries. Rotate for straight stems.",
            careSummer = "Water twice weekly. Avoid direct midday sun.",
            careAutumn = "Clean circular leaves. Remove baby pups to propagate.",
            careWinter = "Water every 2 weeks, keep in bright warm room.",
            soilType = "Peaty, well-drained potting mix",
            sunlight = "Bright Indirect Light",
            iconEmoji = "🪴",
            matureSize = "1 - 1.2 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Spring (Rarely)",
            pestsDiseases = "Fungus gnats, Spider mites",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Also known as the UFO plant due to its perfectly round, flat, green leaves.",
                "Originally brought to Europe by a Norwegian missionary in 1946, shared via cuttings."
            )
        ),
        PlantTemplate(
            name = "String of Pearls",
            type = "Succulent",
            careSpring = "Water sparingly when pearls feel slightly soft.",
            careSummer = "Water bi-weekly. Keep trailing vines in bright window.",
            careAutumn = "Reduce water, let soil dry completely.",
            careWinter = "Water once in 6 weeks, keep dry and bright.",
            soilType = "Cactus mix with extra coarse sand",
            sunlight = "Bright Light with Some Direct Morning Sun",
            iconEmoji = "🟢",
            matureSize = "2 - 3 feet trailing vines",
            wateringNeeds = "Very Low",
            bloomTime = "Summer (tiny white flowers)",
            pestsDiseases = "Root rot, Aphids",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Each 'pearl' is a spherical leaf that minimizes surface area to reduce water evaporation.",
                "The pearls have a narrow green slit called an 'epidermal window' that lets light inside the leaf."
            )
        ),
        PlantTemplate(
            name = "Rubber Plant",
            type = "Tree",
            careSpring = "Water weekly. Wipe leaves to keep glossy.",
            careSummer = "Water twice weekly. Mist leaves. Apply liquid feed.",
            careAutumn = "Reduce watering as growth slows down.",
            careWinter = "Water once in 3 weeks, wipe leaves.",
            soilType = "Rich organic, well-draining soil",
            sunlight = "Bright Indirect Light",
            iconEmoji = "🪴",
            matureSize = "4 - 8 feet tall (indoors)",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering (indoors)",
            pestsDiseases = "Scale, Root rot",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Its white sap contains latex, which was historically used to manufacture rubber.",
                "Native to India and Malaysia where it grows into huge banyan-like trees."
            )
        ),
        PlantTemplate(
            name = "Calathea Ornata",
            type = "Shrub",
            careSpring = "Keep soil damp. Mist daily or use humidifier.",
            careSummer = "Water twice weekly. Avoid tap water; use rainwater.",
            careAutumn = "Ensure temperature is warm. Mist foliage.",
            careWinter = "Water weekly. Keep away from drafts.",
            soilType = "Moisture-retentive, peaty mix",
            sunlight = "Medium Indirect Light / Shade",
            iconEmoji = "🌿",
            matureSize = "1.5 - 2 feet tall",
            wateringNeeds = "High (moist soil)",
            bloomTime = "Non-flowering (indoors)",
            pestsDiseases = "Spider mites, Leaf edge curling",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Known as the Pinstripe Calathea because its leaves look like they were hand-painted with pink lines.",
                "Like other prayer plants, its leaves fold upward at night and open in the morning."
            )
        ),
        PlantTemplate(
            name = "Parlor Palm",
            type = "Tree",
            careSpring = "Water when topsoil feels dry. Mist twice weekly.",
            careSummer = "Water weekly. Keep out of direct hot sun.",
            careAutumn = "Reduce watering. Cut off yellow fronds.",
            careWinter = "Water every 2 weeks. Mist to prevent mites.",
            soilType = "Standard sandy loam mix",
            sunlight = "Low to Medium Indirect Light",
            iconEmoji = "🌴",
            matureSize = "3 - 5 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering (indoors)",
            pestsDiseases = "Spider mites, Scale",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "One of the first houseplants sold in the Victorian era, prized for surviving dark, chilly halls.",
                "It is a highly effective air purifier and completely non-toxic to cats and dogs."
            )
        ),
        PlantTemplate(
            name = "Prayer Plant",
            type = "Shrub",
            careSpring = "Keep soil moist. Mist leaves daily.",
            careSummer = "Water twice weekly with distilled water. Maintain high humidity.",
            careAutumn = "Clean decorated leaves. Move away from cold spots.",
            careWinter = "Water weekly, keep in warm humid room.",
            soilType = "Peaty, well-draining soil",
            sunlight = "Medium Filtered Light / Dappled Shade",
            iconEmoji = "🧘",
            matureSize = "1 - 1.2 feet tall & wide",
            wateringNeeds = "High",
            bloomTime = "Spring (small purple blooms)",
            pestsDiseases = "Spider mites, Leaf spot",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Folds its leaves together at night, resembling hands closed in prayer.",
                "The botanical name Maranta honors Bartolomeo Maranta, a famous 16th-century Italian botanist."
            )
        ),
        PlantTemplate(
            name = "Aloe Vera",
            type = "Succulent",
            careSpring = "Water only when soil is bone dry. Good air flow.",
            careSummer = "Water bi-weekly. Avoid standing water in the rosette.",
            careAutumn = "Bring indoors if cold. Water monthly.",
            careWinter = "Keep dry, bright indirect light limit water.",
            soilType = "Gritty desert cactus mix",
            sunlight = "Bright Indirect Sun",
            iconEmoji = "🪴",
            matureSize = "1 - 2 feet wide",
            wateringNeeds = "Low",
            bloomTime = "Mid Summer",
            pestsDiseases = "Snails, Root rot from overwatering",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Aloe vera has been used medicinally for over 6,000 years, beginning in ancient Egypt.",
                "It consists of 99% water, which allows it to survive in extremely arid climates.",
                "In ancient times, Egyptians called aloe the 'plant of immortality' and gave it as funeral gifts."
            )
        ),
        PlantTemplate(
            name = "Golden Pothos",
            type = "Shrub",
            careSpring = "Water only when top soil is dry. Prune vine tips.",
            careSummer = "Loves warmth and bright indirect light. Water weekly.",
            careAutumn = "Reduce watering as temperatures drop.",
            careWinter = "Keep in warm indoor room, water monthly.",
            soilType = "Rich organic potting soil",
            sunlight = "Filtered Bright Light",
            iconEmoji = "🌿",
            matureSize = "1 - 2 feet tall, 6 - 8 feet trailing",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering",
            pestsDiseases = "Mealybugs, Root rot in standing water",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Nicknamed 'Devil's Ivy' because it is nearly impossible to kill and stays green even in the dark.",
                "It is a highly efficient air purifier, removing airborne toxins like formaldehyde and carbon monoxide.",
                "Pothos vines can grow up to 40 feet long in tropical forests."
            )
        ),
        PlantTemplate(
            name = "Boston Fern",
            type = "Fern",
            careSpring = "Repot with fresh peat moss. Start misting.",
            careSummer = "Water daily, do not let soil dry out. Avoid midday sun.",
            careAutumn = "Shedding old fronds. Lessen feeding.",
            careWinter = "Bring indoors, keep in moist bathroom/washroom.",
            soilType = "Peat moss loam",
            sunlight = "Deep to Partial Shade",
            iconEmoji = "🌿",
            matureSize = "2 - 3 feet wide",
            wateringNeeds = "High",
            bloomTime = "Non-flowering",
            pestsDiseases = "Frond bugs, low humidity leaf drops",
            compatibleClimate = "Temperate Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Boston Ferns were highly popular in the Victorian era, occupying pride of place in parlor windows.",
                "They are excellent humidifiers, releasing moisture and restoring humidity to dry indoor spaces.",
                "Ferns reproduce using microscopic spores on the undersides of their leaves rather than seeds."
            )
        ),

        // === SPECIALTY INDOOR & DESK HOUSEPLANTS ===
        PlantTemplate(
            name = "Saguaro Cactus",
            type = "Succulent",
            careSpring = "Water once a month. No pruning needed.",
            careSummer = "Water every 3 weeks. Full direct sun.",
            careAutumn = "Withhold watering as temperature drops.",
            careWinter = "Keep dry indoors and away from cold window glass.",
            soilType = "Perlite and sand mix",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌵",
            matureSize = "15 - 40 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Late Spring (May - June)",
            pestsDiseases = "Bacterial necrosis, Mealybugs",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Saguaro cacti can take up to 75 years to grow their first side arm.",
                "They store huge amounts of water; a fully hydrated saguaro can weigh over 3,000 pounds.",
                "Saguaro blossoms are Arizona's state flower and bloom only at night."
            )
        ),
        PlantTemplate(
            name = "Desert Marigold",
            type = "Flower",
            careSpring = "Sow seeds in shallow soil. Water lightly to sprout.",
            careSummer = "Moderate drought tolerance. Water weekly.",
            careAutumn = "Cut back old blooms to encourage new sprout cycles.",
            careWinter = "Keep soil dry. Protect from cold indoor window drafts.",
            soilType = "Dry sandy soils",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌼",
            matureSize = "1 - 1.5 feet tall",
            wateringNeeds = "Low to Moderate",
            bloomTime = "Spring to Autumn",
            pestsDiseases = "Spider mites, Powdery mildew",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Its leaves are covered in dense, woolly hairs that reflect sunlight and reduce water loss.",
                "Wild desert marigolds bloom so densely they can turn entire hillsides solid yellow.",
                "Indigenous tribes used the plant's sticky sap as an organic adhesive."
            )
        ),
        PlantTemplate(
            name = "Rosemary",
            type = "Herb",
            careSpring = "Prune woody branches, apply minimal compost.",
            careSummer = "Water weekly. Withstands high desert heat.",
            careAutumn = "Prune lightly to keep compact shape.",
            careWinter = "In severe cold, cover with mulch or burlap.",
            soilType = "Well-draining chalky soil",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌿",
            matureSize = "3 - 5 feet tall & wide",
            wateringNeeds = "Low to Moderate",
            bloomTime = "Spring to Summer",
            pestsDiseases = "Spittlebugs, Root rot in soggy soil",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Rosemary is historically associated with memory; ancient scholars wore it in their hair during exams.",
                "It belongs to the mint family and is highly resistant to common indoor houseplant pests.",
                "The name rosemary comes from Latin 'ros marinus,' meaning 'dew of the sea.'"
            )
        ),
        PlantTemplate(
            name = "Prickly Pear Cactus",
            type = "Succulent",
            careSpring = "Minimal water. Plant in sunny bed.",
            careSummer = "Water deeply but infrequently (monthly).",
            careAutumn = "Stop fertilizer, let soil stay loose and dry.",
            careWinter = "Completely dormant. Keep in bright, dry room during winter.",
            soilType = "Sandy, extremely gravelly soil",
            sunlight = "Full Sun",
            iconEmoji = "🌵",
            matureSize = "3 - 6 feet tall",
            wateringNeeds = "Very Low",
            bloomTime = "Late Spring",
            pestsDiseases = "Cochineal scale, Broad mites",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Both the flat pads (nopales) and the bright red fruits (tunas) are fully edible and delicious.",
                "Prickly pear sap can be used to purify dirty drinking water organically.",
                "The plant was imported to Australia in the 1800s and took over millions of acres of farmland."
            )
        ),
        PlantTemplate(
            name = "Agave Americana",
            type = "Succulent",
            careSpring = "Minimal water. Ensure gravel is loose.",
            careSummer = "Water monthly. Thrives in burning desert heat.",
            careAutumn = "Cease fertilizing. Mulch base slightly.",
            careWinter = "Do not water. Keep in sunny indoor spot away from chill.",
            soilType = "Rocky, sandy alkaline soil",
            sunlight = "Full Direct Sun",
            iconEmoji = "🪴",
            matureSize = "4 - 6 feet tall & wide",
            wateringNeeds = "Very Low",
            bloomTime = "Once every 10-15 years",
            pestsDiseases = "Agave snout weevil",
            compatibleClimate = "Arid (Desert)",
            isIndoor = true,
            funFacts = listOf(
                "Commonly known as the Century Plant because it blooms so infrequently.",
                "It dies after blooming, but produces offsets (pups) at its base to carry on."
            )
        ),
        PlantTemplate(
            name = "Bird of Paradise",
            type = "Flower",
            careSpring = "Fertilize monthly. Keep soil moist.",
            careSummer = "Water twice a week. High ambient humidity.",
            careAutumn = "Cut back tattered leaves.",
            careWinter = "Minimize water, keep above 10°C (50°F).",
            soilType = "Rich organic potting soil",
            sunlight = "Bright Direct/Indirect",
            iconEmoji = "🌺",
            matureSize = "5 - 6 feet tall",
            wateringNeeds = "Moderate to High",
            bloomTime = "Winter to Spring",
            pestsDiseases = "Scale, Mealybugs",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "The unique flower shape resembles a colorful bird in flight, designed to attract pollinating birds.",
                "In Hawaii, the flower is considered a symbol of magnificence and royalty.",
                "It is closely related to the banana plant and shares similar large, glossy leaves."
            )
        ),
        PlantTemplate(
            name = "Monstera Deliciosa",
            type = "Shrub",
            careSpring = "Wipe leaves with damp cloth. Water when top inch of soil is dry.",
            careSummer = "Feed foliage feed. Water thoroughly twice a week.",
            careAutumn = "Reduce watering as days shorten.",
            careWinter = "Keep moist but never soggy. Keep near humidifier.",
            soilType = "Peat-based potting mix",
            sunlight = "Medium Indirect Light",
            iconEmoji = "🌿",
            matureSize = "6 - 10 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Summer (Rare)",
            pestsDiseases = "Fungus gnats, Spider mites",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "The fenestrations (holes) in its leaves allow jungle winds and light to pass through without tearing them.",
                "The name Deliciosa refers to the delicious edible fruit it produces in the wild.",
                "In Chinese culture, the Monstera is a symbol of long life and honoring elders."
            )
        ),
        PlantTemplate(
            name = "Red Ginger",
            type = "Flower",
            careSpring = "Keep soil consistently moist. Apply dynamic organic feed.",
            careSummer = "Loves full hot moisture. Water daily.",
            careAutumn = "Trim spent blossom spikes from the base.",
            careWinter = "Protect from chills. Bring indoors if under 60°F.",
            soilType = "Humus-rich moist soil",
            sunlight = "Partial Sun",
            iconEmoji = "🔥",
            matureSize = "4 - 8 feet tall",
            wateringNeeds = "High",
            bloomTime = "Year-round (Warm climates)",
            pestsDiseases = "Nematodes, Leaf spot",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "The showy red 'flower' is actually a series of colored bracts; the true flower is small and white inside.",
                "It is the national flower of Samoa, known locally as 'teuila.'",
                "It can spread rapidly in humid rainforests via underground rhizomes."
            )
        ),
        PlantTemplate(
            name = "Hibiscus",
            type = "Flower",
            careSpring = "Apply high potassium fertilizer to spark blossoms.",
            careSummer = "Water daily in high heat. Prefers damp, humid air.",
            careAutumn = "Prune woody branches to maintain shape.",
            careWinter = "Bring indoors if temperature falls below 50°F.",
            soilType = "Slightly acidic, organic rich soil",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌺",
            matureSize = "4 - 8 feet tall",
            wateringNeeds = "High",
            bloomTime = "Summer to Autumn",
            pestsDiseases = "Whiteflies, Aphids",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Hibiscus flowers are fully edible and make a popular tart herbal tea rich in Vitamin C.",
                "Its flowers bloom for only 24 hours, but the bush produces new ones continuously."
            )
        ),
        PlantTemplate(
            name = "Plumeria",
            type = "Flower",
            careSpring = "Water weekly. Apply bloom-booster fertilizer.",
            careSummer = "Water deeply twice weekly. Loves hot sun.",
            careAutumn = "Stop fertilizing, reduce water as leaves shed.",
            careWinter = "Let go dormant; keep dry in a warm indoor room.",
            soilType = "Very sandy, porous soil",
            sunlight = "Full Sun",
            iconEmoji = "🌸",
            matureSize = "5 - 15 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Early Summer to Autumn",
            pestsDiseases = "Plumeria rust, Mealybugs",
            compatibleClimate = "Tropical (Humid)",
            isIndoor = true,
            funFacts = listOf(
                "Commonly known as Frangipani, its intensely sweet scent is strongest at night to attract moths.",
                "Plumeria flowers are traditionally used to weave Hawaiian leis."
            )
        ),
        PlantTemplate(
            name = "Cypress Tree",
            type = "Tree",
            careSpring = "Prune lightly to maintain symmetrical conical shape.",
            careSummer = "Water occasionally. Very drought tolerant.",
            careAutumn = "Mulch around base to protect root system.",
            careWinter = "Extremely winter hardy, evergreen foliage.",
            soilType = "Stony, well-draining sandy loam",
            sunlight = "Full Sun",
            iconEmoji = "🌲",
            matureSize = "20 - 40 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Non-flowering",
            pestsDiseases = "Spider mites, Cypress aphids",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "Cypress wood is exceptionally durable; the doors of St. Peter's Basilica in Rome lasted over 1,000 years.",
                "Ancient Romans planted cypress trees to protect homes from bad energy and welcome friends.",
                "Cypress trees can live for thousands of years; the oldest known is over 4,000 years old."
            )
        ),
        PlantTemplate(
            name = "Lavender",
            type = "Herb",
            careSpring = "Prune back by one-third to promote fresh dense growth.",
            careSummer = "Water occasionally. Does not like high humidity.",
            careAutumn = "Cut off spent flower spikes.",
            careWinter = "Ensure perfect drainage to prevent winter root-rot.",
            soilType = "Poor, dry, gravelly loam",
            sunlight = "Full Direct Sun",
            iconEmoji = "🪻",
            matureSize = "2 - 3 feet tall & wide",
            wateringNeeds = "Low",
            bloomTime = "Summer (June - August)",
            pestsDiseases = "Root rot, Spittlebugs",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "Lavender has been used for over 2,500 years as a natural remedy for anxiety and sleep disorders.",
                "The name comes from Latin 'lavare,' which means 'to wash,' as Romans used it in their baths.",
                "Lavender flowers are fully edible and commonly used in teas, desserts, and spices."
            )
        ),
        PlantTemplate(
            name = "English Lavender",
            type = "Flower",
            careSpring = "Water weekly, prune after first flowering flush.",
            careSummer = "Loves full sun and hot weather. Reduce water.",
            careAutumn = "Cut back spent flower stalks to maintain shape.",
            careWinter = "Cold hardy, requires little to no winter watering.",
            soilType = "Sandy, well-draining, slightly alkaline",
            sunlight = "Full Sun",
            iconEmoji = "🪻",
            matureSize = "1 - 3 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Summer",
            pestsDiseases = "Root rot, Spittlebugs",
            compatibleClimate = "Temperate Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "Lavender belongs to the mint family Lamiaceae.",
                "Its scent is widely used in aromatherapy to reduce anxiety and improve sleep."
            )
        ),
        PlantTemplate(
            name = "Bougainvillea",
            type = "Flower",
            careSpring = "Fertilize to trigger bright pink bracts.",
            careSummer = "Water weekly. Train vines on fence or trellis.",
            careAutumn = "Trim trailing ends lightly.",
            careWinter = "Protect from cold window drafts. Reduce watering.",
            soilType = "Well-drained acidic soil",
            sunlight = "Full Hot Sun",
            iconEmoji = "🌸",
            matureSize = "10 - 30 feet climbing",
            wateringNeeds = "Moderate",
            bloomTime = "Year-round in warm cycles",
            pestsDiseases = "Caterpillars, Leaf miners",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "The vibrant magenta petals are actually leaf-like bracts; the true flowers are tiny white tubes inside.",
                "Discovered in 1768 in Brazil by French botanist Philibert Commerson, named after explorer Louis de Bougainville.",
                "It is highly salt-tolerant, making it a favorite decorative plant for coastal homes."
            )
        ),
        PlantTemplate(
            name = "Jacaranda Tree",
            type = "Tree",
            careSpring = "Water regularly to promote early spring purple blooms.",
            careSummer = "Drought tolerant once established. Water bi-weekly.",
            careAutumn = "Clean fallen leaf leaflets to keep soil aerated.",
            careWinter = "Goes deciduous, protect young trees from deep freeze.",
            soilType = "Sandy, well-drained fertile soil",
            sunlight = "Full Sun",
            iconEmoji = "🌳",
            matureSize = "15 - 30 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Late Spring (May - June)",
            pestsDiseases = "Aphids, Mushroom root rot",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "Jacaranda trees turn entire cities purple when they bloom in late spring.",
                "According to college lore, if a jacaranda blossom falls on your head, you will pass all your exams.",
                "It is native to South America but has been planted globally in warm climates as an ornamental tree."
            )
        ),
        PlantTemplate(
            name = "Mealy Cup Sage",
            type = "Flower",
            careSpring = "Cut back old stalks to spark ground shoots.",
            careSummer = "Loves sunny flowerbed edges. Water when topsoil is bone dry.",
            careAutumn = "Collect dried seedpods for propagate cycles.",
            careWinter = "Mulch root crowns generously to resist freezes.",
            soilType = "Average, dry to medium wet",
            sunlight = "Full Sun",
            iconEmoji = "🩵",
            matureSize = "1.5 - 3 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Summer to Autumn",
            pestsDiseases = "Whiteflies, Powdery mildew",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "The name 'Mealy Cup' comes from the powdery white hairs that coat the plant's calyx.",
                "It is a magnet for native bumblebees, honeybees, and beneficial butterflies.",
                "Its aromatic leaves release soothing essential oils in sunny indoor windows."
            )
        ),
        PlantTemplate(
            name = "Olive Tree",
            type = "Tree",
            careSpring = "Prune center branches to let light in. Water weekly.",
            careSummer = "Water bi-weekly. Extremely drought tolerant.",
            careAutumn = "Collect ripe olives. Apply light compost.",
            careWinter = "Very cold-resistant, evergreen foliage.",
            soilType = "Poor, dry, gravelly limestone",
            sunlight = "Full Direct Sun",
            iconEmoji = "🫒",
            matureSize = "15 - 30 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Spring",
            pestsDiseases = "Olive fruit fly, peacock spot",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "Olive branches are a global symbol of peace, originating in ancient Greece.",
                "Some olive trees in the Mediterranean are verified to be over 2,000 years old."
            )
        ),
        PlantTemplate(
            name = "Sweet Fig Tree",
            type = "Tree",
            careSpring = "Mulch root base. Prune leggy winter stems.",
            careSummer = "Water deeply twice weekly when topsoil dries.",
            careAutumn = "Prune woody branch tips. Protect roots.",
            careWinter = "Goes dormant, drops leaves. Can withstand freezes.",
            soilType = "Rich loamy soil",
            sunlight = "Full Sun",
            iconEmoji = "🫒",
            matureSize = "10 - 20 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Late Spring to Summer",
            pestsDiseases = "Fig rust, Mosaic virus",
            compatibleClimate = "Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "Fig trees do not have visible blossoms; the flowers actually bloom *inside* the hollow fruit.",
                "Fossils of cultivated figs have been found dating back to 9000 BC, predating wheat."
            )
        ),
        PlantTemplate(
            name = "California Poppy",
            type = "Flower",
            careSpring = "Sow seeds in gravelly soil. Water lightly.",
            careSummer = "Water weekly. Very drought tolerant.",
            careAutumn = "Let go to seed naturally to self-sow for next year.",
            careWinter = "Dies back in cold weather. Seeds lie dormant.",
            soilType = "Sandy, poor dry soil",
            sunlight = "Full Sun",
            iconEmoji = "🧡",
            matureSize = "8 - 12 inches tall",
            wateringNeeds = "Low",
            bloomTime = "Spring to Summer",
            pestsDiseases = "Mildew in wet soil",
            compatibleClimate = "Arid (Desert) Mediterranean",
            isIndoor = true,
            funFacts = listOf(
                "It is the official state flower of California, celebrating its golden orange color.",
                "Its petals close at night and during cloudy or rainy weather."
            )
        ),
        PlantTemplate(
            name = "Columbine",
            type = "Flower",
            careSpring = "Sow seeds directly. Keep cool and damp.",
            careSummer = "Mulch roots to keep them cool. Deadhead old blooms.",
            careAutumn = "Cut foliage down to ground level.",
            careWinter = "Requires winter chilling to bloom next year.",
            soilType = "Moist, sandy gravel",
            sunlight = "Partial Afternoon Shade",
            iconEmoji = "🪻",
            matureSize = "1.5 - 2 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Mid Spring to Early Summer",
            pestsDiseases = "Leaf miners, Aphids",
            compatibleClimate = "Mountainous",
            isIndoor = true,
            funFacts = listOf(
                "The name Columbine is derived from Latin 'columba,' meaning 'dove,' because the flower resembles five doves.",
                "It is the state flower of Colorado and thrives in rocky mountain soils.",
                "The long spurs on the back of the flower contain sweet nectar that only long-tongued moths can reach."
            )
        ),
        PlantTemplate(
            name = "Alpine Aster",
            type = "Flower",
            careSpring = "Divide clumps if too dense. Keep damp.",
            careSummer = "Water weekly. Deadhead for prolonged flowering.",
            careAutumn = "Mulch deeply with pine needles.",
            careWinter = "Snow cover acts as natural insulator.",
            soilType = "Gritty, neutral alpine soil",
            sunlight = "Full Sun",
            iconEmoji = "🌼",
            matureSize = "6 - 12 inches tall",
            wateringNeeds = "Moderate",
            bloomTime = "Late Summer",
            pestsDiseases = "Slugs, Powdery mildew",
            compatibleClimate = "Mountainous",
            isIndoor = true,
            funFacts = listOf(
                "Derived from the Greek word for 'star,' referring to the flower's radiating purple petals.",
                "It grows natively above the tree line in extreme wind, rocky terrains, and chilly climates.",
                "In ancient times, people burned aster leaves to repel evil spirits and snakes."
            )
        ),
        PlantTemplate(
            name = "Creeping Thyme",
            type = "Herb",
            careSpring = "Shear lightly to start fresh growth.",
            careSummer = "Very low water. Loves poor mountain soils.",
            careAutumn = "Do not fertilize. Move to bright indoor location.",
            careWinter = "High cold resistance under snow cover.",
            soilType = "Sandy, rocky soil",
            sunlight = "Full Sun",
            iconEmoji = "🌿",
            matureSize = "2 - 4 inches tall mat",
            wateringNeeds = "Low",
            bloomTime = "Early Summer",
            pestsDiseases = "Root rot in soggy environments",
            compatibleClimate = "Mountainous",
            isIndoor = true,
            funFacts = listOf(
                "Creeping thyme forms a dense, springy green mat that releases a sweet, herbal scent when stepped on.",
                "It was used by ancient Greeks in their baths and temples to symbolize courage.",
                "It acts as an excellent, low-maintenance organic turf replacement that requires zero mowing."
            )
        ),
        PlantTemplate(
            name = "Edelweiss",
            type = "Flower",
            careSpring = "Likes cool, moist, sandy pockets.",
            careSummer = "Water sparingly. Felted leaf coating prevents sunburn.",
            careAutumn = "Clean ground twigs, let soil drain perfectly.",
            careWinter = "Thrives in extreme subzero cold with low humidity.",
            soilType = "Rocky limestone soils",
            sunlight = "Full Sun",
            iconEmoji = "❄️",
            matureSize = "6 - 12 inches tall",
            wateringNeeds = "Low",
            bloomTime = "Mid Summer",
            pestsDiseases = "Root damp rot, crown mildew",
            compatibleClimate = "Mountainous",
            isIndoor = true,
            funFacts = listOf(
                "Its velvet-like white hairs act as natural sunblock and insulator against high-altitude UV radiation.",
                "In the Swiss Alps, giving an edelweiss flower is a traditional symbol of daring, deep love and devotion.",
                "It is strictly protected by law in European alpine regions to prevent extinction."
            )
        ),
        PlantTemplate(
            name = "Alpine Gentian",
            type = "Flower",
            careSpring = "Keep soil cool and damp. Protect roots.",
            careSummer = "Water weekly. Prefers cooler afternoon shade.",
            careAutumn = "Cut down dead flower stalks. Mulch.",
            careWinter = "Requires cool room temperatures; shield from heat vents.",
            soilType = "Sandy, humus-rich acidic soil",
            sunlight = "Morning Sun / Afternoon Shade",
            iconEmoji = "💙",
            matureSize = "4 - 8 inches tall",
            wateringNeeds = "Moderate to High",
            bloomTime = "Late Spring to Summer",
            pestsDiseases = "Slugs, Snails",
            compatibleClimate = "Mountainous",
            isIndoor = true,
            funFacts = listOf(
                "Famous for its intense, pure electric-blue bell-shaped blossoms.",
                "Gentian root extract has been used for centuries to brew traditional digestive bitters."
            )
        ),
        PlantTemplate(
            name = "Snowdrop",
            type = "Flower",
            careSpring = "Enjoy early blooms! Keep soil moist.",
            careSummer = "Bulbs go dormant in summer. Keep soil cool.",
            careAutumn = "Plant bulbs in groups. Apply mulch.",
            careWinter = "Blooms early in cool bright indoor sunrooms.",
            soilType = "Moist, humus-rich woodland soil",
            sunlight = "Dappled Shade",
            iconEmoji = "🤍",
            matureSize = "4 - 6 inches tall",
            wateringNeeds = "Moderate",
            bloomTime = "Late Winter (February - March)",
            pestsDiseases = "Narcissus bulb fly",
            compatibleClimate = "Mountainous Temperate",
            isIndoor = true,
            funFacts = listOf(
                "One of the first flowers to emerge in late winter, signaling the start of spring.",
                "Its bulbs contain an organic antifreeze protein that prevents ice crystals from breaking cells."
            )
        ),
        PlantTemplate(
            name = "Heather",
            type = "Shrub",
            careSpring = "Trim branch tips lightly to maintain shape.",
            careSummer = "Water weekly. Mulch with pine bark.",
            careAutumn = "Fiery pink/purple blossom show. Do not fertilize.",
            careWinter = "Extremely cold hardy, holds evergreen look.",
            soilType = "Sandy, highly acidic peat loam",
            sunlight = "Full Sun",
            iconEmoji = "🪻",
            matureSize = "1 - 2 feet tall & wide",
            wateringNeeds = "Moderate",
            bloomTime = "Late Summer to Autumn",
            pestsDiseases = "Powdery mildew, root rot",
            compatibleClimate = "Mountainous Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Heather is highly resilient and thrives on wild, wind-swept moorlands.",
                "Traditionally used in Scotland to make brooms, thatch roofs, and flavor ale."
            )
        ),
        PlantTemplate(
            name = "Fuchsia",
            type = "Flower",
            careSpring = "Prune woody stems. Keep soil damp.",
            careSummer = "Water deeply twice weekly. Mist in dry heat.",
            careAutumn = "Cut back branches lightly. Mulch root zone.",
            careWinter = "In severe freeze climates, bring root container indoors.",
            soilType = "Rich, moist, well-draining soil",
            sunlight = "Partial Shade / Filtered Light",
            iconEmoji = "🌺",
            matureSize = "2 - 4 feet trailing/bush",
            wateringNeeds = "High",
            bloomTime = "Summer to Autumn",
            pestsDiseases = "Whiteflies, Fuchsia rust",
            compatibleClimate = "Mountainous Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Its hanging, bell-shaped flowers are shaped like teardrops, loved by hummingbirds.",
                "Named after Leonhart Fuchs, an influential 16th-century German botanist."
            )
        ),
        PlantTemplate(
            name = "English Rose",
            type = "Flower",
            careSpring = "Apply rich rose food. Prune dead branches from winter.",
            careSummer = "Water deeply twice a week at the base. Avoid wetting leaves.",
            careAutumn = "Clean fallen foliage, apply winter compost layer.",
            careWinter = "Prune woody stalks down. Cover roots with thick mulch cover.",
            soilType = "Rich organic clay loam",
            sunlight = "Full Sun (6+ hours)",
            iconEmoji = "🌹",
            matureSize = "3 - 5 feet tall & wide",
            wateringNeeds = "High (Deep watering)",
            bloomTime = "Late Spring to Autumn",
            pestsDiseases = "Black spot, Aphids, Beetles",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Roses are one of the oldest species of plants, with fossils dating back 35 million years.",
                "The rose hip (fruit) contains more vitamin C than an equivalent weight of oranges.",
                "In ancient Rome, roses were hung from ceilings during confidential meetings to declare secrecy."
            )
        ),
        PlantTemplate(
            name = "Japanese Maple",
            type = "Tree",
            careSpring = "Monitor for leaf scorch, apply leaf mold.",
            careSummer = "Keep root zone moist with regular mulch.",
            careAutumn = "Incredible fiery foliage show. Collect seeds.",
            careWinter = "Prune only during absolute dormancy.",
            soilType = "Slightly acidic, moisture-retentive",
            sunlight = "Dappled Shade",
            iconEmoji = "🍁",
            matureSize = "8 - 15 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Spring (Inconspicuous)",
            pestsDiseases = "Scale wood bugs, Verticillium wilt",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Japanese maples are highly prized in the art of Bonsai, representing visual elegance and peace.",
                "In Japan, viewing autumn maple leaves is a cultural tradition called 'Momijigari.'",
                "The scientific genus name 'Acer' comes from the Latin word for 'sharp,' referring to the pointed leaves."
            )
        ),
        PlantTemplate(
            name = "English Ivy",
            type = "Shrub",
            careSpring = "Prune trailing vines to control spread. Mist foliage.",
            careSummer = "Keep in dappled or partial shade. Water twice a week.",
            careAutumn = "Clean ground twigs, maintain trailing boundaries.",
            careWinter = "Evergreen vine. Minimize watering in cold seasons.",
            soilType = "Loamy, moisture-retentive soil",
            sunlight = "Dappled Shade",
            iconEmoji = "🌿",
            matureSize = "1 - 2 feet tall, 10 - 20 feet trailing",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering",
            pestsDiseases = "Spider mites, Leaf spot bacteria",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Ivy vines climb using tiny clinging rootlets that secrete a strong organic glue.",
                "In ancient Greece, ivy was dedicated to Dionysus and believed to prevent alcohol intoxication.",
                "English ivy remains fully evergreen even during the destruction of temperate winters."
            )
        ),
        PlantTemplate(
            name = "Bonsai Juniper",
            type = "Tree",
            careSpring = "Pinch back new foliage shoots. Water regularly.",
            careSummer = "Ensure bright indirect sunlight. Water when topsoil feels dry.",
            careAutumn = "Clean dried needles, prepare root zones for dormancy.",
            careWinter = "Keep near bright indoor window or cool room.",
            soilType = "Fast-draining bonsai soil mix",
            sunlight = "Full Sun to Partial Shade",
            iconEmoji = "🪴",
            matureSize = "Small (1-2 ft)",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering",
            pestsDiseases = "Juniper scale, Cedar-apple rust",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Juniper Bonsai can live for hundreds of years under careful wiring and shaping care.",
                "Junipers secrete phytoncides, airborne chemicals that reduce human stress and blood pressure.",
                "The word 'Bonsai' literally translates to 'planted in a tray' in Japanese."
            )
        ),
        PlantTemplate(
            name = "Hydrangea",
            type = "Flower",
            careSpring = "Water weekly. Apply organic mulch around base.",
            careSummer = "Water deeply twice weekly. Loves consistent damp soil.",
            careAutumn = "Trim faded large flower heads.",
            careWinter = "Prune wood stems down, mulch root base generously.",
            soilType = "Rich, moist, acidic or alkaline loam",
            sunlight = "Morning Sun / Afternoon Shade",
            iconEmoji = "🪻",
            matureSize = "3 - 5 feet tall & wide",
            wateringNeeds = "High",
            bloomTime = "Summer",
            pestsDiseases = "Powdery mildew, Aphids",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "The flower color changes dynamically based on soil pH: acidic soil yields blue, alkaline yields pink.",
                "The name Hydrangea comes from Greek 'hydor' (water) and 'angos' (vessel) due to its thirst."
            )
        ),
        PlantTemplate(
            name = "Peonies",
            type = "Flower",
            careSpring = "Support tall stems with rings. Water weekly.",
            careSummer = "Water deeply. Deadhead spent blooms.",
            careAutumn = "Prune spent stems down as foliage yellowing occurs.",
            careWinter = "Extremely cold hardy bulb, needs winter chill to bloom.",
            soilType = "Rich, deep, moist well-draining soil",
            sunlight = "Full Sun",
            iconEmoji = "🌸",
            matureSize = "2 - 3 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Late Spring to Early Summer",
            pestsDiseases = "Botrytis blight, Ants on buds",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Peonies can live and bloom for over 100 years in the same spot without needing division.",
                "Ants on peony buds are beneficial; they eat sweet nectar in exchange for protecting buds from pests."
            )
        ),
        PlantTemplate(
            name = "Sunflower",
            type = "Flower",
            careSpring = "Sow seeds deeply in full sun. Water weekly.",
            careSummer = "Water deeply twice weekly during bloom. Staking may be needed.",
            careAutumn = "Prune spent flower heads as needed.",
            careWinter = "Annual plant, dies after seed cycle. Save seeds for spring.",
            soilType = "Deep, loose, fertile soil",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌻",
            matureSize = "6 - 10 feet tall",
            wateringNeeds = "Moderate to High",
            bloomTime = "Summer to Early Autumn",
            pestsDiseases = "Downy mildew, Sunflower moths",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Young sunflowers exhibit heliotropism, tracing the sun's path from east to west daily.",
                "A single sunflower head is actually composed of thousands of tiny individual flowers blooming together."
            )
        ),
        PlantTemplate(
            name = "Marigold",
            type = "Flower",
            careSpring = "Sow seeds in indoor starter pots. Water lightly to sprout.",
            careSummer = "Water weekly. Deadhead old blooms regularly.",
            careAutumn = "Collect dried seed heads for spring sowing.",
            careWinter = "Annual plant, dies off. Mulch bed to enrich soil.",
            soilType = "Average, well-draining soil",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌼",
            matureSize = "1 - 2 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Spring to Autumn",
            pestsDiseases = "Spider mites, Slugs",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Marigolds secrete a chemical (alpha-terthienyl) that repels harmful root nematodes.",
                "They are widely used in indoor companion arrangements to naturally protect nearby houseplants."
            )
        ),
        PlantTemplate(
            name = "Snapdragon",
            type = "Flower",
            careSpring = "Plant early in cool soil. Water weekly.",
            careSummer = "Deadhead old spikes to trigger a second autumn bloom.",
            careAutumn = "Enjoys cool autumn weather. Collect seedpods.",
            careWinter = "Cut back. Often behaves as a short-lived perennial.",
            soilType = "Rich organic, well-draining loam",
            sunlight = "Full Sun to Light Shade",
            iconEmoji = "🦁",
            matureSize = "1.5 - 3 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Spring and Autumn (prefers cool weather)",
            pestsDiseases = "Snapdragon rust, Aphids",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Squeezing the sides of the flower gently makes the 'mouth' snap open and shut like a dragon.",
                "When the flowers die, the dried seed pods resemble tiny, detailed human skulls."
            )
        ),
        PlantTemplate(
            name = "Hostas",
            type = "Shrub",
            careSpring = "Apply compost around crowns. Protect from slugs.",
            careSummer = "Water weekly. Keep shaded. Cut tall bloom spikes.",
            careAutumn = "Cut foliage down to the base as leaves turn yellow.",
            careWinter = "Requires winter dormancy. High cold resistance.",
            soilType = "Rich, moist, slightly acidic loam",
            sunlight = "Dappled to Deep Shade",
            iconEmoji = "🌿",
            matureSize = "1.5 - 3 feet wide",
            wateringNeeds = "Moderate to High",
            bloomTime = "Mid Summer",
            pestsDiseases = "Slugs, Snails, Hosta Virus X",
            compatibleClimate = "Temperate",
            isIndoor = true,
            funFacts = listOf(
                "Highly prized for their large, beautifully variegated green and white leaves rather than flowers.",
                "Native to Northeast Asia, where it thrives as a resilient indoor foliage plant."
            )
        )
    )
    
    fun getTemplatesForClimate(climate: String): List<PlantTemplate> {
        return ALL_TEMPLATES.filter { it.isIndoor && it.compatibleClimate.contains(climate, ignoreCase = true) }
    }

    fun getTemplatesForPlanner(climate: String, isIndoor: Boolean = true): List<PlantTemplate> {
        return ALL_TEMPLATES.filter { it.isIndoor }.sortedWith(compareByDescending { 
            it.compatibleClimate.contains(climate, ignoreCase = true)
        })
    }
}
