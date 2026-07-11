from scripts.ship import ship

ship(
    title="🎨 Palette: [UX improvement] Add ARIA labels to Dashboard WeatherSyncCard icons",
    description="""
💡 What: Added missing `contentDescription` attributes to the Thermostat and WaterDrop icons in the `WeatherSyncCard.kt` component on the dashboard.
🎯 Why: These icons previously had `contentDescription = null`, meaning screen readers would skip over them. Adding specific descriptions ensures users relying on assistive technologies understand what the numeric values (Temperature and Humidity) represent, making the weather display fully accessible.
📸 Before/After: Visuals remain unchanged. Screen readers will now announce "Temperature [Value]°F" instead of just "[Value]°F".
♿ Accessibility: Improved screen reader compatibility by providing vital context to previously decorative-only icons.
"""
)
