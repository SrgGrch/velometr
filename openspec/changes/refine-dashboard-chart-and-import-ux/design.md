## Context

The dashboard and import dialog are rendered by Compose Multiplatform on a single wasm canvas. `WeeklyChart` already tracks hovered and selected indices and uses `weekRangeLabel` for its active-week text, but it also accents peak values automatically and lays out fixed-width bars with fixed gaps. `TripCard` currently uses the shared faint text color for dates and statistic labels. `ImportModal` draws a full-screen backdrop and centered content without click handling on either layer.

## Goals / Non-Goals

**Goals:**

- Keep weekly chart feedback driven by the existing local hover and selection state.
- Make the plotting row consume its available width while retaining usable bars and horizontal scrolling on narrow viewports.
- Add backdrop dismissal without allowing clicks inside the modal to reach the backdrop handler.
- Keep the requested card text color explicit and reusable.

**Non-Goals:**

- Changing weekly aggregation, API payloads, week boundaries, or month-label data.
- Changing the orange accent on trip-card distance markers or primary actions.
- Adding keyboard dismissal, focus trapping, or a general-purpose dialog abstraction.

## Decisions

### Keep one active-week source for highlight and legend

Continue deriving the active week as the hovered index when present, otherwise the selected index. Remove the distance-threshold branch from bar coloring so only that active index receives the accent. Render the active week's localized date range and distance beside the block title in the existing header row.

This preserves the current useful behavior where hover temporarily previews another week without destroying a click selection. Separate hover and selection legends were rejected because they would compete for the same compact header space.

### Size the bar track from the viewport with a minimum usable width

Use the available chart width to distribute the full set of bars across the plotting row, including the first and last positions. Preserve a minimum width for each weekly hit target; when the calculated track would be narrower than that minimum total, let the shared chart track scroll horizontally. Month labels must use the same scroll state and track width so their alignment remains stable.

Keeping the current hard-coded bar width was rejected because it leaves unused space on typical desktop widths. Stretching bars without a minimum was rejected because narrow screens would produce impractically small hover and click targets.

### Add a dedicated secondary-card color token

Represent `#6F7874` as a named palette value used by trip dates and statistic labels. The existing global faint color remains available to unrelated UI, which avoids changing text across the dashboard and import dialog.

### Consume clicks at the modal content boundary

Attach the close action to the full-screen backdrop and attach a consuming click handler to the modal surface. Compose hit testing will route clicks inside the surface to the inner handler, while clicks elsewhere invoke `onClose`.

Using a document-level click listener was rejected because the modal is already inside the Compose canvas and document listeners would require coordinate checks and lifecycle cleanup.

## Risks / Trade-offs

- [A too-small minimum bar width can make interaction difficult, while a large value causes unnecessary scrolling] → Reuse the current bar size as the initial minimum and verify desktop and narrow layouts manually.
- [Month labels can drift from bars if they use a separately calculated width] → Derive both rows from one shared track width and scroll state.
- [Click propagation behavior can differ across Compose versions] → Verify that the backdrop closes on outside click and that the close button, file picker, remove action, and import action do not trigger backdrop dismissal.
