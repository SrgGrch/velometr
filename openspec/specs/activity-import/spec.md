# activity-import Specification

## Purpose

Ingests the user's manually downloaded Strava bulk-export archive into the persistent activity store, so the app's data stays in sync with Strava without a paid API dependency, and keeps repeated imports safe and duplicate-free.

## Requirements

### Requirement: Zip archive selection
The system SHALL let the user select a single zip archive for import via drag-and-drop or a file picker, and SHALL only enable the import action once a file is selected.

#### Scenario: Valid file dropped or selected
- **WHEN** the user drags a zip file onto the import dropzone, or selects one via the file picker
- **THEN** the selected file's name and size are shown and the import action becomes enabled

#### Scenario: No file selected
- **WHEN** the import modal is open and no file has been selected
- **THEN** the import action stays disabled

#### Scenario: Selection cleared before confirming
- **WHEN** the user removes the selected file before confirming import
- **THEN** the selection is cleared and the import action becomes disabled again

### Requirement: Archive parsing
The system SHALL unzip the uploaded archive and parse the `activities.csv` file it contains as the source of activity records.

#### Scenario: Archive contains activities.csv
- **WHEN** an uploaded archive is processed
- **THEN** each row of its `activities.csv` is parsed into a candidate activity record (id, date, title, distance, duration, average speed, max speed)

### Requirement: Idempotent import by activity id
The system SHALL use the Strava activity id as the deduplication key: an activity id already present in the store is skipped, and only new activity ids are inserted.

#### Scenario: Same archive imported twice
- **WHEN** the same archive is imported a second time
- **THEN** no duplicate activity records are created

#### Scenario: Archive with partially new activities
- **WHEN** an archive contains a mix of already-imported and new activity ids
- **THEN** only the activities with new ids are added to the store

### Requirement: Track file storage
When a track file (`.gpx` or `.fit.gz`) in the archive corresponds to an imported activity, the system SHALL gzip-compress it and store it as a BLOB associated with that activity's record.

#### Scenario: Matching track file present
- **WHEN** an activity being imported has a corresponding track file in the archive
- **THEN** the track file is compressed and stored linked to that activity's record

#### Scenario: No matching track file
- **WHEN** an activity being imported has no corresponding track file in the archive
- **THEN** the activity is still imported, with no track data stored for it

### Requirement: Statistics sourced from CSV fields
The system SHALL use the distance, duration, average speed, and max speed values from `activities.csv` as-is, and SHALL NOT recompute them from track file points.

#### Scenario: Activity has both CSV stats and a track file
- **WHEN** an activity is imported with both `activities.csv` fields and a track file
- **THEN** the stored distance, duration, and speed values are the ones from `activities.csv`, not values derived from the track

### Requirement: Track data excluded from list and stats responses
List and statistics endpoints SHALL NOT include the stored track BLOB in their responses.

#### Scenario: Client requests activity list or stats
- **WHEN** a client calls a list or statistics endpoint
- **THEN** the response contains no track BLOB field

### Requirement: Reads available during import
The system SHALL keep list and statistics reads available while an import is in progress, without blocking them on the import's writes.

#### Scenario: Stats requested during an in-progress import
- **WHEN** a client requests statistics or the activity list while an import is being processed
- **THEN** the request succeeds without waiting for the import to complete
