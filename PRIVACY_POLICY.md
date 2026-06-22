# Privacy Policy - Current Mobile App

**Effective Date**: June 22, 2026

Current (package name `com.sudegoratechglobal.current`) is designed as a local-first, zero-server application. We prioritize your data privacy above all. Because there are no backend servers managed by us, your productivity tracks and task entries remain completely under your control.

## 1. Data Collection & Processing
- **No Account Sign-up**: No account registration or cloud sign-in is required to use the app. Your onboarding name is saved locally in SharedPreferences.
- **Task and Focus Entries**: All tasks, priorities, notes, timers, and tracking metrics are stored locally on your device within an encrypted SQLite database via Room. We cannot access, inspect, or retrieve any of this data.
- **Analytics and Tracking**: We do not collect telemetry, crash metrics, or third-party cookies.

## 2. Permissions Disclosures
- **Internet Permission (`android.permission.INTERNET`)**: Required only to fetch shared team sync spaces (Single-Link Space) and execute private Google Drive backups.
- **Notifications Permission (`android.permission.POST_NOTIFICATIONS`)**: Required to trigger system alerts for timer completions and Procrastination Tax deadlines.
- **Exact Alarms (`android.permission.SCHEDULE_EXACT_ALARM`)**: Required to schedule exact, precise timings for Pomodoro countdowns and lock-in deadliness.
- **Send SMS Permission (`android.permission.SEND_SMS`)**: Required to programmatically alert your study partner/boss if you miss a locked task deadline (Procrastination Tax). If this permission is denied, the app falls back to opening your system SMS client with a pre-filled message.

## 3. Remote Cloud Backup (Optional)
- **Google Drive Auth**: When linking Google Drive in Settings, the app connects directly to Google APIs using native OAuth.
- **Isolated App Data Folder**: Backups are written directly to your private Google Drive in an isolated app folder (`drive.appdata` scope). Developers have no visibility or access to this storage or your JSON payloads.

## 4. Updates & Contact
If you have questions about this policy, you can review the codebase directly on GitHub or contact our support team.
