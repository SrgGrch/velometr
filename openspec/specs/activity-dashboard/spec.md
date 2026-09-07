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
The system SHALL display, for the selected year, the total distance, the number of trips, the average weekly distance, and the longest single trip's distance.

#### Scenario: Year has activities
- **WHEN** the selected year has one or more imported activities
- **THEN** the hero block shows the total distance and the three supporting metrics computed from that year's activities

#### Scenario: Year has no activities
- **WHEN** the selected year has no imported activities
- **THEN** the hero block shows zero/empty values without error

### Requirement: Weekly distance chart
The system SHALL display a 52-week bar chart of distance per week for the selected year, scrollable horizontally, labeled with months, and visually highlighting peak weeks.

#### Scenario: A week's distance is a peak
- **WHEN** a week's total distance is among the highest in the selected year
- **THEN** that week's bar is rendered with the peak highlight

### Requirement: Trip list as cards
The system SHALL display the selected year's trips as individual cards (not a table), each showing the trip's title, date, distance, duration, average speed, and max speed. The system SHALL NOT provide a trip detail view.

#### Scenario: Year has trips
- **WHEN** the selected year has imported activities
- **THEN** each is rendered as a card showing title, date, distance, duration, average speed, and max speed

#### Scenario: Card selected
- **WHEN** the user interacts with a trip card
- **THEN** no separate detail screen is opened

### Requirement: Server-computed aggregates
The system SHALL compute weekly, monthly, seasonal, and yearly aggregates on the backend and expose them via REST endpoints, so the frontend only renders data it receives.

#### Scenario: Client requests yearly aggregates
- **WHEN** the frontend requests aggregate statistics for a year
- **THEN** the backend returns the computed aggregates and the frontend performs no aggregation itself
