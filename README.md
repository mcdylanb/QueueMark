# Queuemark 🔖

> **Zero Hoarding. Just Reading.**
> A modern, local-first Android bookmarking utility engineered to eliminate the "digital graveyard" of unread links. 

Queuemark solves read-later cognitive load by organizing bookmarks around **time-to-read metrics** ("Quick Wins") and **intentional scheduling intents** right at the moment of capture. It is built using modern Android standards: Kotlin, Jetpack Compose (Material 3), Room DB, Firebase (Auth & Firestore), and WorkManager.

---

## 🚀 Getting Started

To get the project running locally, follow these steps. Since our team develops across mixed environments (**Apple Silicon macOS** and **Windows**), we maintain environment consistency natively rather than utilizing virtualization containers.

### 1. System Requirements & Setup
- **Java Development Kit (JDK):** JDK 17 (or JDK 21) is strictly required. Android Studio comes bundled with a compatible runtime, but ensure your system paths match.
- **Android Studio:** Ladybug (2024.2.1) or newer.
- **Git Configuration:** To prevent "line ending wars" in mixed OS environments, configure your global git variables before committing:
  - **Windows Users:** Run `git config --global core.autocrlf true`
  - **macOS/Linux Users:** Run `git config --global core.autocrlf input`

### 2. Clone the Repository
```bash
git clone https://github.com/mcdylanb/QueueMark.git
cd queuemark
```

### 3. Configure Local Properties
Create a `local.properties` file in the root directory if it wasn't auto-generated:
```properties
# macOS Example
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk

# Windows Example (Escape backslashes)
# sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```

### 4. Setup Firebase (Authentication & Cloud Firestore)
The project requires a configured Firebase project to support real-time local-first sync.
1. Go to the [Firebase Console](https://console.firebase.google.com/).
2. Create a new project named **Queuemark**.
3. Register an Android App using our application ID: `com.bookmarkapp.queuemark`.
4. Download the `google-services.json` config file.
5. Move the downloaded `google-services.json` file into your local `/app/` directory (e.g., `queuemark/app/google-services.json`).
6. Enable **Firebase Authentication** (Email/Password and Google Provider) and **Cloud Firestore** (in Test Mode or with proper security rules) in your Firebase dashboard.

### 5. Build and Run
We enforce using the Gradle Wrapper to ensure everyone builds with the exact same Gradle version.
- **macOS / Linux:**
  ```bash
  ./gradlew assembleDebug
  ```
- **Windows (Command Prompt / Powershell):**
  ```cmd
  gradlew.bat assembleDebug
  ```
Open the project in Android Studio, allow Gradle to sync, and press **Run (Shift + F10)** targeting an emulator or physical device.

---

## 🤝 Contribution Guidelines

We maintain high code-quality standards and structured branching workflows to make sure our code is modular and reliable.

### 1. Branching Strategy (GitHub Flow)
All active developments must happen on feature branches branched from `main`. Direct pushes to `main` are blocked.

- **Branch Naming Conventions:**
  - Core Features: `feature/your-feature-name` (e.g., `feature/room-database`)
  - Bug Fixes: `bugfix/issue-description` (e.g., `bugfix/sync-worker-retry`)
  - Refactoring: `refactor/scoped-refactor` (e.g., `refactor/theme-tokens`)

### 2. Standard Development Loop
1. **Pull the latest shifts:** Ensure your local copy is updated:
   ```bash
   git checkout main
   git pull origin main
   ```
2. **Create a topic branch:**
   ```bash
   git checkout -b feature/auth-viewmodel
   ```
3. **Commit your changes:** Follow descriptive, conventional commit guidelines:
   ```bash
   git commit -m "feat(auth): implement anonymous sign-in state in AuthViewModel"
   ```
4. **Push and Open a Pull Request (PR):** Push your branch to GitHub and open a PR targeting `main`.
5. **Peer Review:** Every PR requires a minimum of **one approved peer review** and a passing local build before it can be merged.

### 3. Core Architecture Rules (Clean MVVM)
To prevent spaghetti code, align with these development guidelines:
- **Single Source of Truth:** The UI must always query data from `BookmarkRepository`. Never call `BookmarkDao` or `FirestoreService` directly from ViewModels or Screens.
- **State Management:** ViewModels must expose a single read-only `StateFlow<UiState>` following Unidirectional Data Flow (UDF). Composables must remain functional and stateless by hoisting events up to the ViewModel via UI Actions.
- **Coroutines Threading:** Never hardcode dispatchers. Inject dispatchers or run heavy tasks exclusively using structured contexts such as `Dispatchers.IO`.

---

## 🛠️ Diagnostics & Troubleshooting
* **Gradle Build Errors:** Try cleaning up stale build artifacts:
  ```bash
  ./gradlew clean
  ```
* **Lint & Formatting:** To verify formatting rules are consistent before pushing code, run:
  ```bash
  ./gradlew ktlintCheck
  ```

---

*For detailed architectural layouts, color palettes, and database entity relationships, please refer to our [Queuemark - Technical Design Document](https://docs.google.com/document/d/1tvlg3WMHVN7qRhb4WCzJmm1hgjzx2_HSv0I5775Rzmg/edit?usp=drive_web).*