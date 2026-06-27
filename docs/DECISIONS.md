# Decisions

## Monorepo

### Confirmed From Code

The project is kept as one repository with Android, backend, connector, and docs together.

### Rationale

Early-stage work changes JSON contracts, mobile behavior, backend import logic, and connector behavior together. A monorepo keeps those changes visible in one place.

## Local-First Android

### Confirmed From Code

The Android app loads built-in assets and stores imported/edited lessons locally.

### Rationale

This keeps the mobile app usable without a backend while the server side is still a prototype.

## SQLite First, PostgreSQL Later

### Confirmed From Code

Django uses SQLite by default. Environment configuration allows a future `DATABASE_URL` for PostgreSQL.

### Rationale

SQLite is simple for local learning and test data. PostgreSQL should be introduced when multi-user/server deployment becomes real.

## Minimum Android SDK

### Confirmed From Code

Android `minSdk = 34`, `targetSdk = 35`, `compileSdk = 35`.

### Rationale

Recent offline SpeechRecognizer APIs require modern Android versions.

## English Fallback After Encoding Incident

### Confirmed From Code

Core UI labels now route through clean English fallback text after corrupted localized literals broke the UI.

### Rationale

Immediate stability is more important than partial broken localization. v0.02 should rebuild localization safely.

## Product Naming

### Confirmed From Code

Android display strings were renamed to MurrLex. Package paths still contain `polishcards`.

### Uncertain / Needs User Confirmation

Whether package id and internal class/package names should be renamed later.

## Backend Auth

### Confirmed From Code

The lab UI uses Django login. Internal import API uses bearer token.

### Rationale

This is enough for a temporary private demo, but not a final auth model.

