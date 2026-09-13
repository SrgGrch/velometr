## Purpose

Lets the user see where a ride actually went by rendering its recorded GPS track on a map, opened directly from the trip list.

## ADDED Requirements

### Requirement: Per-activity track endpoint
The system SHALL expose an endpoint that returns an activity's recorded track as an ordered list of GPS points, decoded server-side from the activity's stored track file. When the stored track file is not a GPX file (e.g. it is a raw FIT file), or no track file is stored for the activity, the endpoint SHALL report that no track is available rather than erroring.

#### Scenario: Activity has a GPX track
- **WHEN** a client requests the track for an activity whose stored track file is GPX
- **THEN** the response contains the ordered list of GPS points decoded from that file

#### Scenario: Activity has no stored track
- **WHEN** a client requests the track for an activity with no stored track file
- **THEN** the response indicates no track is available, without an error

#### Scenario: Activity's stored track is not GPX
- **WHEN** a client requests the track for an activity whose stored track file is not GPX (e.g. a raw FIT file)
- **THEN** the response indicates no track is available, without an error

### Requirement: Map view opened from the trip list
Clicking a trip card SHALL open a map view for that activity. When the activity has an available GPS track, the map SHALL render it as a path over map tiles, framed to the track's bounding box. When no track is available for the activity, the map view SHALL open showing a "no track available" state instead of a path.

#### Scenario: Trip has a usable track
- **WHEN** the user clicks a trip card for an activity with an available GPS track
- **THEN** the map view opens showing that activity's route as a path, framed to the route's bounding box

#### Scenario: Trip has no usable track
- **WHEN** the user clicks a trip card for an activity with no available GPS track
- **THEN** the map view opens showing a "no track available" state instead of a path

### Requirement: Adaptive map placement
On viewports that qualify for the dashboard's two-pane adaptive layout (at least 600dp wide AND at least 900dp tall), the map view SHALL open in an additional pane alongside the existing dashboard panes, leaving the dashboard visible and interactive. On narrower or shorter viewports, the map view SHALL open as a separate full screen that replaces the dashboard until dismissed. In both placements, the map view SHALL provide a way to close it and return to the prior view.

#### Scenario: Wide and tall viewport
- **WHEN** the viewport is at least 600dp wide and at least 900dp tall and the user clicks a trip card
- **THEN** the map view opens in an additional pane alongside the dashboard's existing panes, and the dashboard remains visible

#### Scenario: Narrow or short viewport
- **WHEN** the viewport is narrower than 600dp or shorter than 900dp and the user clicks a trip card
- **THEN** the map view opens as a separate full screen replacing the dashboard

#### Scenario: Closing the map view
- **WHEN** the user closes an open map view, in either placement
- **THEN** the dashboard is shown again with no map view open

### Requirement: Map tiles from an external provider
The system SHALL render map tiles fetched directly by the client from a public OpenStreetMap-compatible tile source, and SHALL display that source's required attribution on the map view.

#### Scenario: Map view displayed
- **WHEN** a map view is open
- **THEN** it shows map tiles from the configured tile source together with that source's attribution
