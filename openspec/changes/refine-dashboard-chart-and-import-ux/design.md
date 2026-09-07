## Context

The dashboard and import dialog are rendered by Compose Multiplatform on a single wasm canvas. `WeeklyChart` already tracks hovered and selected indices and uses `weekRangeLabel` for its active-week text, but it also accents peak values automatically and lays out fixed-width bars with fixed gaps. `TripCard` currently uses the shared faint text color for dates and statistic labels. `ImportModal` draws a full-screen backdrop and centered content without click handling on either layer. The backend currently calculates average weekly distance as the yearly total divided by 52, so inactive weeks reduce the displayed average.

## Goals / Non-Goals

**Goals:**

- Keep weekly chart feedback driven by the existing local hover and selection state.
- Make the plotting row and month labels always consume the available width without horizontal scrolling.
- Add backdrop dismissal without allowing clicks inside the modal to reach the backdrop handler.
- Keep the requested card text color explicit and reusable.
- Calculate the yearly summary's weekly average from active weeks on the backend.

**Non-Goals:**

- Changing weekly bucket boundaries, API payload shapes, or month-label data.
- Changing the orange accent on trip-card distance markers or primary actions.
- Adding keyboard dismissal, focus trapping, or a general-purpose dialog abstraction.

## Decisions

### Keep one active-week source for highlight and legend

Continue deriving the active week as the hovered index when present, otherwise the selected index. Remove the distance-threshold branch from bar coloring so only that active index receives the accent. Render the active week's localized date range and distance beside the block title in the existing header row.

This preserves the current useful behavior where hover temporarily previews another week without destroying a click selection. Separate hover and selection legends were rejected because they would compete for the same compact header space.

### Always size the chart track to the viewport

Apply `fillMaxWidth` to the chart track and distribute the full set of bars across that width, including the first and last positions. Month labels use the same available width and proportional distribution so their alignment remains stable at every supported viewport size. The chart has no horizontal scroll state.

Keeping the current hard-coded bar width was rejected because it leaves unused space on typical desktop widths. A minimum track width with horizontal scrolling was rejected because the chart must remain fully visible within its block at all viewport widths.

### Add a dedicated secondary-card color token

Represent `#6F7874` as a named palette value used by trip dates and statistic labels. The existing global faint color remains available to unrelated UI, which avoids changing text across the dashboard and import dialog.

### Consume clicks at the modal content boundary

Attach the close action to the full-screen backdrop and attach a consuming click handler to the modal surface. Compose hit testing will route clicks inside the surface to the inner handler, while clicks elsewhere invoke `onClose`.

Using a document-level click listener was rejected because the modal is already inside the Compose canvas and document listeners would require coordinate checks and lifecycle cleanup.

### Derive the average from distinct active calendar weeks

In the backend yearly-summary calculation, group the selected year's activities by the same Monday-to-Sunday calendar-week convention used by the weekly chart, count each distinct week with at least one trip once, and divide total yearly distance by that count. Return zero when there are no active weeks.

Counting distinct weeks from activities keeps zero-distance trips eligible to make a week active and directly matches the user-visible definition of a week with trips. Dividing by the number of non-zero weekly-distance buckets was rejected because a week containing only a zero-distance imported trip would be incorrectly treated as inactive. The response model remains unchanged.

## Risks / Trade-offs

- [Bars become narrow on small screens] → Keep each bar's full allocated cell as its hover and click target and verify interaction at the narrow layout breakpoint.
- [Month labels can drift from bars if they use a separately calculated width] → Derive both rows from the same available width and proportional positions.
- [Click propagation behavior can differ across Compose versions] → Verify that the backdrop closes on outside click and that the close button, file picker, remove action, and import action do not trigger backdrop dismissal.
- [The summary and chart could disagree if they use different week boundaries] → Reuse the repository's Monday-to-Sunday week convention and add boundary-focused aggregation tests.
