## 1. Trip card secondary text

- [ ] 1.1 Add a palette token for `#6F7874`, apply it to trip-card dates and statistic labels, and verify all requested secondary text uses the exact color without changing unrelated faint text.

## 2. Weekly average aggregation

- [ ] 2.1 Change the backend yearly summary to divide total distance by the number of distinct calendar weeks containing at least one trip, return zero when there are no trips, and verify repository tests cover empty years, multiple trips in one week, trips across separate weeks, and a week containing a zero-distance trip.

## 3. Weekly chart interaction and layout

- [ ] 3.1 Remove distance-based peak coloring, keep hover-over-selection precedence for the accent, and verify an idle chart has no orange bars while hover and click each accent only the active bar.
- [ ] 3.2 Keep the active-week details beside the chart title, format them as the localized start-to-end date range plus distance, and verify hovering a bar shows a range such as `7 сен - 13 сен` instead of a week number.
- [ ] 3.3 Rework the bar and month-label tracks to always use `fillMaxWidth` without horizontal scrolling, and verify the bars reach both plotting edges and the month labels remain aligned at desktop and narrow viewport widths.

## 4. Import modal dismissal

- [ ] 4.1 Make the import backdrop invoke the existing close callback and consume clicks on the modal surface, then verify an outside click closes the modal while its file picker, remove button, close button, and import action continue to work without accidental dismissal.

## 5. Verification

- [ ] 5.1 Run the backend test suite and build the wasm frontend with `./gradlew :backend:test :frontend:wasmJsBrowserDistribution`, then manually verify the updated average, chart, trip-card colors, and modal behavior at desktop and narrow widths.
