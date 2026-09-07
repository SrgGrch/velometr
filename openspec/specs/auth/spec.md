# auth Specification

## Purpose

Gates the entire single-user application behind a shared passcode, since there is no per-user account system, registration, or sharing.

## Requirements

### Requirement: Passcode required for access
The system SHALL require a correct passcode before granting access to any application screen or data.

#### Scenario: No passcode submitted
- **WHEN** a client requests an application screen or API endpoint without having authenticated
- **THEN** the system denies access and does not return application data

#### Scenario: Correct passcode submitted
- **WHEN** the user submits the passcode configured for the deployment
- **THEN** the system grants access to the application

### Requirement: Server-side passcode verification
The system SHALL verify the submitted passcode on the backend against the deployment's configured secret, and SHALL NOT expose or validate the passcode on the client alone.

#### Scenario: Passcode checked against configured secret
- **WHEN** a login request is received
- **THEN** the backend compares it to the passcode configured for the deployment and only the backend's decision determines access

### Requirement: Incorrect passcode rejected
The system SHALL reject an incorrect passcode without granting access.

#### Scenario: Wrong passcode entered
- **WHEN** the user submits a passcode that does not match the configured value
- **THEN** the system denies access and the login screen indicates the attempt failed

### Requirement: No self-service registration
The system SHALL NOT provide any account creation, registration, or multi-user signup flow.

#### Scenario: No registration entry point exists
- **WHEN** a user views the login screen
- **THEN** no option to create an account or register is presented
