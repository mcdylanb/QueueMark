# Queuemark — Task Handoff

Full plan and rationale: see `.ai/` docs (DESIGN.md, DATABASE_SCHEMA.md, INSTRUCTIONS.md, REQUIREMENTS.md, WORKFLOW.md).
Approved decisions (do not revisit without team discussion):
- Package/applicationId: `com.bookmarkapp.queuemark`
- DI: **Hilt** (KSP)
- Design source of truth: `.ai/DESIGN.md` simple spec (Slate Teal `#2B5C55` primary, tabs + FAB dashboard, **no bottom nav**, `dynamicColor = false`)
- Firebase (Auth + Firestore) wired from day one; Dylan provides `app/google-services.json`

Git rules (graded, 20%): work on feature branches, PR into `main`, ≥1 review, no direct push to main, task-scoped commits.

## Current state — branch `feature/01-project-setup` (BUILDS GREEN ✅, uncommitted)

`./gradlew :app:assembleDebug :app:testDebugUnitTest` passes. Done:
- [x] Package rename → `com.bookmarkapp.queuemark` (git mv + all package/import lines + instrumented-test assertion + namespace/applicationId)
- [x] Full version catalog: **kotlin 2.3.21, ksp 2.3.10, hilt 2.60.1**, androidxHilt 1.3.0, room 2.8.2, work 2.10.5, navigationCompose 2.9.6, firebaseBom 34.3.0, googleServices 4.4.4, coroutines 1.10.2, jsoup 1.21.2
- [x] Hilt wired end-to-end: plugins + deps, `QueuemarkApplication` (@HiltAndroidApp) registered in manifest, `@AndroidEntryPoint` MainActivity
- [x] Firebase SDK deps added (Auth, Firestore via BoM) — google-services **plugin** commented out until the json exists
- [x] Theme: `Color.kt` = the 5 token pairs from `.ai/DESIGN.md`; `Theme.kt` light/dark schemes, dynamic color removed, Amber mapped to `tertiary`
- [x] Manifest: INTERNET permission
- [x] `compileSdk = 37` (compose BOM 2026.02 lifecycle requires API 37)

### Remaining Phase 1 checklist
1. **Dylan:** create Firebase project, register Android app `com.bookmarkapp.queuemark`, enable Anonymous + Email/Password auth and Firestore, download `google-services.json` → place at `app/google-services.json`
2. Uncomment `alias(libs.plugins.google.services)` in `app/build.gradle.kts`, rebuild
3. Commit the json (Firebase Android config is not a secret; the team can't build without it)
4. Emulator check (API 31+): Slate Teal branding; system dark mode shows charcoal palette (proves dynamic color is off)
5. Open PR `feature/01-project-setup` → `main`

### Toolchain — how the AGP 9 / Hilt / KSP conflict was resolved (do NOT "clean up" these settings)
AGP 9.2.1's new built-in Kotlin is **incompatible with KSP** (which Hilt and Room require), and the standalone Kotlin plugin is incompatible with AGP 9's new DSL. The working combination, each step prescribed by the plugins' own error messages:
- `gradle.properties`: `android.builtInKotlin=false` **and** `android.newDsl=false`
- `org.jetbrains.kotlin.android` plugin IS applied (kotlin 2.3.21) — pre-AGP-9 style
- `app/build.gradle.kts` starts with `@file:Suppress("DEPRECATION_ERROR", "DEPRECATION")` because AGP 9 marks the classic DSL as error-level deprecated; classic DSL syntax is used (`compileSdk = 37`, `isMinifyEnabled`), jvmTarget 11 via `kotlin { compilerOptions { } }`
- These are temporary escape hatches that die at AGP 10; fine for this project's lifetime. Alternative if they ever break: downgrade to AGP 8.13.x + Gradle 8.14.x.
- Other rules: keep KSP/Kotlin versions paired; configuration cache stays ON; Room uses `@Database(exportSchema = false)`.

---

## Phase 2 — Local data layer (branch `feature/02-data-layer`)
All under `app/src/main/java/com/bookmarkapp/queuemark/`:
- `data/local/BookmarkEntity.kt` — copy exactly from `.ai/DATABASE_SCHEMA.md` (String UUID PK, url, title, description?, estimatedReadTime=1, createdAt, reminderTime?, isCompleted=false, completedAt?, isSynced=false)
- `data/local/BookmarkDao.kt` — Flow queries: observeActive (isCompleted=0, createdAt ASC), observeCompleted (completedAt DESC), observeQuickWins (active AND estimatedReadTime < 5), search (LIKE over title/description/url), observeById; suspend: getUnsynced (isSynced=0), getById, @Upsert, deleteById, setCompleted(id, completedAt) → also sets isSynced=0, markSynced(id), setReminder(id, time)
- `data/local/BookmarkDatabase.kt` — version 1, exportSchema=false
- `domain/model/Bookmark.kt` — domain model + entity↔domain mappers
- `domain/ReadTimeCalculator.kt` — `max(1, round(wordCount / 200.0))`
- `data/repository/BookmarkRepository.kt` (interface) + `BookmarkRepositoryImpl.kt` — Result-wrapped suspend ops on injected `@IoDispatcher`; every write sets isSynced=false and calls injected `SyncScheduler` interface (**no-op binding now**, real one in Phase 5)
- `di/AppModule.kt` (dispatcher qualifiers), `di/DatabaseModule.kt`, `di/RepositoryModule.kt` (@Binds)
- Gradle: room-runtime/ktx, ksp(room-compiler), room-testing (androidTest), kotlinx-coroutines-test
- **Tests:** ReadTimeCalculatorTest (0→1, 999→5, rounding), BookmarkDaoTest (in-memory: quick-wins boundary 4-in/5-out, search all 3 columns, unsynced round-trip), BookmarkRepositoryImplTest (fake DAO)
- Verify: `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest`

## Phase 3 — Auth + navigation (branch `feature/03-auth-navigation`)
- `data/remote/AuthRepository.kt` + impl over FirebaseAuth: `authState: Flow<AuthUser?>` (callbackFlow on AuthStateListener), signInAnonymously, signInWithEmail, signUpWithEmail — Result-wrapped, `.await()`
- `ui/navigation/NavGraph.kt` — string routes `"auth"`, `"dashboard"`, `"detail/{bookmarkId}"`; start destination from auth state
- `ui/auth/AuthViewModel.kt` — @HiltViewModel, single `StateFlow<AuthUiState>`, `sealed interface AuthUiAction` → one `onAction()` (INSTRUCTIONS.md §3.2 pattern)
- `ui/auth/AuthScreen.kt` — per `.ai/DESIGN.md` screen 1 ("Bookmark." headline, "Zero Hoarding. Just Reading.", email/password 12dp fields, Sign In, disabled Google button = stretch, "Continue offline for now" → anonymous). Light+dark @Previews (required for every composable, all phases)
- Dashboard placeholder scaffold; MainActivity hosts NavGraph
- Gradle: navigation-compose, hilt-navigation-compose, lifecycle-viewmodel-compose
- Verify: cold start → Auth; Continue offline → anonymous user in Firebase console → dashboard; restart skips auth

## Phase 4 — Dashboard + Detail (branch `feature/04-dashboard-detail`)
- `ui/dashboard/DashboardViewModel.kt` — combine(quickWins, active/completed, search) → one StateFlow, stateIn WhileSubscribed(5000); sealed actions
- `ui/dashboard/DashboardScreen.kt` — header + sync dot, search field, Quick Wins LazyRow (tertiary/amber border), TabRow Unread/Completed, LazyColumn with SwipeToDismissBox (delete / toggle-complete), FAB → AddBookmarkBottomSheet. **No bottom nav.**
- `ui/dashboard/components/`: BookmarkCard, QuickWinCard, AddBookmarkBottomSheet
- `data/remote/UrlMetadataService.kt` — jsoup: title/description/body word count → ReadTimeCalculator; 10s timeout, IO dispatcher
- `ui/detail/DetailViewModel.kt` — SavedStateHandle id; wall-clock session timer via injected clock; ≥80% of estimatedReadTime → showCompletePrompt (WORKFLOW.md flow 3)
- `ui/detail/DetailScreen.kt` — top bar (back, complete toggle, share); AndroidView WebView; BackHandler: WebView.canGoBack() first, else session-end check → "Mark as complete?" dialog
- Gradle: jsoup
- Tests: DashboardViewModelTest (fake repo), DetailViewModel 80% threshold (fake clock)

## Phase 5 — Share target + reminders + Firestore sync (branch `feature/05-share-sync`)
- `ui/share/ShareTargetActivity.kt` — @AndroidEntryPoint, translucent theme, ACTION_SEND text/plain filter; sheet shows immediately, scrape+insert in parallel
- `ui/share/ShareViewModel.kt` + `ScheduleReminderBottomSheet.kt` — Tonight 8 PM / Tomorrow 9 AM / Sat 10 AM / Custom pickers; pure `ReminderTimeCalculator` (injected clock) + unit test
- `data/remote/FirestoreService.kt` — `/users/{uid}/bookmarks/{id}` set().await(); snapshot-listener callbackFlow
- `data/remote/SyncWorker.kt` — @HiltWorker CoroutineWorker: no uid → retry; push getUnsynced → markSynced; NetworkType.CONNECTED constraint, exponential backoff
- `data/remote/NotificationWorker.kt` — skip if completed; notification → PendingIntent to MainActivity + bookmarkId → detail
- `WorkManagerSyncScheduler` replaces the Phase 2 no-op: unique work `"bookmark_sync"` (APPEND_OR_REPLACE) and `"reminder_$bookmarkId"` (REPLACE, setInitialDelay)
- Remote→local mirror: **skip upsert when local row has isSynced == false** (echo suppression, DATABASE_SCHEMA.md rule 2)
- `QueuemarkApplication` implements `Configuration.Provider` with injected HiltWorkerFactory
- Manifest: POST_NOTIFICATIONS (+ runtime request API 33+), ShareTargetActivity + translucent theme style, **remove default WorkManager initializer** (InitializationProvider meta-data `tools:node="remove"` — forgetting this crashes @HiltWorker at runtime)
- Gradle: work-runtime-ktx, androidx-hilt-work, ksp(androidx-hilt-compiler) — androidx compiler is in addition to Dagger's
- Verify end-to-end: share from Chrome → sheet → 2-min custom reminder → notification → correct detail. Sync: airplane mode add (isSynced=0, nothing in console) → airplane off → doc appears, flag flips.

## Timeline
Week 1: P1+P2 · Week 2: P3 + start P4 · Week 3: finish P4 + start P5 · Week 4: finish P5, polish (accessibility/text scaling), README/demo.
