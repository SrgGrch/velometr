## 1. Backend: track decoding and endpoint

- [x] 1.1 Add a GPX parser (gunzip the stored BLOB, then parse `<trkpt lat="" lon="">` elements from the GPX XML) and a way to detect whether decompressed bytes are GPX vs. something else (e.g. FIT's binary header); verify with a unit test covering a sample GPX file, a non-GPX (FIT-like) byte sequence, and an empty/absent track.
- [x] 1.2 Add `ActivityRepository` support for reading a single activity's `track_gpx` BLOB by id, keeping this off the existing list/stats query paths; verify with a repository test that list/stats queries still exclude the column while the new lookup returns it for a known id.
- [x] 1.3 Add `GET /api/activities/{id}/track` returning decoded points on success and an explicit "no track available" response (not an error) when there's no stored track or it isn't GPX; verify with a route test for all three cases (GPX track, no track, non-GPX track) plus an unknown activity id.

## 2. Frontend: map dependency and rendering

- [x] 2.1 Add the MapCompose-MP dependency to the `wasmJs` frontend target and verify the project still builds (`./gradlew :frontend:wasmJsBrowserDistribution`).
- [x] 2.2 Implement a `TileStreamProvider` that fetches tiles directly from a public OSM-compatible XYZ tile server, and Web Mercator projection helpers converting lat/lon into the `MapState` tile-pixel space used by `addPath`; verify with a unit test asserting known lat/lon inputs project to the expected pixel coordinates at a fixed zoom level.
- [x] 2.3 Build a map composable that takes an activity id, fetches its track from the new endpoint, renders it as a path via `MapUI`/`MapState`, frames the initial view to the track's bounding box, shows the tile source's attribution, and renders a "no track available" state when the endpoint reports none; verify by running the app and opening the map for one activity with a GPX track and one without.

## 3. Frontend: opening the map from the trip list

- [x] 3.1 Add a click handler to `TripCard`'s outer container that opens the map for that activity, without altering existing text-selection behavior; verify by running the app and confirming both clicking a card and selecting its text still work.
- [x] 3.2 Wire `DashboardScreen`'s existing `rememberSupportingPaneScaffoldNavigator<Long>()` to the clicked activity id and add an `extraPane`/third-pane slot to the `SupportingPaneScaffold` branch that renders the map composable when a destination is set, with a close affordance that clears it; verify by running the app at a wide/tall viewport and confirming the map opens alongside the existing panes and can be closed back to the dashboard-only view.
- [x] 3.3 In the single-column (narrow/short) branch of `DashboardScreen`, open the map composable as a full screen replacing the dashboard when a card is clicked, with a way to return to the dashboard; verify by running the app at a narrow/short viewport and confirming the same open/close flow.

## 4. Specs and docs

- [x] 4.1 Run `openspec sync-specs` (or equivalent) once implementation matches this change's delta specs, merging `activity-map` into `openspec/specs/` and applying the `activity-dashboard` delta.
- [x] 4.2 Update `CLAUDE.md`'s "Explicitly out of scope" line to drop "map/track visualization" while keeping "a trip detail screen" (beyond the map view) listed as out of scope.
