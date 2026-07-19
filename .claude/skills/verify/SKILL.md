---
name: verify
description: Run unit tests and flag anything that should be manually verified before committing. Use after making changes to confirm nothing is broken.
disable-model-invocation: true
---

## Step 1 — Unit tests

```bash
./gradlew testDebug
```

Check output for failures. Note: the project currently has only scaffold tests; failures likely mean a compilation error, not a logic bug.

## Step 2 — Build check

```bash
./gradlew assembleDebug
```

Confirms the app compiles cleanly. Fix any warnings about deprecated APIs before committing.

## Step 3 — Manual checklist (no linting configured)

Since no lint tool is set up, review these manually after code changes:

- [ ] New Composables follow the existing pattern: stateless where possible, state lifted to ViewModel.
- [ ] Coroutines are launched from `viewModel.scope` (not `GlobalScope`).
- [ ] Any new SharedPreferences keys are added to `AppSettings` (not scattered across Activities).
- [ ] `settings.refresh()` is called in `onResume()` if the new code reads settings state.
- [ ] If you added a new Activity, `onDestroy()` cleans up any coroutine scopes manually.

## Step 4 — Instrumented tests (optional, requires device)

```bash
./gradlew connectedAndroidTest
```
