## 1. Project scaffolding

- [x] 1.1 Create Kotlin/Ktor backend module skeleton (build config, entry point, health-check route) and verify it starts and responds on a local port
- [x] 1.2 Create Kotlin Compose Multiplatform (wasmJs) frontend module skeleton and verify it builds a static wasmJs bundle
- [x] 1.3 Add `.env.example` documenting `DATA_PATH` and `AUTH_PASSCODE` and verify the backend reads both at startup

## 2. Data layer

- [x] 2.1 Add SQLite dependency and create the `activities` table per design.md's schema, opened against the `DATA_PATH` file, and verify the table is created on first backend startup
- [x] 2.2 Set `PRAGMA journal_mode=WAL` on the connection and verify (via a concurrent read while a write transaction is open) that reads are not blocked
- [x] 2.3 Write a repository function to upsert an activity row using `INSERT OR IGNORE` keyed on `id`, and verify a unit test shows a repeated insert of the same id is a no-op

## 3. Auth

- [x] 3.1 Implement a login endpoint that compares a submitted passcode to `AUTH_PASSCODE` server-side and returns a session token/cookie on success, and verify a test covers both correct and incorrect passcode cases
- [x] 3.2 Add backend middleware that rejects unauthenticated requests to all non-login API routes, and verify a test confirms a request without a valid session is denied
- [ ] 3.3 Build the login screen (single passcode field, submit button, contour-line background per mockup) and verify manually that submitting the correct passcode grants access and an incorrect one shows an error without granting access

## 4. Activity import

- [x] 4.1 Implement the import endpoint: accept a zip upload, unzip it, and locate `activities.csv`, and verify a test with a sample archive confirms the CSV is found and read
- [x] 4.2 Parse `activities.csv` rows into activity records (id, date, title, distance_km, duration_sec, avg_speed, max_speed) and verify a unit test covers a representative CSV sample
- [x] 4.3 Wire parsed rows through the upsert-by-id repository function from 2.3 and verify an integration test shows importing the same archive twice produces no duplicate rows
- [x] 4.4 For each imported activity, locate a matching track file (`.gpx`/`.fit.gz`) in the archive, gzip it, and store it in `track_gpx`, and verify a test confirms activities without a matching track file still import successfully with `track_gpx` left null
- [x] 4.5 Confirm distance/duration/avg/max speed values are taken directly from the CSV row and never recomputed from track points, and verify via a test using an activity that has both a CSV row and a track file
- [ ] 4.6 Build the import modal UI (dropzone drag/drop + file picker, selected-file row with remove action, import button enabled only when a file is selected) per the mockup, and verify manually that selecting, removing, and re-selecting a file toggles the import button correctly
- [x] 4.7 Wire the modal's confirm action to the import endpoint and verify manually that a successful import closes/updates the modal and the dashboard reflects the newly imported activities

## 5. Aggregation and dashboard API

- [x] 5.1 Implement a REST endpoint returning yearly hero stats (total distance, trip count, average weekly distance, longest trip) for a given year, computed server-side, and verify a unit test against a fixed set of activities produces the expected numbers, including a year with zero activities
- [x] 5.2 Implement a REST endpoint returning per-week distance totals (52 entries) for a given year, and verify a unit test covers week boundaries and an empty year
- [x] 5.3 Implement a REST endpoint returning the trip list for a given year (excluding `track_gpx`) with title, date, distance, duration, avg speed, max speed, and verify a test confirms the response contains no track BLOB field
- [x] 5.4 Verify (via test or manual check) that all three endpoints in this section remain responsive while an import (section 4) is in progress

## 6. Dashboard frontend

- [x] 6.1 Build the dashboard header (app name, year switcher, import button) and wire the year switcher to refetch data for the selected year, and verify manually that switching years updates the displayed data
- [x] 6.2 Build the hero block (yearly total + three supporting stats) bound to the endpoint from 5.1, and verify manually it renders correctly for a year with data and a year without
- [x] 6.3 Build the 52-week horizontal-scroll bar chart with month labels and peak-week highlighting, bound to the endpoint from 5.2, and verify manually against the mockup's visual behavior
- [x] 6.4 Build the trip list as cards (title, date, distance, duration, avg speed, max speed; no click-through to a detail screen), bound to the endpoint from 5.3, and verify manually that no detail navigation is triggered on card interaction

## 7. Deployment

- [x] 7.1 Write Dockerfiles for `backend` and `frontend` and a `docker-compose.yml` wiring both services plus the nginx `/api/*` proxy to `backend`, and verify `docker compose up` serves the login screen at the frontend port
- [x] 7.2 Mount `DATA_PATH` as a volume in compose and verify that removing and recreating the `backend` container preserves previously imported activities
- [ ] 7.3 Verify end-to-end: fresh `docker compose up`, log in with `AUTH_PASSCODE`, import a sample Strava export zip, and confirm the dashboard shows the imported activities' stats
