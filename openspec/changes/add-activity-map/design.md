## Context

See proposal.md - Why/What Changes for motivation and scope. Relevant current state:

- Backend routes live in `backend/src/main/kotlin/dev/velometr/backend/presentation/Routes.kt` (`GET /api/activities`, `GET /api/stats/{year}/summary`, `GET /api/stats/{year}/weekly`); `ActivityRepository` never selects `track_gpx` in its existing queries (deliberately, per `activity-import` spec and the reserved-column comment at `ActivityRepository.kt:69`).
- `activities.track_gpx` is a gzip-compressed BLOB that is either a GPX (XML) file or Strava's raw `.fit.gz` (binary FIT) file, decided per-activity at import time by whatever track file the archive contained (`activity-import` spec, "Track file storage").
- `DashboardScreen.kt` already renders an adaptive `SupportingPaneScaffold` with `mainPane` (hero stats + weekly chart) and `supportingPane` (trip list), gated on the same 600dp/900dp breakpoint this change reuses, and already calls `rememberSupportingPaneScaffoldNavigator<Long>()` — created but never used to navigate. Below that breakpoint, `DashboardScreen` renders a single-column `TripList` instead.
- Frontend architecture constraint (`CLAUDE.md`): the frontend is a pure renderer of backend JSON, with no local aggregation/business logic. Track decoding (gunzip + GPX XML parsing) is therefore a backend concern; the frontend receives plain points.

## Goals / Non-Goals

**Goals:**
- Serve one activity's GPS points, decoded from GPX, over a new endpoint.
- Render those points as a path over map tiles, opened by clicking a trip card, placed adaptively per the existing dashboard breakpoint.

**Non-Goals:**
- Parsing FIT tracks (per proposal.md; activities with only a `.fit.gz` track show "no track available").
- A general trip-detail screen, or any other new information surfaced from clicking a card besides the map.
- Server-side map rendering, tile caching/proxying, or point simplification (deferred exactly as the original mileage-tracker design.md flagged "point-level GPX processing ... deferred until a map screen exists" — this change is that map screen, but simplification itself stays deferred since MapCompose-MP's tiled rendering doesn't need it at this app's per-ride point counts).

## Decisions

### New endpoint returns decoded points, not raw GPX
`GET /api/activities/{id}/track` returns JSON, e.g. `{"points": [{"lat": ..., "lon": ...}, ...]}` (or an equivalent "unavailable" shape) when the id exists but has no GPX-decodable track. The backend gunzips the stored BLOB, sniffs whether it's GPX (XML) or something else (FIT), and only parses GPX. This keeps the "frontend never computes business/domain logic" line intact and keeps FIT genuinely out of scope rather than half-supported (e.g. no client-side XML parsing to maintain, no ambiguity about who decodes what).

Alternative considered: serve the decompressed raw GPX bytes and parse XML client-side in Compose. Rejected — it splits track-format knowledge across both tiers for no benefit, and would need to be redone anyway if FIT support is added later (that parsing would also belong server-side).

Library choice for GPX parsing is an implementation detail for tasks.md (a small XML-based GPX parser is sufficient; JVM has `javax.xml` built in, no new heavy dependency needed).

### Map rendering: MapCompose-MP with direct client-side OSM tiles
Use MapCompose-MP (`ovh.plrapps.mapcompose`, wasmJs-compatible per its published targets) for the map widget. Tiles are fetched by the browser directly from a standard OSM-compatible XYZ tile endpoint (e.g. `https://tile.openstreetmap.org/{z}/{x}/{y}.png`) via a custom `TileStreamProvider`, with the required attribution shown on the map view (per the `activity-map` spec's tile requirement). This keeps the stack free of a paid tile provider, consistent with the original project's "no external paid dependencies" goal, and needs no backend changes.

Alternative considered: proxy tiles through the backend. Rejected for this single-user app — it adds route/caching surface for a problem (CORS, rate limiting at scale) this deployment doesn't have, and can be added later without touching the frontend's tile-provider abstraction if usage ever grows past what direct fetching can sustain.

GPS points are projected into MapCompose's tile pixel space using standard Web Mercator / XYZ slippy-map math (the same formulas used to compute tile URLs), scoped to a max zoom level chosen to keep `fullWidth`/`fullHeight` reasonable; this projection is presentation logic for the map widget, not a business computation, so it lives in the frontend alongside the widget.

### Card click wiring reuses the existing (dormant) navigator
`DashboardScreen`'s `rememberSupportingPaneScaffoldNavigator<Long>()` already threads an activity id as its content type — wire `TripCard`'s click to `navigator.navigateTo(ContentKey, activity.id)` (or equivalent) instead of introducing a second piece of navigation state. Below the two-pane breakpoint, the same click instead pushes a full-screen map composable (e.g. via a simple `mutableStateOf<Long?>` "open map for id" in the view model), since `SupportingPaneScaffold` isn't used in that branch of `DashboardScreen` today.

Making the whole `TripCard` clickable (per the resolved product decision) sits alongside its existing `SelectionContainer`-wrapped text: `SelectionContainer` and `Modifier.clickable` don't conflict at the framework level (`clickable` is a pointer-press gesture, text selection is a drag gesture recognized by `SelectionContainer`'s own long-press handling), so no rework of the existing selection behavior is required — just adding a `clickable` modifier to the card's outer container.

### Third pane on wide/tall, full screen otherwise
On the ≥600dp/≥900dp breakpoint, add a third pane to the existing `SupportingPaneScaffold` (`extraPane`) that renders the map when the navigator's current destination is non-null, dismissible via the pane's own back/close affordance (`SupportingPaneScaffold` supports a third pane natively via `ThreePaneScaffoldRole.Extra`/`extraPane` slot). Below the breakpoint, the map opens as a distinct full-screen composable over the dashboard (not a fourth stacked pane), matching how that branch already forgoes the multi-pane scaffold entirely.

## Risks / Trade-offs

- [Public OSM tile server usage policy limits automated/heavy use] → Acceptable at personal single-user traffic; if it ever becomes a problem, swap the `TileStreamProvider` implementation for a proxied or alternate provider without touching map/path rendering code.
- [GPX files can contain thousands of trackpoints for long rides, all sent to the client in one response] → Acceptable at this app's ride lengths/frequency (personal cycling log, not endurance-event scale); revisit with point simplification only if a real ride's payload proves too large in practice.
- [FIT-only activities show no map, which may surprise the user if most of their historical archive predates a GPX-exporting device/app] → Explicitly accepted scope cut (see proposal.md); the "no track available" state makes this visible rather than silently missing.
