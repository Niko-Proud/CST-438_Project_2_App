# Brainworm (Android App)

Brainworm is a small app for keeping track of the food you buy. You add your foods, and the app lets you know if any of them have been recalled by the FDA.

This repo is the Android app. The server is in our API repo:
https://github.com/Niko-Proud/CST-438_Project_2_API

## Team

- Victoria Ha
- Alexander Trujillo
- Nikolii Proud
- Hyun Jeong Lim

## Links

- Live API: https://brain-worm-api.onrender.com

The server can take a little while to wake up the first time.

## Tech stack

- Kotlin and Jetpack Compose
- Retrofit for API calls
- AppAuth for Google sign-in (PKCE)
- JUnit, MockWebServer, and Compose UI tests
- GitHub Actions

## Screens

1. **Login**: sign in with Google.
2. **My Items**: the foods you are tracking. Green means safe, orange means check the batch, and red means recalled.
3. **Food details**: recall info for one food. You can also edit it here, or add a new food.
4. **Admin**: only for admins. Manage users and view recalls.

## How to run

1. Clone the repo and open it in Android Studio.
   ```bash
   git clone https://github.com/Niko-Proud/CST-438_Project_2_App.git
   ```
2. Choose a server by adding lines to `local.properties`, then click **Sync Now**.

   Live API:
   ```properties
   API_BASE_URL=https://brain-worm-api.onrender.com/
   ```
   API on your computer (run this in the API repo first):
   ```bash
   docker compose up --build
   ```
   ```properties
   API_BASE_URL=http://10.0.2.2:8080/
   ```
   No server, just sample data:
   ```properties
   USE_FAKE_API=true
   ```
3. Pick the `app` configuration and a device, then click Run.

To sign in with Google, your account needs to be added as a test user, so just ask the team. In debug builds you can also tap **Skip login** to see the screens with sample data (use `USE_FAKE_API=true`).

## Tests

```bash
./gradlew testDebugUnitTest
```

The UI tests need an emulator or a phone:
```bash
./gradlew connectedDebugAndroidTest
```
