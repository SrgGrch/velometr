# activity-dashboard Specification

## Purpose

Presents year-scoped mileage totals and the underlying trip list so the user can see their cycling volume at a glance, without opening Strava or counting manually.

## Requirements

### Requirement: Year selection scopes the dashboard
The system SHALL let the user switch between calendar years, and SHALL scope the hero stats, weekly chart, and trip list to the selected year.

#### Scenario: User switches year
- **WHEN** the user selects a different calendar year
- **THEN** the hero stats, weekly chart, and trip list all update to reflect only that year's activities

### Requirement: Yearly hero stats
The system SHALL display, for the selected year, the total distance, the number of trips, the average weekly distance, and the longest single trip's distance. The average weekly distance SHALL equal the selected year's total distance divided by the number of distinct calendar weeks that contain at least one trip, excluding weeks without trips from the divisor.

#### Scenario: Year has activities
- **WHEN** the selected year has one or more imported activities across one or more calendar weeks
- **THEN** the hero block shows the total distance, trip count, longest trip, and average weekly distance calculated using only the distinct weeks that contain at least one trip

#### Scenario: Multiple trips occur in one active week
- **WHEN** the selected year contains multiple trips in the same calendar week
- **THEN** that calendar week contributes once to the average weekly distance divisor

#### Scenario: Year has no activities
- **WHEN** the selected year has no imported activities
- **THEN** the hero block shows zero/empty values without error and the average weekly distance is zero

### Requirement: Weekly distance chart
The system SHALL display a 52-week bar chart of distance per week for the selected year, with month labels and bars always filling the available plotting width from its leading edge to its trailing edge without horizontal scrolling. Weekly bars SHALL use the accent color only while hovered or selected by click, and SHALL otherwise use the default chart color regardless of distance.

#### Scenario: Chart is displayed at any supported viewport width
- **WHEN** the weekly chart is displayed
- **THEN** its bars and month labels fill the current plotting width without horizontal scrolling, with the first and last bars aligned to its edges

#### Scenario: No week is active
- **WHEN** no weekly bar is hovered or selected
- **THEN** every weekly bar uses the default chart color, including the highest-distance weeks

#### Scenario: A week's distance is a peak
- **WHEN** a week's total distance is among the highest in the selected year and that week is neither hovered nor selected
- **THEN** that week's bar uses the default chart color without a persistent peak highlight

#### Scenario: User hovers a week
- **WHEN** the pointer hovers a weekly bar
- **THEN** that bar alone uses the accent color and the title row shows the week's localized start and end dates plus its distance

#### Scenario: User selects a week
- **WHEN** the user clicks a weekly bar
- **THEN** that bar remains accented and the title row shows the week's localized start and end dates plus its distance until the selection changes or is cleared

#### Scenario: Hover temporarily overrides a selection
- **WHEN** one week is selected and the user hovers a different weekly bar
- **THEN** the hovered week's accent and details are shown, and the selected week's state is restored after the pointer leaves

### Requirement: Trip list as cards
The system SHALL display the selected year's trips as individual cards (not a table), each showing the trip's title, date, distance, duration, average speed, and max speed. Dates and statistic labels on every card SHALL use color `#6F7874`. The system SHALL NOT provide a trip detail view.

#### Scenario: Year has trips
- **WHEN** the selected year has imported activities
- **THEN** each is rendered as a card showing title, date, distance, duration, average speed, and max speed, with its date and statistic labels colored `#6F7874`

#### Scenario: Card selected
- **WHEN** the user interacts with a trip card
- **THEN** no separate detail screen is opened

### Requirement: Adaptive two-pane layout on wide, tall viewports
The system SHALL present the hero stats and weekly chart in a main pane alongside the trip list in an adjacent supporting pane, side by side, whenever the viewport is at least the medium width breakpoint (600dp) wide AND at least the expanded height breakpoint (900dp) tall. In this layout the trip list SHALL use a two-column card grid.

#### Scenario: Viewport is both wide and tall
- **WHEN** the viewport is at least 600dp wide and at least 900dp tall
- **THEN** the hero stats and weekly chart render in a main pane, the trip list renders in an adjacent supporting pane, and the trip list uses a two-column card grid

### Requirement: Collapsed single-column layout on narrow or short viewports
The system SHALL collapse the dashboard to a single scrollable column — trip list with the hero stats and weekly chart as its header — whenever the viewport is narrower than the medium width breakpoint (600dp) or shorter than the expanded height breakpoint (900dp). The dashboard header (title, year controls, import button) SHALL stack into two rows only when the viewport is narrower than the medium width breakpoint; shortness alone SHALL NOT trigger header stacking. The trip list SHALL use a single-column card grid only when the viewport is narrower than the medium width breakpoint; a viewport that is merely short SHALL keep the two-column card grid.

#### Scenario: Viewport is narrow or short
- **WHEN** the viewport is narrower than 600dp or shorter than 900dp
- **THEN** the dashboard renders as a single scrollable column with the hero stats and weekly chart shown above the trip list

#### Scenario: Viewport is narrow
- **WHEN** the viewport is narrower than 600dp
- **THEN** the header stacks the title above a row containing the year controls and import button, and the trip list uses a single-column card grid

#### Scenario: Viewport is short but not narrow
- **WHEN** the viewport is at least 600dp wide but shorter than 900dp
- **THEN** the header stays a single row and the trip list still uses a two-column card grid

### Requirement: Server-computed aggregates
The system SHALL compute weekly, monthly, seasonal, and yearly aggregates on the backend and expose them via REST endpoints, so the frontend only renders data it receives.

#### Scenario: Client requests yearly aggregates
- **WHEN** the frontend requests aggregate statistics for a year
- **THEN** the backend returns the computed aggregates and the frontend performs no aggregation itself
