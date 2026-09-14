## MODIFIED Requirements

### Requirement: Trip list as cards
The system SHALL display the selected year's trips as individual cards (not a table), each showing the trip's title, date, distance, duration, average speed, and max speed. Dates and statistic labels on every card SHALL use color `#6F7874`. Clicking a card SHALL open that trip's map view (see the `activity-map` capability). The system SHALL NOT provide any other trip detail view.

#### Scenario: Year has trips
- **WHEN** the selected year has imported activities
- **THEN** each is rendered as a card showing title, date, distance, duration, average speed, and max speed, with its date and statistic labels colored `#6F7874`

#### Scenario: Card selected
- **WHEN** the user clicks a trip card
- **THEN** that trip's map view opens and no other detail screen is shown
