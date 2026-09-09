# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Velometr is implemented and deployable: a Kotlin + Ktor backend (`backend/`), a Compose Multiplatform `wasmJs` frontend (`frontend/`), and Docker Compose deployment all exist. Two OpenSpec changes have shipped and been archived (`openspec/changes/archive/2026-09-07-add-mileage-tracker/`, `openspec/changes/archive/2026-09-08-refine-dashboard-chart-and-import-ux/`); their requirements are merged into `openspec/specs/`.

Commands (see `README.md` for full local-dev instructions):
- `docker compose up -d --build` — run the full stack (backend + frontend behind nginx)
- `./gradlew :backend:test` — backend tests
- `./gradlew :backend:run` — run backend directly (needs JDK 21; requires `DATA_PATH`/`AUTH_PASSCODE`/`PORT` env vars, see README)
- `./gradlew :frontend:wasmJsBrowserDistribution` — build the frontend for deployment (output in `frontend/build/dist/wasmJs/productionExecutable/`)

## What this project is

A personal, single-user cycling mileage tracker. Source of truth is a manually-uploaded Strava bulk-export archive (`activities.csv` + track files), not the Strava API (avoided due to Strava's paid API tier). Full product spec: `_docs/prd.md` (written in Russian). UI reference: `_docs/mileage-tracker-mockup.html`.

## Architecture

Decisions below are fixed design decisions from `openspec/changes/archive/2026-09-07-add-mileage-tracker/design.md` — treat them as constraints, not open choices:

- **Backend:** Kotlin + Ktor. Owns *all* business logic — zip upload handling, CSV parsing, dedup, SQLite access, and REST aggregation endpoints. Chosen for toolchain continuity with the Compose Multiplatform frontend.
- **Frontend:** Kotlin Compose Multiplatform, `wasmJs` target. A pure renderer of JSON the backend returns — no local computation of aggregates, stats, or dedup logic belongs here.
- **Storage:** a single SQLite file (path from `.env` var `DATA_PATH`, mounted on a persistent volume so it survives image rebuilds), one `activities` table, `PRAGMA journal_mode=WAL` so reads aren't blocked during import writes.
  - `id` is the Strava `activity_id` itself (not a surrogate key) — this is what makes `INSERT OR IGNORE` a correct, sufficient dedup mechanism on repeated imports.
  - Track files (`.gpx`/`.fit.gz`) are gzip-compressed and stored as a `track_gpx` BLOB in the same row, not as separate files on disk (one thing to back up).
  - List/stats endpoints must never `SELECT` the `track_gpx` column — it's reserved for a future per-activity endpoint.
  - Distance/duration/avg/max-speed values always come directly from `activities.csv` columns; never recompute them from track points.
- **Auth:** single shared passcode (`.env` var `AUTH_PASSCODE`), verified server-side only. No accounts, no registration, no per-user model.
- **Deployment:** `docker compose up` with two services — `backend` (Ktor) and `frontend` (nginx serving the compiled wasmJs static bundle, proxying `/api/*` to `backend` over the compose-internal network).
- **Import flow:** user uploads the whole Strava export zip in one shot (drag-and-drop or file picker) → backend unzips it, streams-parses `activities.csv` entry-by-entry (avoid loading the whole archive into memory for large multi-year exports), upserts each row with `INSERT OR IGNORE` on `id`, and attaches a matching track file if present. Import must be safely repeatable — re-uploading the same or an overlapping archive must never create duplicates.

Explicitly out of scope: a trip detail screen, map/track visualization, direct Strava API integration, automatic sync, and multi-user/sharing support.

## Git workflow

Development follows Gitflow: never commit directly to `master`/`main`. Create a new branch for every change (e.g. `feature/*`, `fix/*`) and work there.

## Working with OpenSpec

This repo uses OpenSpec (`openspec/config.yaml`, schema `spec-driven`) to plan work before it's implemented:

- `openspec/specs/*/spec.md` holds the current, merged requirements per capability (`auth`, `activity-import`, `activity-dashboard`) — the source of truth for what's built today. Keep these in sync with behavior changes, even ones landed without an OpenSpec change (e.g. update the relevant `spec.md` alongside the code, not just after a formal `sync-specs`).
- `openspec/changes/` currently has no active (unimplemented) change — only `archive/`, which holds completed changes each with their own `proposal.md`, `design.md`, `tasks.md`, and `specs/*/spec.md` delta.
- Use the `openspec-*` skills (or `/opsx:*` slash commands) for this workflow: `propose` to draft a new change, `apply` to work through an existing change's `tasks.md`, `update` to revise a change's own artifacts, `sync-specs` to merge a change's delta specs into `openspec/specs/` once implemented, `archive-change` to finalize and move a completed change to `archive/`.
- When working through a change's `tasks.md`, follow it in order — each task has a stated verification step; treat that verification as part of the task, not optional polish.
