## 1. Trip card secondary text

- [ ] 1.1 Add a palette token for `#6F7874`, apply it to trip-card dates and statistic labels, and verify all requested secondary text uses the exact color without changing unrelated faint text.

## 2. Weekly chart interaction and layout

- [ ] 2.1 Remove distance-based peak coloring, keep hover-over-selection precedence for the accent, and verify an idle chart has no orange bars while hover and click each accent only the active bar.
- [ ] 2.2 Keep the active-week details beside the chart title, format them as the localized start-to-end date range plus distance, and verify hovering a bar shows a range such as `7 сен - 13 сен` instead of a week number.
- [ ] 2.3 Rework the bar and month-label tracks to share a responsive width and scroll state, and verify the bars reach both plotting edges on a wide viewport while both rows stay aligned and horizontally scrollable on a narrow viewport.

## 3. Import modal dismissal

- [ ] 3.1 Make the import backdrop invoke the existing close callback and consume clicks on the modal surface, then verify an outside click closes the modal while its file picker, remove button, close button, and import action continue to work without accidental dismissal.

## 4. Frontend verification

- [ ] 4.1 Build the wasm frontend with `./gradlew :frontend:wasmJsBrowserDistribution` and manually verify the updated chart, trip-card colors, and modal behavior at desktop and narrow widths.
