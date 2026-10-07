# DayFlow

An offline habit tracker and goal planner focused on simple daily consistency.

## Overview

DayFlow is a private, lightweight, and completely offline habit tracker and goal management application for Android.

- **100% Offline & Private:** All habit logs, streaks, and personal goals exist exclusively on your device. No account creation, cloud servers, advertising, or network tracking.
- **Daily Focus:** Clean dashboard for today's habits with one-tap completion and streak tracking.
- **Goal Milestones:** Break long-term objectives down into manageable milestones and link daily habits directly to them.
- **Local Reminders:** Reliable daily and weekly reminders scheduled via Android AlarmManager with a gentle notification sound.

## Building and Testing

Prerequisites:
- JDK 17 or higher
- Android SDK (API 36)

### Clean and Build

```bash
# Clean project
./gradlew clean

# Run all unit and Robolectric tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug

# Generate release bundle (AAB)
STORE_PASSWORD=<your_store_password> KEY_PASSWORD=<your_key_password> ./gradlew bundleRelease
```

### Windows

```cmd
gradlew.bat clean
gradlew.bat testDebugUnitTest
gradlew.bat assembleDebug
set STORE_PASSWORD=<your_store_password>
set KEY_PASSWORD=<your_key_password>
gradlew.bat bundleRelease
```

## Release Signing

For release builds (`bundleRelease`), provide keystore credentials via environment variables:
- `STORE_PASSWORD`: Keystore password
- `KEY_PASSWORD`: Key password
- `KEYSTORE_PATH`: (Optional) Custom path to `.jks` file (defaults to `my-upload-key.jks`)
- `KEY_ALIAS`: (Optional) Key alias (defaults to `upload`)
