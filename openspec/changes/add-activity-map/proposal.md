## Why

The dashboard currently reduces every ride to numbers (distance, duration, speed) with no way to see where it went. The archive already stores each activity's GPX track (`activities.track_gpx`), but it is write-only today — no endpoint serves it and no screen renders it. Adding a map view turns that dormant data into the most requested kind of context for a mileage tracker: the route itself.

## What Changes

- Add a new backend endpoint that returns an activity's track as an ordered list of lat/lon points, decoded server-side from its stored (gzip-compressed) GPX file. Activities whose stored track is not GPX (e.g. Strava's raw `.fit.gz`) report no track available — FIT parsing is out of scope for this change.
- Add a map view (via MapCompose-MP, https://github.com/p-lr/mapcomposemp) that renders that track as a path over OpenStreetMap tiles, fetched directly from the browser (no backend tile proxy), framed to the track's bounding box on open.
- Clicking anywhere on a trip card opens that activity's map:
  - On viewports that qualify for the existing two-pane adaptive layout (≥600dp wide AND ≥900dp tall), the map opens in a third pane alongside the existing main/supporting panes.
  - On narrower/shorter viewports, the map opens as a separate full screen, replacing the dashboard until dismissed.
- Narrow the existing "no trip detail view" rule so it no longer blocks this map view, while still ruling out a general trip-detail screen.
- Update `CLAUDE.md`'s "explicitly out of scope" note to reflect that map/track visualization is now in scope (a general trip-detail screen remains out of scope).

## Capabilities

### New Capabilities
- `activity-map`: serving an activity's decoded GPX track and rendering it on a map, opened from the trip list, adaptively shown in an extra pane or a separate screen.

### Modified Capabilities
- `activity-dashboard`: the "Trip list as cards" requirement's blanket "SHALL NOT provide a trip detail view" and its "no detail screen opens" scenario are narrowed — clicking a card now opens the new map view, though no other trip-detail screen is introduced.

## Impact

- Backend: new route (e.g. `GET /api/activities/{id}/track`) and a GPX parser dependency; `ActivityRepository` gains a query that selects `track_gpx` for a single id (still excluded from list/stats queries).
- Frontend: new MapCompose-MP dependency (wasmJs target), a map screen/pane composable, GPS→tile-space projection logic, wiring `DashboardScreen`'s existing (currently unused for navigation) `rememberSupportingPaneScaffoldNavigator<Long>()` to an activity id, and a click handler on `TripCard`.
- Specs: `openspec/specs/activity-dashboard/spec.md` (delta) and a new `openspec/specs/activity-map/spec.md`.
- Docs: `CLAUDE.md` out-of-scope note.
- Third-party dependency: a public OSM-compatible tile server, subject to its usage/attribution policy.
