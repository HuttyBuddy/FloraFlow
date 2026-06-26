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
    val funFacts: List<String> = emptyList()
)

object ClimatePlants {
    val CLIMATES = listOf("Temperate", "Arid (Desert)", "Tropical (Humid)", "Mediterranean", "Mountainous")
    
    val STYLES = listOf("Indoor Area", "Cottage Garden", "English Classic", "Modern Patio", "Botanical Sanctuary")

    val ALL_TEMPLATES = listOf(
        // Arid Climate Plants
        PlantTemplate(
            name = "Saguaro Cactus",
            type = "Succulent",
            careSpring = "Water once a month. No pruning needed.",
            careSummer = "Water every 3 weeks. Full direct sun.",
            careAutumn = "Withhold watering as temperature drops.",
            careWinter = "Keep completely dry to prevent frost root rot.",
            soilType = "Perlite and sand mix",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌵",
            matureSize = "15 - 40 feet tall",
            wateringNeeds = "Low",
            bloomTime = "Late Spring (May - June)",
            pestsDiseases = "Bacterial necrosis, Mealybugs",
            compatibleClimate = "Arid (Desert)",
            funFacts = listOf(
                "Saguaro cacti can take up to 75 years to grow their first side arm.",
                "They store huge amounts of water; a fully hydrated saguaro can weigh over 3,000 pounds.",
                "Saguaro blossoms are Arizona's state flower and bloom only at night."
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
            funFacts = listOf(
                "Aloe vera has been used medicinally for over 6,000 years, beginning in ancient Egypt.",
                "It consists of 99% water, which allows it to survive in extremely arid climates.",
                "In ancient times, Egyptians called aloe the 'plant of immortality' and gave it as funeral gifts."
            )
        ),
        PlantTemplate(
            name = "Desert Marigold",
            type = "Flower",
            careSpring = "Sow seeds in shallow soil. Water lightly to sprout.",
            careSummer = "Moderate drought tolerance. Water weekly.",
            careAutumn = "Cut back old blooms to encourage new sprout cycles.",
            careWinter = "Leave dry. Survives mild frost beautifully.",
            soilType = "Dry sandy soils",
            sunlight = "Full Direct Sun",
            iconEmoji = "🌼",
            matureSize = "1 - 1.5 feet tall",
            wateringNeeds = "Low to Moderate",
            bloomTime = "Spring to Autumn",
            pestsDiseases = "Spider mites, Powdery mildew",
            compatibleClimate = "Arid (Desert)",
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
            funFacts = listOf(
                "Rosemary is historically associated with memory; ancient scholars wore it in their hair during exams.",
                "It belongs to the mint family and is highly resistant to common garden pests.",
                "The name rosemary comes from Latin 'ros marinus,' meaning 'dew of the sea.'"
            )
        ),
        PlantTemplate(
            name = "Prickly Pear Cactus",
            type = "Succulent",
            careSpring = "Minimal water. Plant in sunny bed.",
            careSummer = "Water deeply but infrequently (monthly).",
            careAutumn = "Stop fertilizer, let soil stay loose and dry.",
            careWinter = "Completely dormant. Extremely hardy to frost.",
            soilType = "Sandy, extremely gravelly soil",
            sunlight = "Full Sun",
            iconEmoji = "🌵",
            matureSize = "3 - 6 feet tall",
            wateringNeeds = "Very Low",
            bloomTime = "Late Spring",
            pestsDiseases = "Cochineal scale, Broad mites",
            compatibleClimate = "Arid (Desert)",
            funFacts = listOf(
                "Both the flat pads (nopales) and the bright red fruits (tunas) are fully edible and delicious.",
                "Prickly pear sap can be used to purify dirty drinking water organically.",
                "The plant was imported to Australia in the 1800s and took over millions of acres of farmland."
            )
        ),

        // Tropical Climate Plants
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
            funFacts = listOf(
                "The unique flower shape resembles a colorful bird in flight, designed to attract pollinating birds.",
                "In Hawaii, the flower is considered a symbol of magnificence and royalty.",
                "It is closely related to the banana plant and shares similar large, glossy leaves."
            )
        ),
        PlantTemplate(
            name = "Orchid",
            type = "Flower",
            careSpring = "Mist roots daily. Feed with diluted orchid fertilizer.",
            careSummer = "Water weekly with misting. Avoid direct intense sun.",
            careAutumn = "Ensure temperature drop at night to spike new blooms.",
            careWinter = "Water every 10 days, protect from cold drafts.",
            soilType = "Fir bark and sphagnum moss",
            sunlight = "Filtered Bright Light",
            iconEmoji = "🌸",
            matureSize = "1 - 1.5 feet tall",
            wateringNeeds = "Moderate",
            bloomTime = "Winter to Spring",
            pestsDiseases = "Aphids, Crown rot",
            compatibleClimate = "Tropical (Humid)",
            funFacts = listOf(
                "Orchids represent the largest family of flowering plants, with over 25,000 species.",
                "Vanilla flavoring is derived directly from the seed pods of the Vanilla Orchid.",
                "Orchid seeds are microscopic, resembling fine dust, with a single pod containing millions."
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
            bloomTime = "Summer (Rare indoors)",
            pestsDiseases = "Fungus gnats, Spider mites",
            compatibleClimate = "Tropical (Humid)",
            funFacts = listOf(
                "The fenestrations (holes) in its leaves allow jungle winds and light to pass through without tearing them.",
                "The name Deliciosa refers to the delicious edible fruit it produces in the wild.",
                "In Chinese culture, the Monstera is a symbol of long life and honoring elders."
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
            funFacts = listOf(
                "Nicknamed 'Devil's Ivy' because it is nearly impossible to kill and stays green even in the dark.",
                "It is a highly efficient air purifier, removing airborne toxins like formaldehyde and carbon monoxide.",
                "Pothos vines can grow up to 40 feet long in tropical forests."
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
            funFacts = listOf(
                "The showy red 'flower' is actually a series of colored bracts; the true flower is small and white inside.",
                "It is the national flower of Samoa, known locally as 'teuila.'",
                "It can spread rapidly in humid rainforests via underground rhizomes."
            )
        ),

        // Mediterranean Climate Plants
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
            funFacts = listOf(
                "Lavender has been used for over 2,500 years as a natural remedy for anxiety and sleep disorders.",
                "The name comes from Latin 'lavare,' which means 'to wash,' as Romans used it in their baths.",
                "Lavender flowers are fully edible and commonly used in teas, desserts, and spices."
            )
        ),
        PlantTemplate(
            name = "Bougainvillea",
            type = "Flower",
            careSpring = "Fertilize to trigger bright pink bracts.",
            careSummer = "Water weekly. Train vines on fence or trellis.",
            careAutumn = "Trim trailing ends lightly.",
            careWinter = "Protect from frost. Reduce watering.",
            soilType = "Well-drained acidic soil",
            sunlight = "Full Hot Sun",
            iconEmoji = "🌸",
            matureSize = "10 - 30 feet climbing",
            wateringNeeds = "Moderate",
            bloomTime = "Year-round in warm cycles",
            pestsDiseases = "Caterpillars, Leaf miners",
            compatibleClimate = "Mediterranean",
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
            funFacts = listOf(
                "The name 'Mealy Cup' comes from the powdery white hairs that coat the plant's calyx.",
                "It is a magnet for native bumblebees, honeybees, and beneficial butterflies.",
                "Its aromatic leaves act as a natural deer and rabbit repellent in Mediterranean gardens."
            )
        ),

        // Mountainous Climate Plants
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
            careAutumn = "Do not fertilize. Prepare for frost.",
            careWinter = "High cold resistance under snow cover.",
            soilType = "Sandy, rocky soil",
            sunlight = "Full Sun",
            iconEmoji = "🌿",
            matureSize = "2 - 4 inches tall mat",
            wateringNeeds = "Low",
            bloomTime = "Early Summer",
            pestsDiseases = "Root rot in soggy environments",
            compatibleClimate = "Mountainous",
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
            funFacts = listOf(
                "Its velvet-like white hairs act as natural sunblock and insulator against high-altitude UV radiation.",
                "In the Swiss Alps, giving an edelweiss flower is a traditional symbol of daring, deep love and devotion.",
                "It is strictly protected by law in European alpine regions to prevent extinction."
            )
        ),

        // Temperate Climate Plants
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
            bloomTime = "Late Spring to Frost",
            pestsDiseases = "Black spot, Aphids, Beetles",
            compatibleClimate = "Temperate",
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
            funFacts = listOf(
                "Ivy vines climb using tiny clinging rootlets that secrete a strong organic glue.",
                "In ancient Greece, ivy was dedicated to Dionysus and believed to prevent alcohol intoxication.",
                "English ivy remains fully evergreen even during the harshest temperate winters."
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
            compatibleClimate = "Temperate",
            funFacts = listOf(
                "Boston Ferns were highly popular in the Victorian era, occupying pride of place in parlor windows.",
                "They are excellent humidifiers, releasing moisture and restoring humidity to dry indoor spaces.",
                "Ferns reproduce using microscopic spores on the undersides of their leaves rather than seeds."
            )
        ),
        PlantTemplate(
            name = "Bonsai Juniper",
            type = "Tree",
            careSpring = "Pinch back new foliage shoots. Water regularly.",
            careSummer = "Ensure bright indirect sunlight. Water when topsoil feels dry.",
            careAutumn = "Clean dried needles, prepare root zones for dormancy.",
            careWinter = "Extremely cold hardy bonsai. Keep outdoors or cool porch.",
            soilType = "Fast-draining bonsai soil mix",
            sunlight = "Full Sun to Partial Shade",
            iconEmoji = "🪴",
            matureSize = "Small (1-2 ft)",
            wateringNeeds = "Moderate",
            bloomTime = "Non-flowering",
            pestsDiseases = "Juniper scale, Cedar-apple rust",
            compatibleClimate = "Temperate",
            funFacts = listOf(
                "Juniper Bonsai can live for hundreds of years under careful wiring and shaping care.",
                "Junipers secrete phytoncides, airborne chemicals that reduce human stress and blood pressure.",
                "The word 'Bonsai' literally translates to 'planted in a tray' in Japanese."
            )
        )
    )
    
    fun getTemplatesForClimate(climate: String): List<PlantTemplate> {
        return ALL_TEMPLATES.filter { it.compatibleClimate.contains(climate, ignoreCase = true) }
    }
}
