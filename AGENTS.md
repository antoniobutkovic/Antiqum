# Project Rules

- Keep feature code under `composeApp/src/commonMain/kotlin/com/strive/antiqum/<feature>/`.
- Use `data/` for services, repositories, and models, and `ui/` for screens, UI state, and view models.
- Keep dependencies one-way: `Screen -> ViewModel -> Repository -> Service -> HttpClient`.
- Wire feature dependencies in `composeApp/src/commonMain/kotlin/com/strive/antiqum/di/AppComponent.kt`.
- Screens must not call services directly.
- View models must not depend on `HttpClient` directly.
- For feature implementation, follow `ARCHITECTURE_RULES.md`.
