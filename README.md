# StoryStream

This app has two parts:

- `backend/api` — Spring Boot API, run with Docker Compose
- `readerapp` — Android app, run from Android Studio

## Quick start

1. Install and start Docker Desktop.
2. Clone the repo and open a terminal in the project root:
   ```bash
   cd "/news-reader"
   ```
3. Create a root `.env` file:
   ```bash
   cp .env.example .env
   ```
4. Edit `.env` and set `STORYSTREAM_GEMINI_API_KEY`.
5. Start the backend and database:
   ```bash
   docker compose up --build
   ```
6. Verify the backend is responding:
   ```bash
   curl -sS "http://localhost:8080/api/articles?page=0&size=1"
   ```
7. Open `readerapp` in Android Studio.
8. Copy `readerapp/env.example.properties` to `readerapp/env.local.properties` if needed and set `BASE_URL`.
9. Sync Gradle and click **Run**.

---

## Backend setup

Run everything from the project root:

```bash
cd "/news-reader"
```

### 1. Create `.env`

```bash
cp .env.example .env
```

Edit `.env` and set:

```dotenv
STORYSTREAM_GEMINI_API_KEY=replace-with-your-current-gemini-api-key
# Get a free Gemini API key from https://aistudio.google.com/
ALLOW_GEMINI_DEBUG=true
POSTGRES_DB=storystream
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
```

### 2. Start backend + database

```bash
docker compose up --build
```

This starts:

- PostgreSQL
- Spring Boot backend

What is automatic:

- database container startup
- database creation
- database user/password creation
- Spring Boot startup inside Docker
- schema creation/update through Spring JPA/Hibernate

What is not required:

- manual backend startup with Gradle
- manual Postgres setup
- manual SQL init scripts
- pgAdmin

### 3. Verify backend

Use a public endpoint:

```bash
curl -sS "http://localhost:8080/api/articles?page=0&size=1"
```

Optional logs:

```bash
docker compose logs -f backend
```

Stop the stack:

```bash
docker compose down
```

---

## Android setup

Open this project in Android Studio:

```text
/news-reader/readerapp
```

### Android SDK file: `local.properties`

`readerapp/local.properties` is the standard Android/Gradle SDK file.

It is **not** where you set the API base URL.

For this project, a user normally only needs this in `readerapp/local.properties`:

```properties
sdk.dir=/Users/your-username/Library/Android/sdk
```

Android Studio usually creates this automatically. If it does not, set `sdk.dir` to your local Android SDK path.

### Base URL configuration

The Android app reads `BASE_URL` from these files in `readerapp`:

1. `env.local.properties`
2. `env.example.properties` fallback

This value is compiled into the app in:

- `readerapp/app/build.gradle.kts`

and used by:

- `readerapp/app/src/main/java/com/storystream/reader_app/di/DiNetworkModule.kt`

### Set the correct `BASE_URL`

If `readerapp/env.local.properties` does not exist, create it from the example:

```bash
cd "/news-reader/readerapp"
cp env.example.properties env.local.properties
```

Then edit `readerapp/env.local.properties`.

#### Android emulator

```properties
BASE_URL=http://10.0.2.2:8080/
```

#### Physical Android device

Use the Mac's LAN IP on the same Wi‑Fi network:

```properties
BASE_URL=http://10.0.0.85:8080/
```

Find the current IP on macOS:

```bash
ipconfig getifaddr en0
```

### Run the app

- Open `readerapp` in Android Studio
- Let Gradle sync finish
- Select emulator or physical device
- Click **Run**

Optional terminal install:

```bash
cd "/news-reader/readerapp"
./gradlew :app:installDebug
```

---

## Local networking notes

### Emulator

Use:

```text
http://10.0.2.2:8080/
```

### Physical device

Use:

```text
http://<your-mac-lan-ip>:8080/
```

Requirements:

- phone and Mac on the same Wi‑Fi network
- backend running on the Mac
- port `8080` reachable from the phone

The Android app allows cleartext HTTP for local development.

---

## Setup checklist

### Backend

- [ ] Docker Desktop is running
- [ ] `.env` exists in the repo root
- [ ] `docker compose up --build` starts the backend and database
- [ ] `http://localhost:8080/api/articles?page=0&size=1` returns JSON

### Android

- [ ] Android Studio opens `readerapp`
- [ ] Gradle sync succeeds
- [ ] `BASE_URL` is set correctly in `readerapp/env.local.properties`
- [ ] Emulator uses `10.0.2.2`
- [ ] Physical device uses the correct LAN IP
- [ ] App runs and can reach the backend

---

## Troubleshooting

### Port `8080` already in use

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
```

### Backend not reachable from physical device

Check:

- same Wi‑Fi network
- correct LAN IP in `readerapp/env.local.properties`
- backend is running
- Mac firewall or VPN is not blocking traffic

### Android app still using the old backend URL

Rebuild the app after changing `env.local.properties`:

```bash
cd "/Users/ryanortiz/Desktop/news-reader/readerapp"
./gradlew clean :app:installDebug
```
