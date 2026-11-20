# RachaJusto

**RachaJusto** is a native Android application for smart group bill splitting, cash-flow transfer minimization, and local EMV PIX BR Code generation.

## Product Vision

Splitting group expenses during trips, barbecues, or shared apartment bills often turns messy when participants consume different items (such as non-alcoholic drinks vs. alcoholic beverages) or hold different shares. Without debt simplification, a group of six people ends up making dozens of cross-transfers when only a few payments are needed.

RachaJusto solves this with:
- Exact integer arithmetic in cents (`Long`) for all monetary values (BRL / R$) to eliminate floating-point rounding bugs.
- Four expense splitting modes: **Equal**, **Itemized (Receipt + Proportional Service Fee)**, **Exact Amounts**, and **Shares / Weights**.
- A **Cash Flow Minimization** engine that reduces cross-debts to the fewest possible transfers while tracking partial settlements.
- Local **EMV PIX BR Code** payload generation (CRC16-CCITT) and on-device QR Code rendering.
- Multi-language support selectable directly inside the app: **English**, **Portuguese (Brazil)**, **Spanish**, and **French**.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (Light & Dark themes, edge-to-edge)
- **Architecture:** Clean layered architecture (`core`, `domain`, `data`, `feature/*`) with MVVM + Unidirectional Data Flow (`StateFlow`)
- **Async & Navigation:** Kotlin Coroutines, Navigation Compose
- **Local Persistence (Phase 1):** Versioned JSON storage (`rachajusto_v1.json`) via `kotlinx.serialization`
- **QR Code Rendering:** `com.google.zxing:core` drawn locally onto Compose `Canvas`
- **Testing:** JUnit 4 + `kotlinx-coroutines-test`

## Build and Test

Requires JDK 17 and Android SDK (API 36).

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## Development Roadmap

1. Project foundation, layered architecture, Material 3 theme, navigation, and 4-language i18n setup.
2. Domain engines: exact-cent split calculators, cash-flow minimizer, EMV PIX BR Code generator, and validation rules.
3. Mock-first data layer: network latency/error simulator, versioned local JSON persistence, dynamic seed data, and demo receipt scanner.
4. Groups list and group creation flows.
5. Group detail screen with Expenses, Balances, Optimized Settlements, and Participants/WhatsApp summary sharing.
6. Four-mode expense creation flow with demo receipt scanning.
7. PIX payment modal with local QR Code rendering and settlement confirmation/undo.
8. Demo mode controls, language switcher, UI polish, and full test coverage.
