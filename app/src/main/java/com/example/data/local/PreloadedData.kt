package com.example.data.local

import com.example.data.entity.OfficeEntity
import com.example.data.entity.ParkingSlotEntity
import com.example.data.entity.VehicleEntity

data class CarDekhoVehicleModel(
    val manufacturer: String,
    val model: String,
    val type: String, // Car or Bike
    val fuelType: String, // Petrol, Diesel, Hybrid, Electric, CNG
    val defaultColor: String,
    val availableColors: List<String>,
    val batterySpec: String,
    val rangeKm: Int,
    val powerBhp: Int,
    val priceLakhs: String,
    val bodyType: String,
    val isEV: Boolean = fuelType.equals("Electric", ignoreCase = true)
)

object PreloadedData {

    val carDekhoCatalog: List<CarDekhoVehicleModel> = listOf(
        // MARUTI SUZUKI
        CarDekhoVehicleModel(
            manufacturer = "Maruti Suzuki",
            model = "Swift",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Luster Blue",
            availableColors = listOf("Luster Blue", "Sizzling Red", "Pearl Arctic White", "Magma Grey"),
            batterySpec = "1.2L Z-Series Petrol Engine",
            rangeKm = 650,
            powerBhp = 82,
            priceLakhs = "₹ 6.49 - 9.60 Lakh",
            bodyType = "Hatchback"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Maruti Suzuki",
            model = "Brezza",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Brave Khaki",
            availableColors = listOf("Brave Khaki", "Magma Grey", "Sizzling Red", "Pearl Arctic White"),
            batterySpec = "1.5L K15C Smart Hybrid",
            rangeKm = 620,
            powerBhp = 103,
            priceLakhs = "₹ 8.34 - 14.14 Lakh",
            bodyType = "Compact SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Maruti Suzuki",
            model = "Grand Vitara",
            type = "Car",
            fuelType = "Hybrid",
            defaultColor = "Opulent Red",
            availableColors = listOf("Opulent Red", "Grandeur Grey", "Arctic White", "Midnight Black"),
            batterySpec = "1.5L Strong Hybrid Electric",
            rangeKm = 1100,
            powerBhp = 115,
            priceLakhs = "₹ 10.99 - 20.09 Lakh",
            bodyType = "Mid-Size SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Maruti Suzuki",
            model = "e Vitara",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Celestial Blue",
            availableColors = listOf("Celestial Blue", "Pearl Arctic White", "Bluish Black", "Grandeur Grey"),
            batterySpec = "61 kWh Battery Pack",
            rangeKm = 500,
            powerBhp = 172,
            priceLakhs = "₹ 20.00 - 25.00 Lakh",
            bodyType = "Electric SUV"
        ),

        // HYUNDAI
        CarDekhoVehicleModel(
            manufacturer = "Hyundai",
            model = "Creta",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Ranger Khaki",
            availableColors = listOf("Ranger Khaki", "Abyss Black", "Atlas White", "Titan Grey"),
            batterySpec = "1.5L MPi Petrol / CRDi Diesel",
            rangeKm = 720,
            powerBhp = 115,
            priceLakhs = "₹ 11.00 - 20.15 Lakh",
            bodyType = "SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Hyundai",
            model = "Venue",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Denim Blue",
            availableColors = listOf("Denim Blue", "Typhoon Silver", "Polar White", "Phantom Black"),
            batterySpec = "1.0L Turbo GDi Petrol",
            rangeKm = 640,
            powerBhp = 120,
            priceLakhs = "₹ 7.94 - 13.48 Lakh",
            bodyType = "Compact SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Hyundai",
            model = "Ioniq 5",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Gravity Gold Matte",
            availableColors = listOf("Gravity Gold Matte", "Optic White", "Midnight Black Pearl"),
            batterySpec = "72.6 kWh E-GMP Battery",
            rangeKm = 631,
            powerBhp = 217,
            priceLakhs = "₹ 46.05 Lakh",
            bodyType = "Premium Electric Crossover"
        ),

        // TATA MOTORS
        CarDekhoVehicleModel(
            manufacturer = "Tata",
            model = "Nexon",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Daytona Grey",
            availableColors = listOf("Daytona Grey", "Fearless Purple", "Creative Ocean", "Pristine White"),
            batterySpec = "1.2L Revotron Turbocharged",
            rangeKm = 650,
            powerBhp = 120,
            priceLakhs = "₹ 8.00 - 15.50 Lakh",
            bodyType = "Compact SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Tata",
            model = "Nexon EV",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Intensi-Teal",
            availableColors = listOf("Intensi-Teal", "Empowered Dark", "Daytona Grey", "Pristine White"),
            batterySpec = "45 kWh Red Dark",
            rangeKm = 489,
            powerBhp = 145,
            priceLakhs = "₹ 12.49 - 17.19 Lakh",
            bodyType = "Compact SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Tata",
            model = "Curvv EV",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Empowered Oxide",
            availableColors = listOf("Empowered Oxide", "Pure Grey", "Flame Red", "Pristine White"),
            batterySpec = "55 kWh Long Range",
            rangeKm = 585,
            powerBhp = 167,
            priceLakhs = "₹ 17.49 - 21.99 Lakh",
            bodyType = "Coupe SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Tata",
            model = "Harrier",
            type = "Car",
            fuelType = "Diesel",
            defaultColor = "Sunlit Yellow",
            availableColors = listOf("Sunlit Yellow", "Ash Grey", "Coral Red", "Pebble Grey"),
            batterySpec = "2.0L Kryotec Turbo Diesel",
            rangeKm = 780,
            powerBhp = 170,
            priceLakhs = "₹ 15.49 - 26.44 Lakh",
            bodyType = "Mid-Size SUV"
        ),

        // MAHINDRA
        CarDekhoVehicleModel(
            manufacturer = "Mahindra",
            model = "Scorpio-N",
            type = "Car",
            fuelType = "Diesel",
            defaultColor = "Deep Forest",
            availableColors = listOf("Deep Forest", "Everest White", "Napoli Black", "Dazzling Silver"),
            batterySpec = "2.2L mHawk CRDe Diesel",
            rangeKm = 750,
            powerBhp = 175,
            priceLakhs = "₹ 13.60 - 24.54 Lakh",
            bodyType = "SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Mahindra",
            model = "Thar",
            type = "Car",
            fuelType = "Diesel",
            defaultColor = "Red Rage",
            availableColors = listOf("Red Rage", "Napoli Black", "Galaxy Grey", "Aquamarine"),
            batterySpec = "2.2L mHawk Diesel 4x4",
            rangeKm = 600,
            powerBhp = 130,
            priceLakhs = "₹ 11.25 - 17.60 Lakh",
            bodyType = "Off-Road SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Mahindra",
            model = "BE 6e",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Firestorm Orange",
            availableColors = listOf("Firestorm Orange", "Stealth Black", "Arctic White", "Desert Gold"),
            batterySpec = "79 kWh INGLO Architecture",
            rangeKm = 682,
            powerBhp = 286,
            priceLakhs = "₹ 18.90 - 26.90 Lakh",
            bodyType = "Electric Aero-SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Mahindra",
            model = "XUV400 EV",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Arctic Blue",
            availableColors = listOf("Arctic Blue", "Everest White", "Galaxy Grey", "Napoli Black"),
            batterySpec = "39.4 kWh High Energy Density",
            rangeKm = 456,
            powerBhp = 150,
            priceLakhs = "₹ 15.49 - 19.39 Lakh",
            bodyType = "Compact SUV"
        ),

        // TOYOTA
        CarDekhoVehicleModel(
            manufacturer = "Toyota",
            model = "Innova Hycross",
            type = "Car",
            fuelType = "Hybrid",
            defaultColor = "Blackish Ageha",
            availableColors = listOf("Blackish Ageha", "Pearl White", "Silver Metallic", "Attitude Black"),
            batterySpec = "2.0L 5th Gen Strong Hybrid",
            rangeKm = 1050,
            powerBhp = 186,
            priceLakhs = "₹ 19.77 - 30.98 Lakh",
            bodyType = "MPV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Toyota",
            model = "Fortuner",
            type = "Car",
            fuelType = "Diesel",
            defaultColor = "Phantom Brown",
            availableColors = listOf("Phantom Brown", "Attitude Black", "Super White", "Silver Metallic"),
            batterySpec = "2.8L Turbo Diesel 4x4",
            rangeKm = 850,
            powerBhp = 204,
            priceLakhs = "₹ 33.43 - 51.44 Lakh",
            bodyType = "Full-Size SUV"
        ),

        // KIA
        CarDekhoVehicleModel(
            manufacturer = "Kia",
            model = "Seltos",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Pewter Olive",
            availableColors = listOf("Pewter Olive", "Gravity Grey", "Aurora Black Pearl", "Glacier White"),
            batterySpec = "1.5L Smartstream Turbo GDi",
            rangeKm = 680,
            powerBhp = 160,
            priceLakhs = "₹ 10.90 - 20.35 Lakh",
            bodyType = "Mid-Size SUV"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Kia",
            model = "EV6",
            type = "Car",
            fuelType = "Electric",
            defaultColor = "Yacht Blue",
            availableColors = listOf("Yacht Blue", "Moonscape Matte", "Snow White Pearl", "Aurora Black"),
            batterySpec = "77.4 kWh Ultra-Fast 800V",
            rangeKm = 708,
            powerBhp = 325,
            priceLakhs = "₹ 60.95 - 65.95 Lakh",
            bodyType = "Crossover GT"
        ),

        // HONDA
        CarDekhoVehicleModel(
            manufacturer = "Honda",
            model = "City",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Radiant Red Metallic",
            availableColors = listOf("Radiant Red Metallic", "Platinum White Pearl", "Lunar Silver Metallic", "Golden Brown"),
            batterySpec = "1.5L i-VTEC DOHC with VTC",
            rangeKm = 700,
            powerBhp = 121,
            priceLakhs = "₹ 12.08 - 16.35 Lakh",
            bodyType = "Sedan"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Honda",
            model = "Elevate",
            type = "Car",
            fuelType = "Petrol",
            defaultColor = "Phoenix Orange Pearl",
            availableColors = listOf("Phoenix Orange Pearl", "Obsidian Blue Pearl", "Platinum White", "Meteoroid Grey"),
            batterySpec = "1.5L i-VTEC Petrol",
            rangeKm = 640,
            powerBhp = 121,
            priceLakhs = "₹ 11.69 - 16.43 Lakh",
            bodyType = "Compact SUV"
        ),

        // BIKES & 2-WHEELERS
        CarDekhoVehicleModel(
            manufacturer = "Royal Enfield",
            model = "Classic 350",
            type = "Bike",
            fuelType = "Petrol",
            defaultColor = "Halcyon Black",
            availableColors = listOf("Halcyon Black", "Signals Desert Sand", "Dark Stealth Black", "Chrome Red"),
            batterySpec = "349cc J-Series Fuel-Injected Single",
            rangeKm = 450,
            powerBhp = 20,
            priceLakhs = "₹ 1.93 - 2.30 Lakh",
            bodyType = "Cruiser Motorcycle"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Royal Enfield",
            model = "Hunter 350",
            type = "Bike",
            fuelType = "Petrol",
            defaultColor = "Dapper Ash",
            availableColors = listOf("Dapper Ash", "Rebel Blue", "Factory Black", "Dapper White"),
            batterySpec = "349cc Single Cylinder EFI",
            rangeKm = 460,
            powerBhp = 20,
            priceLakhs = "₹ 1.50 - 1.75 Lakh",
            bodyType = "Roadster"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Hero",
            model = "Splendor Plus",
            type = "Bike",
            fuelType = "Petrol",
            defaultColor = "Black with Silver",
            availableColors = listOf("Black with Silver", "Matte Axis Grey", "Ruby Red", "Heavy Grey"),
            batterySpec = "97.2cc Air Cooled 4-stroke",
            rangeKm = 550,
            powerBhp = 8,
            priceLakhs = "₹ 0.75 - 0.80 Lakh",
            bodyType = "Commuter Bike"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Honda (2W)",
            model = "Activa 6G",
            type = "Bike",
            fuelType = "Petrol",
            defaultColor = "Decent Blue",
            availableColors = listOf("Decent Blue", "Pearl Siren Blue", "Matte Axis Grey", "Rebel Red"),
            batterySpec = "109.51cc PGM-FI Engine",
            rangeKm = 280,
            powerBhp = 8,
            priceLakhs = "₹ 0.76 - 0.82 Lakh",
            bodyType = "Scooter"
        ),
        CarDekhoVehicleModel(
            manufacturer = "TVS",
            model = "Apache RTR 160",
            type = "Bike",
            fuelType = "Petrol",
            defaultColor = "Racing Red",
            availableColors = listOf("Racing Red", "Matte Blue", "Gloss Black", "Pearl White"),
            batterySpec = "159.7cc SI 4-stroke Oil Cooled",
            rangeKm = 480,
            powerBhp = 16,
            priceLakhs = "₹ 1.20 - 1.30 Lakh",
            bodyType = "Sports Streetfighter"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Ather Energy",
            model = "450X Gen 3",
            type = "Bike",
            fuelType = "Electric",
            defaultColor = "True Red",
            availableColors = listOf("True Red", "Cosmic Black", "Salt Green", "Space Grey", "Lunar Grey"),
            batterySpec = "3.7 kWh Lithium-ion Pack",
            rangeKm = 150,
            powerBhp = 9,
            priceLakhs = "₹ 1.40 - 1.55 Lakh",
            bodyType = "Performance Smart Scooter"
        ),
        CarDekhoVehicleModel(
            manufacturer = "Ola Electric",
            model = "S1 Pro Gen 2",
            type = "Bike",
            fuelType = "Electric",
            defaultColor = "Amethyst",
            availableColors = listOf("Amethyst", "Stellar Blue", "Matt White", "Jet Black", "Midnight Blue"),
            batterySpec = "4.0 kWh Battery Unit",
            rangeKm = 195,
            powerBhp = 15,
            priceLakhs = "₹ 1.30 - 1.45 Lakh",
            bodyType = "High-Speed Electric Scooter"
        )
    )

    fun getAllManufacturers(): List<String> {
        val list = carDekhoCatalog.map { it.manufacturer }.distinct().sorted().toMutableList()
        if (!list.contains("Others")) {
            list.add("Others")
        }
        return list
    }

    fun getModelsByManufacturer(manufacturer: String): List<CarDekhoVehicleModel> {
        return carDekhoCatalog.filter { it.manufacturer.equals(manufacturer, ignoreCase = true) }
    }

    fun getSampleDeloitteOffice(): OfficeEntity {
        return OfficeEntity(
            id = 1L,
            name = "Deloitte Hyderabad",
            latitude = 17.4375,
            longitude = 78.3752,
            address = "Hitec City, Madhapur, Hyderabad, Telangana 500081",
            geofenceRadius = 150f,
            imageUri = null,
            isDefault = true
        )
    }

    fun getDeloitteEVParkingSlots(officeId: Long = 1L): List<ParkingSlotEntity> {
        val slots = mutableListOf<ParkingSlotEntity>()

        // Floor B1 EV Fast Charging Slots
        val b1Slots = listOf("252", "251", "216", "217", "160", "161", "134", "132", "133")
        b1Slots.forEach { slot ->
            slots.add(
                ParkingSlotEntity(
                    officeId = officeId,
                    floor = "B1",
                    slotNumber = slot,
                    zone = if (slot in listOf("252", "251")) "Near Lift 3" else "EV Fast Charge Alpha",
                    isEV = true,
                    notes = "Level 2 AC Fast Charger (22kW)"
                )
            )
        }

        // Floor B3 EV Slots
        val b3Slots = listOf("24", "25", "41", "42", "47", "63")
        b3Slots.forEach { slot ->
            slots.add(
                ParkingSlotEntity(
                    officeId = officeId,
                    floor = "B3",
                    slotNumber = slot,
                    zone = "Wing B Charging Bay",
                    isEV = true,
                    notes = "11kW Type 2 Connector"
                )
            )
        }

        // Floor B4 EV Slots
        val b4Slots = listOf("46", "47", "24", "25", "41", "42", "134", "144")
        b4Slots.forEach { slot ->
            slots.add(
                ParkingSlotEntity(
                    officeId = officeId,
                    floor = "B4",
                    slotNumber = slot,
                    zone = "Tower 2 Basement Hub",
                    isEV = true,
                    notes = "Dedicated EV Fast Charging Bay"
                )
            )
        }

        return slots
    }

    fun getDeloitteGeneralParkingSlots(officeId: Long = 1L): List<ParkingSlotEntity> {
        val slots = mutableListOf<ParkingSlotEntity>()

        // Floor B1 General Non-EV Slots
        listOf("101", "102", "103", "104", "105", "106", "110", "112", "115").forEach { slot ->
            slots.add(
                ParkingSlotEntity(
                    officeId = officeId,
                    floor = "B1",
                    slotNumber = slot,
                    zone = "General Bay East",
                    isEV = false,
                    notes = "Standard Parking Bay (ICE / Hybrid / 2W)"
                )
            )
        }

        // Floor B3 General Slots
        listOf("10", "11", "12", "15", "18", "20", "22").forEach { slot ->
            slots.add(
                ParkingSlotEntity(
                    officeId = officeId,
                    floor = "B3",
                    slotNumber = slot,
                    zone = "General Bay Central",
                    isEV = false,
                    notes = "Standard Parking Bay"
                )
            )
        }

        // Floor B4 General Slots
        listOf("1", "2", "5", "8", "12", "15", "18").forEach { slot ->
            slots.add(
                ParkingSlotEntity(
                    officeId = officeId,
                    floor = "B4",
                    slotNumber = slot,
                    zone = "General Bay South",
                    isEV = false,
                    notes = "Standard Parking Bay"
                )
            )
        }

        return slots
    }

    /**
     * Requirement: By default there shall be NO vehicle set to the user!
     */
    fun getSampleVehicles(): List<VehicleEntity> {
        return emptyList()
    }
}
