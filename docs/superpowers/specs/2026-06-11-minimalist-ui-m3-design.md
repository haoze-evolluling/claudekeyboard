# Minimalist UI + M3 Migration Design

## Goal

Migrate the Claude Code Keyboard app from Material Components (M2) to Material Design 3, with a minimalist visual style: text + outlines only, no filled color blocks. Dark mode follows the system setting automatically.

## Approach

Use `Theme.Material3.DayNight.NoActionBar` as the base theme. Derive all colors from a single accent color `#0593ff` via M3's color system. Replace all `@color/xxx` references with `?attr/colorXxx` theme attributes so dark mode switches automatically.

## Color System

### Light Mode

| Role | Value | Usage |
|---|---|---|
| colorPrimary | #0593ff | Outlines, icons, accent text |
| colorOnPrimary | #ffffff | Text on Primary (rarely used) |
| colorSurface | #fafafa | Background |
| colorOnSurface | #1a1a1a | Primary text |
| colorOnSurfaceVariant | #666666 | Secondary text, hints |
| colorOutline | #cccccc | Borders, dividers |
| colorOutlineVariant | #e0e0e0 | Light borders |

### Dark Mode

| Role | Value | Usage |
|---|---|---|
| colorPrimary | #66b8ff | Outlines, icons (lighter variant) |
| colorOnPrimary | #003258 | Text on Primary |
| colorSurface | #121212 | Background |
| colorOnSurface | #e0e0e0 | Primary text |
| colorOnSurfaceVariant | #999999 | Secondary text |
| colorOutline | #444444 | Borders, dividers |
| colorOutlineVariant | #333333 | Light borders |

## Component Styles

### Top Bar
- Custom `LinearLayout` (no MaterialToolbar)
- Remove `elevation="2dp"`, use bottom divider instead
- Title: `?attr/colorOnSurface`, `TextAppearance.Material3.TitleLarge`
- Settings icon tint: `?attr/colorOnSurfaceVariant`

### Status Chip
- Style: `Widget.Material3.Chip.Assist` (no fill background)
- Border: `?attr/colorOutline`
- Connected state: stroke + text use Primary color
- Waiting/last-device state: stroke + text use `?attr/colorOnSurfaceVariant`

### Core Buttons (Yes / Yes to All / No)
- Style: `Widget.Material3.Button.OutlinedButton`
- Remove `backgroundTint`, use `app:strokeColor="?attr/colorOutline"`
- Text: `?attr/colorOnSurface`
- Ctrl+C: OutlinedButton with `app:strokeColor="?attr/colorPrimary"` + `android:textColor="?attr/colorPrimary"` for visual distinction

### Text Input
- Style: `Widget.Material3.TextInputLayout.OutlinedBox`
- `boxStrokeColor`: `?attr/colorOutline` (default) / `?attr/colorPrimary` (focused)
- Remove hardcoded `boxCornerRadius`, use M3 shape theme

### Macro Buttons
- Remove `MaterialCardView` wrapper
- Direct `Widget.Material3.Button.OutlinedButton`
- Border: `?attr/colorOutline`
- Text: `?attr/colorOnSurface`

### Dividers
- Keep, color: `?attr/colorOutlineVariant`

## Typography

Use M3 type scale via `android:textAppearance` instead of inline `textSize`:

| Element | Style |
|---|---|
| Top bar title | TextAppearance.Material3.TitleLarge |
| Status chip | TextAppearance.Material3.LabelMedium |
| Core buttons | TextAppearance.Material3.LabelLarge |
| Input text | TextAppearance.Material3.BodyLarge |
| Macro list header | TextAppearance.Material3.TitleSmall |
| Macro button text | TextAppearance.Material3.BodyMedium |
| Dialog title | TextAppearance.Material3.HeadlineSmall |

## Dark Mode

`Theme.Material3.DayNight` + `?attr/` references handle most of it automatically. Manual adjustments:

1. **Status Chip colors in Kotlin** (`MainActivity.kt`): Use theme color resources instead of hardcoded colors
2. **Settings icon** (`ic_settings.xml`): Change `fillColor="#000000"` to `@color/on_surface_variant`
3. **Status bar**: M3 DayNight theme auto-switches `windowLightStatusBar`

## Files to Modify

| File | Changes |
|---|---|
| `values/themes.xml` | Theme parent to M3, define light M3 color roles |
| `values-night/themes.xml` | Sync to M3, define dark M3 color roles |
| `values/colors.xml` | Add M3 color roles |
| `values-night/colors.xml` | **New file**, dark mode color overrides |
| `activity_main.xml` | All components to M3 styles, `?attr/` references, remove elevation |
| `item_macro_button.xml` | Remove CardView, use OutlinedButton |
| `dialog_macro_edit.xml` | TextInputLayout to M3 style |
| `ic_settings.xml` | fillColor to theme color |
| `MainActivity.kt` | Chip color logic to M3 color references |
| SplashTheme in themes.xml | Change to M3 base theme |

## Not Changed

- `BluetoothHidService.kt` — no UI changes
- `KeyboardSender.kt` — no UI changes
- `strings.xml` — no text changes
- Business logic — no functional changes
