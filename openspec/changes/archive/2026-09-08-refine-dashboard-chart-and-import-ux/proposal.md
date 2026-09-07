## Why

The dashboard currently gives peak weeks a persistent orange emphasis, uses overly dim secondary text on trip cards, and leaves the weekly chart interaction and spacing less clear than intended. The import dialog also requires an explicit close action even when the user clicks the surrounding overlay.

## What Changes

- Remove automatic orange highlighting from high-distance weekly bars; apply the accent only to the bar under the pointer or selected by click.
- Change trip-card dates and statistic labels to `#6F7874` for more legible secondary text.
- Show the hovered or selected week's start and end dates, together with its distance, instead of a week number.
- Close the import dialog when the user clicks its backdrop while keeping clicks inside the dialog from closing it.
- Expand the weekly bars across the full chart width and place the active-week legend on the same row as the chart section title.
- Calculate average weekly distance using only weeks that contain at least one trip; keep the value at zero when the selected year has no trips.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `activity-dashboard`: Refine weekly-chart highlighting, date-range feedback, layout, trip-card secondary text styling, and the average-weekly-distance calculation.
- `activity-import`: Allow the import modal to close when its backdrop is clicked.

## Impact

- Frontend Compose UI in `DashboardScreen.kt`, `ImportModal.kt`, `Formatting.kt`, and the shared color palette in `Theme.kt`.
- Backend yearly-summary aggregation in `ActivityRepository.kt` and its tests; endpoint paths and response models are unaffected.
