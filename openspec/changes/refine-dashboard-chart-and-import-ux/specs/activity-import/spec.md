## ADDED Requirements

### Requirement: Import modal backdrop dismissal
The system SHALL close the import modal when the user clicks the backdrop outside the modal content, and SHALL keep the modal open when the user clicks within its content.

#### Scenario: User clicks outside the import modal
- **WHEN** the import modal is open and the user clicks its backdrop outside the modal content
- **THEN** the import modal closes

#### Scenario: User clicks inside the import modal
- **WHEN** the import modal is open and the user clicks anywhere within the modal content
- **THEN** the backdrop dismissal does not close the modal
