## MODIFIED Requirements

### Requirement: Weekly distance chart
The system SHALL display a 52-week bar chart of distance per week for the selected year, scrollable horizontally when its content does not fit, with month labels and bars spanning the available plotting width from its leading edge to its trailing edge. Weekly bars SHALL use the accent color only while hovered or selected by click, and SHALL otherwise use the default chart color regardless of distance.

#### Scenario: Chart has available horizontal space
- **WHEN** the weekly chart is displayed in a viewport wide enough for its content
- **THEN** the weekly bars are distributed across the full plotting width with the first and last bars aligned to its edges

#### Scenario: Chart content exceeds the viewport
- **WHEN** the weekly chart cannot fit its content at its minimum usable bar width
- **THEN** the chart remains horizontally scrollable together with its month labels

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
