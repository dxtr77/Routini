# Import/Export Routines Plan

## 1. Data Structure (`RoutineExportData`)
We need a serializable data structure to hold the routines and their tasks.
```kotlin
data class RoutineExportData(
    val version: Int = 1, // For future compatibility
    val timestamp: Long = System.currentTimeMillis(),
    val routines: List<RoutineWithTasks>
)

data class RoutineWithTasks(
    val routine: Routine,
    val tasks: List<RoutineTask>
)
```

## 2. UI Hook
Add a "More Options" menu to the `RoutinesScreen`.
- **Import Routines**: Opens file picker.
- **Export Routines**: Opens file saver/share sheet.

## 3. Export Flow
1. **User Action**: User clicks "Export Routines".
2. **Data-Gathering**: `MainViewModel` fetches all routines and fetch their tasks.
3. **Serialization**: Convert `RoutineExportData` object to JSON string using Gson.
4. **File Output**:
   - Use `ActivityResultContracts.CreateDocument` to let user pick a save location.
   - Filename default: `routini_backup_YYYYMMDD.json`.
   - Write the JSON string to the OutputStream of the returned URI.
5. **Feedback**: Show Snackbar "Export Successful".

## 4. Import Flow
1. **User Action**: User clicks "Import Routines".
2. **File Input**:
   - Use `ActivityResultContracts.OpenDocument` to let user pick a JSON file.
   - MIME type: `application/json`.
3. **Parsing**: Read InputStream and parse JSON string back to `RoutineExportData` using Gson.
4. **Resiliency**: catch `JsonSyntaxException` if file is invalid.
5. **Merging/Handling**:
   - For each imported routine:
     - Create a **NEW** `Routine` entity (ignore the ID from JSON to avoid conflicts, let Room generate a new one). Copy name, days, etc.
     - Insert the new Routine -> gets `newRoutineId`.
     - For each task in that routine:
       - Create a **NEW** `RoutineTask` entity (ignore ID).
       - Set `routineId = newRoutineId`.
       - Insert task.
       - Schedule alarm if `task.time` is set.
6. **Feedback**: Show Snackbar "Imported X routines successfully".

## 5. Components to Modify
- `RoutinesScreen.kt`: Add Menu, Launchers for Import/Export.
- `MainViewModel.kt`: Add functions `exportRoutines()`, `importRoutines(uri)`.
