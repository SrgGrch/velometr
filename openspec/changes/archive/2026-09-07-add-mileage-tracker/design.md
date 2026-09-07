## Context

Greenfield project — no existing code or specs. See `proposal.md` - Why for motivation (Strava API paywall) and Impact for the overall footprint. This document covers the architecture, data model, and deployment shape needed to satisfy the `auth`, `activity-import`, and `activity-dashboard` specs. UI reference: `_docs/mileage-tracker-mockup.html` (dark theme, contour-line motif, accent color on primary actions).

## Goals / Non-Goals

**Goals:**
- A single deployable unit (`docker compose up`) with no external paid dependencies.
- All business logic (parsing, dedup, aggregation) server-side; frontend is a pure renderer.
- Data durability independent of container rebuilds.

**Non-Goals:**
- Trip detail screen, map/track rendering, Strava API integration, auto-sync, multi-user support (see proposal.md - What Changes, last bullet).
- Point-level GPX processing (e.g., Douglas-Peucker simplification) — deferred until a map screen exists.

## Decisions

### Backend: Kotlin + Ktor
Handles zip upload, CSV parsing, dedup, SQLite access, and REST aggregation endpoints. Chosen for language continuity with the Compose Multiplatform frontend (shared toolchain, no second language to maintain for a single-developer project).

### Frontend: Kotlin Compose Multiplatform, wasmJs target
Thin client: fetches JSON from the backend and renders it, no local computation of aggregates. Chosen to reuse Kotlin skills/tooling instead of introducing a JS framework for a small, single-user UI.

### Storage: single SQLite file, tracks as BLOB, WAL mode
```sql
CREATE TABLE activities (
  id            INTEGER PRIMARY KEY,   -- Strava activity_id; dedup key
  date          TEXT NOT NULL,
  title         TEXT,
  distance_km   REAL NOT NULL,
  duration_sec  INTEGER NOT NULL,
  avg_speed     REAL,
  max_speed     REAL,
  track_gpx     BLOB,                  -- gzipped source GPX/FIT, may be absent
  created_at    TEXT DEFAULT CURRENT_TIMESTAMP
);
```
- Tracks stored as a BLOB inside SQLite rather than as separate files on disk: one file to back up, simpler operations for a single-user deployment. Alternative considered: files on disk + path in DB — rejected, adds a second thing to keep in sync with backups/volume.
- `PRAGMA journal_mode=WAL`: satisfies the activity-import "reads available during import" requirement — readers aren't blocked by the writer during an import.
- List/stats queries never select `track_gpx`; it is served only by a future per-id endpoint, keeping today's payloads small since no screen consumes it yet.
- `id` is the Strava `activity_id` itself (not an autoincrement surrogate), which is what makes `INSERT OR IGNORE` a correct and sufficient dedup mechanism.

### Import processing
1. Accept the uploaded zip in memory or to a temp path, unzip, locate `activities.csv`.
2. Parse each row into a candidate record; for each, `INSERT OR IGNORE INTO activities (id, ...) VALUES (...)`.
3. For rows that match a track file in the archive (by activity id in filename), gzip the track file and update/insert it as `track_gpx` for that row.
4. Values written for distance/duration/avg/max speed come directly from the CSV columns — no recomputation from track points (per activity-import spec).

### Deployment: docker compose, two services
- `backend`: Ktor app, reads `DATA_PATH` (SQLite file location) and `AUTH_PASSCODE` from `.env`.
- `frontend`: nginx serving the compiled wasmJs static bundle, proxying `/api/*` to `backend` over the compose-internal network.
- `DATA_PATH` points at a mounted volume so the database survives image rebuilds/redeploys.

### Auth
Passcode compared server-side against `AUTH_PASSCODE`; no user table, no registration surface, consistent with the `auth` spec's single-secret model.

## Risks / Trade-offs

- [SQLite as BLOB store could grow large if track files are big] → Acceptable at single-user, personal-archive scale; revisit only if it becomes a problem.
- [In-memory zip/CSV parsing on a large multi-year export could spike backend memory] → Stream-parse the CSV and process the zip entry-by-entry rather than loading the whole archive into memory at once.
- [Passcode is a single shared secret with no rate limiting specified] → Acceptable given the app is not publicly indexed/linked (proposal.md - Impact, non-functional requirements); revisit if the deployment becomes reachable more broadly.

## Migration Plan

Greenfield — no data or users to migrate. Initial rollout is: build images, provision the persistent volume for `DATA_PATH`, set `.env` (`DATA_PATH`, `AUTH_PASSCODE`), `docker compose up`. Rollback is redeploying the previous image set; the SQLite file on the volume is untouched by container changes either way.

## Open Questions

Carried over from the PRD as deliberately deferred (do not affect this change's specs, approach, or tasks):
- Whether track-on-map visualization will be built, which would drive map-library choice and possible point simplification.
- Whether a one-click DB export/backup is needed beyond manual volume backup.
- Whether an activity-type filter (e.g., run vs. ride) will be needed if the Strava account covers multiple sports.
