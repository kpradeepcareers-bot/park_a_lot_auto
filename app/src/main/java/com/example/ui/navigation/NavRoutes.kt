package com.example.ui.navigation

sealed class NavRoutes(val route: String) {
    object Onboarding : NavRoutes("onboarding")
    object Home : NavRoutes("home")
    object ParkVehicle : NavRoutes("park_vehicle")
    object FindVehicle : NavRoutes("find_vehicle")
    object OfficeParking : NavRoutes("office_parking")
    object SavedLocations : NavRoutes("saved_locations")
    object MyVehicles : NavRoutes("my_vehicles")
    object Vehicle360 : NavRoutes("vehicle_360")
    object ParkingHistory : NavRoutes("parking_history")
    object Settings : NavRoutes("settings")
}
