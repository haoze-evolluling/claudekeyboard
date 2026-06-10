# Minimalist UI + M3 Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the app from M2 to M3 with a minimalist text+outline style and automatic dark mode support.

**Architecture:** Switch theme to `Theme.Material3.DayNight.NoActionBar`, define M3 color roles for light/dark, replace all M2 component styles with M3 equivalents, and update hardcoded colors in Kotlin to use theme-aware resources.

**Tech Stack:** Android XML Views, Material Components 1.10.0, Kotlin

---

### Task 1: Define M3 Color System

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values-night/colors.xml`

- [ ] **Step 1: Rewrite `values/colors.xml` with M3 color roles**

Replace the entire file with M3 color roles based on `#0593ff`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Basic -->
    <color name="black">#FF000000</color>
    <color name="white">#FFFFFFFF</color>

    <!-- M3 Color Roles - Light Mode -->
    <color name="primary">#0593FF</color>
    <color name="on_primary">#FFFFFF</color>
    <color name="primary_container">#D1E4FF</color>
    <color name="on_primary_container">#001D36</color>

    <color name="secondary">#546E7A</color>
    <color name="on_secondary">#FFFFFF</color>
    <color name="secondary_container">#CFD8DC</color>
    <color name="on_secondary_container">#0E1D22</color>

    <color name="tertiary">#6B5778</color>
    <color name="on_tertiary">#FFFFFF</color>
    <color name="tertiary_container">#F2DAFF</color>
    <color name="on_tertiary_container">#251432</color>

    <color name="error">#BA1A1A</color>
    <color name="on_error">#FFFFFF</color>
    <color name="error_container">#FFDAD6</color>
    <color name="on_error_container">#410002</color>

    <color name="surface">#FAFAFA</color>
    <color name="on_surface">#1A1A1A</color>
    <color name="on_surface_variant">#666666</color>
    <color name="surface_variant">#E0E0E0</color>

    <color name="outline">#CCCCCC</color>
    <color name="outline_variant">#E0E0E0</color>

    <color name="inverse_surface">#2E2E2E</color>
    <color name="inverse_on_surface">#F0F0F0</color>
    <color name="inverse_primary">#9ECAFF</color>

    <!-- Status colors (semantic, not M3 roles) -->
    <color name="status_connected">#0593FF</color>
    <color name="status_waiting">#666666</color>
    <color name="status_disconnected">#BA1A1A</color>
</resources>
```

- [ ] **Step 2: Create `values-night/colors.xml` for dark mode**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- M3 Color Roles - Dark Mode -->
    <color name="primary">#66B8FF</color>
    <color name="on_primary">#003258</color>
    <color name="primary_container">#00497D</color>
    <color name="on_primary_container">#D1E4FF</color>

    <color name="secondary">#90A8B0</color>
    <color name="on_secondary">#1B333B</color>
    <color name="secondary_container">#324A52</color>
    <color name="on_secondary_container">#CFD8DC</color>

    <color name="tertiary">#D6BEE4</color>
    <color name="on_tertiary">#3A2A49</color>
    <color name="tertiary_container">#523F60</color>
    <color name="on_tertiary_container">#F2DAFF</color>

    <color name="error">#FFB4AB</color>
    <color name="on_error">#690005</color>
    <color name="error_container">#93000A</color>
    <color name="on_error_container">#FFDAD6</color>

    <color name="surface">#121212</color>
    <color name="on_surface">#E0E0E0</color>
    <color name="on_surface_variant">#999999</color>
    <color name="surface_variant">#444444</color>

    <color name="outline">#444444</color>
    <color name="outline_variant">#333333</color>

    <color name="inverse_surface">#E0E0E0</color>
    <color name="inverse_on_surface">#1A1A1A</color>
    <color name="inverse_primary">#0593FF</color>

    <!-- Status colors (semantic, not M3 roles) -->
    <color name="status_connected">#66B8FF</color>
    <color name="status_waiting">#999999</color>
    <color name="status_disconnected">#FFB4AB</color>
</resources>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml
git commit -m "feat: define M3 color system with light/dark mode"
```

---

### Task 2: Migrate Theme to M3

**Files:**
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`

- [ ] **Step 1: Rewrite `values/themes.xml`**

Replace the entire file:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">

    <style name="Theme.Claudekeyboard" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Primary -->
        <item name="colorPrimary">@color/primary</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="colorPrimaryContainer">@color/primary_container</item>
        <item name="colorOnPrimaryContainer">@color/on_primary_container</item>

        <!-- Secondary -->
        <item name="colorSecondary">@color/secondary</item>
        <item name="colorOnSecondary">@color/on_secondary</item>
        <item name="colorSecondaryContainer">@color/secondary_container</item>
        <item name="colorOnSecondaryContainer">@color/on_secondary_container</item>

        <!-- Tertiary -->
        <item name="colorTertiary">@color/tertiary</item>
        <item name="colorOnTertiary">@color/on_tertiary</item>
        <item name="colorTertiaryContainer">@color/tertiary_container</item>
        <item name="colorOnTertiaryContainer">@color/on_tertiary_container</item>

        <!-- Error -->
        <item name="colorError">@color/error</item>
        <item name="colorOnError">@color/on_error</item>
        <item name="colorErrorContainer">@color/error_container</item>
        <item name="colorOnErrorContainer">@color/on_error_container</item>

        <!-- Surface & Background -->
        <item name="android:colorBackground">@color/surface</item>
        <item name="colorSurface">@color/surface</item>
        <item name="colorOnSurface">@color/on_surface</item>
        <item name="colorOnSurfaceVariant">@color/on_surface_variant</item>
        <item name="colorSurfaceVariant">@color/surface_variant</item>

        <!-- Outline -->
        <item name="colorOutline">@color/outline</item>
        <item name="colorOutlineVariant">@color/outline_variant</item>

        <!-- Inverse -->
        <item name="colorInverseSurface">@color/inverse_surface</item>
        <item name="colorInverseOnSurface">@color/inverse_on_surface</item>
        <item name="colorInversePrimary">@color/inverse_primary</item>

        <!-- Status & navigation bars -->
        <item name="android:statusBarColor">@color/surface</item>
        <item name="android:navigationBarColor">@color/surface</item>
    </style>

    <!-- Splash screen theme -->
    <style name="SplashTheme" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="android:colorBackground">@color/surface</item>
        <item name="android:statusBarColor">@color/surface</item>
        <item name="android:navigationBarColor">@color/surface</item>
    </style>

</resources>
```

- [ ] **Step 2: Rewrite `values-night/themes.xml`**

```xml
<resources xmlns:tools="http://schemas.android.com/tools">

    <style name="Theme.Claudekeyboard" parent="Theme.Material3.DayNight.NoActionBar">
        <!-- Primary -->
        <item name="colorPrimary">@color/primary</item>
        <item name="colorOnPrimary">@color/on_primary</item>
        <item name="colorPrimaryContainer">@color/primary_container</item>
        <item name="colorOnPrimaryContainer">@color/on_primary_container</item>

        <!-- Secondary -->
        <item name="colorSecondary">@color/secondary</item>
        <item name="colorOnSecondary">@color/on_secondary</item>
        <item name="colorSecondaryContainer">@color/secondary_container</item>
        <item name="colorOnSecondaryContainer">@color/on_secondary_container</item>

        <!-- Tertiary -->
        <item name="colorTertiary">@color/tertiary</item>
        <item name="colorOnTertiary">@color/on_tertiary</item>
        <item name="colorTertiaryContainer">@color/tertiary_container</item>
        <item name="colorOnTertiaryContainer">@color/on_tertiary_container</item>

        <!-- Error -->
        <item name="colorError">@color/error</item>
        <item name="colorOnError">@color/on_error</item>
        <item name="colorErrorContainer">@color/error_container</item>
        <item name="colorOnErrorContainer">@color/on_error_container</item>

        <!-- Surface & Background -->
        <item name="android:colorBackground">@color/surface</item>
        <item name="colorSurface">@color/surface</item>
        <item name="colorOnSurface">@color/on_surface</item>
        <item name="colorOnSurfaceVariant">@color/on_surface_variant</item>
        <item name="colorSurfaceVariant">@color/surface_variant</item>

        <!-- Outline -->
        <item name="colorOutline">@color/outline</item>
        <item name="colorOutlineVariant">@color/outline_variant</item>

        <!-- Inverse -->
        <item name="colorInverseSurface">@color/inverse_surface</item>
        <item name="colorInverseOnSurface">@color/inverse_on_surface</item>
        <item name="colorInversePrimary">@color/inverse_primary</item>

        <!-- Status & navigation bars -->
        <item name="android:statusBarColor">@color/surface</item>
        <item name="android:navigationBarColor">@color/surface</item>
    </style>

</resources>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml
git commit -m "feat: migrate theme from M2 to M3 DayNight"
```

---

### Task 3: Update `activity_main.xml` to M3 Styles

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Rewrite `activity_main.xml`**

Replace the entire file:

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/main"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/colorSurface"
    android:orientation="vertical"
    tools:context=".MainActivity">

    <!-- Top Bar -->
    <LinearLayout
        android:id="@+id/top_bar"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="?attr/colorSurface"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingHorizontal="16dp">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:ellipsize="end"
            android:maxLines="1"
            android:text="@string/app_name"
            android:textAppearance="?attr/textAppearanceTitleLarge"
            android:textColor="?attr/colorOnSurface" />

        <com.google.android.material.chip.Chip
            android:id="@+id/status_chip"
            style="@style/Widget.Material3.Chip.Assist"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="8dp"
            android:text="@string/status_waiting"
            android:textAppearance="?attr/textAppearanceLabelMedium"
            android:textColor="@color/status_waiting"
            app:chipBackgroundColor="@android:color/transparent"
            app:chipIcon="@android:drawable/ic_dialog_info"
            app:chipIconTint="@color/status_waiting"
            app:chipMinHeight="32dp"
            app:chipStrokeColor="@color/outline"
            app:chipStrokeWidth="1dp" />

        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_disconnect"
            style="@style/Widget.Material3.Button.TextButton"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_disconnect"
            android:textAppearance="?attr/textAppearanceLabelMedium"
            android:textColor="?attr/colorPrimary"
            android:visibility="gone" />

        <ImageButton
            android:id="@+id/btn_settings"
            android:layout_width="40dp"
            android:layout_height="40dp"
            android:layout_marginStart="4dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/settings_title"
            android:src="@drawable/ic_settings"
            app:tint="?attr/colorOnSurfaceVariant" />

    </LinearLayout>

    <!-- Divider -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="?attr/colorOutlineVariant" />

    <!-- Core Buttons: 2x2 Grid -->
    <GridLayout
        android:id="@+id/grid_buttons"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:columnCount="2"
        android:rowCount="2"
        android:padding="12dp"
        android:useDefaultMargins="true">

        <!-- Row 0, Col 0: Yes -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_yes"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="72dp"
            android:layout_columnWeight="1"
            android:layout_margin="4dp"
            android:layout_rowWeight="1"
            android:gravity="center"
            android:maxLines="2"
            android:text="@string/btn_yes"
            android:textAppearance="?attr/textAppearanceLabelLarge"
            android:textColor="?attr/colorOnSurface"
            app:cornerRadius="12dp"
            app:strokeColor="?attr/colorOutline"
            app:strokeWidth="1dp" />

        <!-- Row 0, Col 1: Yes to All -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_yes_to_all"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="72dp"
            android:layout_columnWeight="1"
            android:layout_margin="4dp"
            android:layout_rowWeight="1"
            android:gravity="center"
            android:maxLines="2"
            android:text="@string/btn_yes_to_all"
            android:textAppearance="?attr/textAppearanceLabelLarge"
            android:textColor="?attr/colorOnSurface"
            app:cornerRadius="12dp"
            app:strokeColor="?attr/colorOutline"
            app:strokeWidth="1dp" />

        <!-- Row 1, Col 0: No -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_no"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="72dp"
            android:layout_columnWeight="1"
            android:layout_margin="4dp"
            android:layout_rowWeight="1"
            android:gravity="center"
            android:maxLines="2"
            android:text="@string/btn_no"
            android:textAppearance="?attr/textAppearanceLabelLarge"
            android:textColor="?attr/colorOnSurface"
            app:cornerRadius="12dp"
            app:strokeColor="?attr/colorOutline"
            app:strokeWidth="1dp" />

        <!-- Row 1, Col 1: Ctrl+C -->
        <com.google.android.material.button.MaterialButton
            android:id="@+id/btn_ctrl_c"
            style="@style/Widget.Material3.Button.OutlinedButton"
            android:layout_width="0dp"
            android:layout_height="72dp"
            android:layout_columnWeight="1"
            android:layout_margin="4dp"
            android:layout_rowWeight="1"
            android:gravity="center"
            android:maxLines="2"
            android:text="@string/btn_ctrl_c"
            android:textAppearance="?attr/textAppearanceLabelLarge"
            android:textColor="?attr/colorPrimary"
            app:cornerRadius="12dp"
            app:strokeColor="?attr/colorPrimary"
            app:strokeWidth="1dp" />

    </GridLayout>

    <!-- Divider -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginHorizontal="12dp"
        android:background="?attr/colorOutlineVariant" />

    <!-- Text Input -->
    <com.google.android.material.textfield.TextInputLayout
        android:id="@+id/til_input"
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginHorizontal="12dp"
        android:layout_marginTop="12dp"
        android:layout_marginBottom="8dp"
        android:hint="@string/hint_input_text"
        app:endIconDrawable="@android:drawable/ic_menu_send"
        app:endIconMode="custom"
        app:endIconTint="?attr/colorPrimary"
        app:boxStrokeColor="@color/outline">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_input_text"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:digits="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !@#$%^&amp;*()-=[]\;',./`~{}|:&quot;&lt;&gt;?_"
            android:imeOptions="actionSend"
            android:inputType="text"
            android:maxLines="1"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface"
            android:textColorHint="?attr/colorOnSurfaceVariant" />

    </com.google.android.material.textfield.TextInputLayout>

    <!-- Macro list header -->
    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="16dp"
        android:layout_marginBottom="4dp"
        android:text="@string/macro_list_title"
        android:textAppearance="?attr/textAppearanceTitleSmall"
        android:textColor="?attr/colorOnSurfaceVariant" />

    <!-- Macros RecyclerView -->
    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_macros"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:clipToPadding="false"
        android:padding="4dp" />

</LinearLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml
git commit -m "feat: migrate activity_main layout to M3 outlined style"
```

---

### Task 4: Update Macro Card and Dialog Layouts

**Files:**
- Modify: `app/src/main/res/layout/item_macro_button.xml`
- Modify: `app/src/main/res/layout/dialog_macro_edit.xml`

- [ ] **Step 1: Rewrite `item_macro_button.xml` — remove CardView, use OutlinedButton**

```xml
<?xml version="1.0" encoding="utf-8"?>
<com.google.android.material.button.MaterialButton xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/btn_macro"
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:layout_margin="4dp"
    android:gravity="center"
    android:maxLines="2"
    android:padding="8dp"
    android:textAllCaps="false"
    android:textAppearance="?attr/textAppearanceBodyMedium"
    android:textColor="?attr/colorOnSurface"
    app:cornerRadius="12dp"
    app:strokeColor="?attr/colorOutline"
    app:strokeWidth="1dp" />
```

- [ ] **Step 2: Rewrite `dialog_macro_edit.xml` — M3 OutlinedBox**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="16dp"
        android:hint="@string/dialog_macro_label"
        app:boxStrokeColor="@color/outline">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_macro_label"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:inputType="text"
            android:maxLines="1"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface"
            android:textColorHint="?attr/colorOnSurfaceVariant" />

    </com.google.android.material.textfield.TextInputLayout>

    <com.google.android.material.textfield.TextInputLayout
        style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:hint="@string/dialog_macro_command"
        app:boxStrokeColor="@color/outline">

        <com.google.android.material.textfield.TextInputEditText
            android:id="@+id/et_macro_command"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="top|start"
            android:inputType="textMultiLine"
            android:maxLines="5"
            android:minLines="3"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface"
            android:textColorHint="?attr/colorOnSurfaceVariant" />

    </com.google.android.material.textfield.TextInputLayout>

</LinearLayout>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/item_macro_button.xml app/src/main/res/layout/dialog_macro_edit.xml
git commit -m "feat: migrate macro card and dialog layouts to M3 outlined style"
```

---

### Task 5: Update Settings Icon and MainActivity

**Files:**
- Modify: `app/src/main/res/drawable/ic_settings.xml`
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

- [ ] **Step 1: Update `ic_settings.xml` — use theme-aware color**

Replace `fillColor="#000000"` with `fillColor="@color/on_surface_variant"`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <!-- Material Design settings gear icon -->
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M19.14,12.94c0.04,-0.31 0.06,-0.63 0.06,-0.94c0,-0.31 -0.02,-0.63 -0.06,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z" />
</vector>
```

- [ ] **Step 2: Update `MainActivity.kt` — Chip color logic**

Replace the `updateStatusUI` method (lines 199-220) with:

```kotlin
    private fun updateStatusUI(isConnected: Boolean, deviceName: String?) {
        if (isConnected) {
            statusChip.text = getString(R.string.status_connected, deviceName ?: getString(R.string.status_unknown_device))
            statusChip.setChipBackgroundColorResource(android.R.color.transparent)
            statusChip.setTextColor(getColor(R.color.status_connected))
            statusChip.setChipStrokeColorResource(R.color.primary)
            statusChip.chipIconTint = getColorStateList(R.color.primary)
            btnDisconnect.text = getString(R.string.settings_disconnect)
            btnDisconnect.visibility = View.VISIBLE
            enableAllButtons()
        } else {
            val lastName = hidService?.getLastConnectedDeviceName()
            if (lastName != null) {
                statusChip.text = getString(R.string.status_last_device, lastName)
            } else {
                statusChip.text = getString(R.string.status_waiting)
            }
            statusChip.setChipBackgroundColorResource(android.R.color.transparent)
            statusChip.setTextColor(getColor(R.color.status_waiting))
            statusChip.setChipStrokeColorResource(R.color.outline)
            statusChip.chipIconTint = getColorStateList(R.color.on_surface_variant)
            btnDisconnect.text = getString(R.string.btn_connect)
            btnDisconnect.visibility = if (hidService?.hasLastConnectedDevice() == true) View.VISIBLE else View.GONE
            disableAllButtons()
        }
    }
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_settings.xml app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt
git commit -m "feat: update settings icon and chip colors for M3 dark mode"
```

---

### Task 6: Build and Verify

- [ ] **Step 1: Clean build to remove stale artifacts**

```bash
./gradlew clean
```

- [ ] **Step 2: Build debug APK**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Verify no compilation errors related to missing color/style resources**

If build fails, check for missing color resources or incorrect style references and fix.

- [ ] **Step 4: Commit any fixes if needed**

```bash
git add -A
git commit -m "fix: resolve build issues from M3 migration"
```
