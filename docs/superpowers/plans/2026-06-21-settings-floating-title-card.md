# Settings Floating Title Card Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将设置页标题卡片从 RecyclerView 中移出为独立悬浮元素，嵌入返回按钮，毛玻璃效果，滚动增强阴影。

**Architecture:** FrameLayout 内 RecyclerView 在上层，悬浮 ConstraintLayout 卡片在下层（后绘制覆盖在上方）。卡片包含返回按钮和标题文字。RecyclerView 添加 paddingTop 避免内容被遮挡。

**Tech Stack:** Android XML layouts, Kotlin, Material Components, RenderEffect (API 31+)

## Global Constraints

- 最低 API 28，目标 API 36
- 不引入新依赖
- 保持现有 FrameLayout 根布局不变
- 保持现有导航逻辑（navigateToHome()）

---

## File Structure

| File | Role |
|------|------|
| `app/src/main/res/layout/content_settings.xml` | 修改：添加悬浮卡片，更新 RecyclerView padding |
| `app/src/main/res/drawable/bg_settings_floating_card.xml` | 新建：悬浮卡片半透明背景 |
| `app/src/main/java/com/.../MainActivity.kt` | 修改：移除 TitleHeader，添加滚动监听和模糊效果 |

---

### Task 1: Create floating card background drawable

**Files:**
- Create: `app/src/main/res/drawable/bg_settings_floating_card.xml`

**Interfaces:**
- Produces: `@drawable/bg_settings_floating_card` — 圆角 22dp 半透明矩形，供悬浮卡片使用

- [ ] **Step 1: Create the drawable**

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android">
    <solid android:color="#DDF0F0F5" />
    <corners android:radius="22dp" />
</shape>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/drawable/bg_settings_floating_card.xml
git commit -m "feat: add floating card background drawable for settings"
```

---

### Task 2: Update content_settings.xml layout

**Files:**
- Modify: `app/src/main/res/layout/content_settings.xml`

**Interfaces:**
- Consumes: `@drawable/bg_settings_floating_card` (Task 1), `@drawable/bg_settings_back_button` (已存在)
- Produces: `R.id.card_settings_title` (ConstraintLayout), `R.id.btn_back_settings` (ImageButton), `R.id.tv_settings_title` (TextView), `R.id.rv_settings` (RecyclerView with paddingTop)

- [ ] **Step 1: Replace layout content**

Replace the entire content of `content_settings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_home_screen">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_settings"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipToPadding="false"
        android:paddingTop="92dp"
        android:paddingBottom="12dp" />

    <androidx.constraintlayout.widget.ConstraintLayout
        android:id="@+id/card_settings_title"
        android:layout_width="match_parent"
        android:layout_height="72dp"
        android:layout_marginHorizontal="20dp"
        android:layout_marginTop="20dp"
        android:background="@drawable/bg_settings_floating_card"
        android:elevation="4dp"
        android:paddingHorizontal="20dp"
        android:paddingVertical="12dp">

        <ImageButton
            android:id="@+id/btn_back_settings"
            android:layout_width="36dp"
            android:layout_height="36dp"
            android:background="@drawable/bg_settings_back_button"
            android:contentDescription="@string/nav_back"
            android:scaleType="centerInside"
            android:src="@drawable/baseline_arrow_back_24"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:tint="@color/home_on_hero" />

        <TextView
            android:id="@+id/tv_settings_title"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="48dp"
            android:text="@string/home_settings_title"
            android:textAppearance="?attr/textAppearanceTitleMedium"
            android:textColor="@color/home_on_hero"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>

</FrameLayout>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/layout/content_settings.xml
git commit -m "feat: add floating title card with back button to settings"
```

---

### Task 3: Update MainActivity.kt — remove TitleHeader, add scroll listener, add blur

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt:409-494`

**Interfaces:**
- Consumes: `R.id.card_settings_title`, `R.id.btn_back_settings`, `R.id.tv_settings_title`, `R.id.rv_settings` (Task 2)
- Produces: `setupSettingsPage()` — 无 TitleHeader 数据，有滚动阴影和模糊效果

- [ ] **Step 1: Add necessary imports at top of file**

Add after existing imports (line 40):

```kotlin
import android.animation.ObjectAnimator
import android.graphics.RenderEffect
import android.graphics.Shader
```

- [ ] **Step 2: Replace setupSettingsPage() method**

Replace the entire method (lines 409-494):

```kotlin
private fun setupSettingsPage() {
    val btnBack = findViewById<ImageButton>(R.id.btn_back_settings)
    val cardTitle = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.card_settings_title)
    btnBack.setOnClickListener {
        it.performKeyClick()
        navigateToHome()
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        cardTitle.setRenderEffect(
            RenderEffect.createBlurEffect(25f, 25f, Shader.TileMode.CLAMP)
        )
    }

    val rvSettings = findViewById<RecyclerView>(R.id.rv_settings)
    rvSettings.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val scrollOffset = recyclerView.computeVerticalScrollOffset()
            val maxScroll = (40 * resources.displayMetrics.density).toInt()
            val fraction = (scrollOffset.toFloat() / maxScroll).coerceIn(0f, 1f)
            val targetElevation = 4f + fraction * 8f
            ObjectAnimator.ofFloat(cardTitle, "elevation", cardTitle.elevation, targetElevation).apply {
                duration = 50
                start()
            }
        }
    })

    val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    val adapter = SettingsAdapter(
        prefs = prefs,
        onToggleGroupChanged = { key, index ->
            if (key == "theme_mode_index") {
                AppCompatDelegate.setDefaultNightMode(
                    when (index) {
                        1 -> AppCompatDelegate.MODE_NIGHT_NO
                        2 -> AppCompatDelegate.MODE_NIGHT_YES
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }
                )
                recreate()
            }
        },
        onButtonClick = { item ->
            when (item.title) {
                getString(R.string.settings_reset_macros) -> {
                    val dlg = MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.settings_reset_macros)
                        .setMessage(R.string.dialog_reset_macros_confirm)
                        .setPositiveButton(R.string.dialog_reset) { _, _ ->
                            macroRepository.resetToDefaults()
                            loadMacros()
                        }
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .create()
                    dlg.fixM3Background()
                    dlg.show()
                }
            }
        },
        onSwitchChanged = { key, value ->
            if (key == "connection_notifications" && !value) {
                bluetoothViewModel.dismissConnectionNotification()
            }
        }
    )

    rvSettings.apply {
        layoutManager = LinearLayoutManager(this@MainActivity)
        this.adapter = adapter
    }

    val versionName = try {
        val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
        pInfo.versionName ?: "1.0.0"
    } catch (e: Exception) {
        "1.0.0"
    }

    val items = listOf(
        SettingsItem.SectionHeader(getString(R.string.settings_section_connection)),
        SettingsItem.SwitchItem("auto_connect_on_launch", getString(R.string.settings_auto_connect_launch), null, true),
        SettingsItem.SwitchItem("auto_reconnect_on_disconnect", getString(R.string.settings_auto_reconnect), null, true),
        SettingsItem.SwitchItem("keep_screen_on", getString(R.string.settings_keep_screen_on), getString(R.string.settings_keep_screen_on_subtitle), true),
        SettingsItem.SwitchItem("connection_notifications", getString(R.string.settings_connection_notifications), getString(R.string.settings_connection_notifications_subtitle), true),
        SettingsItem.SectionHeader(getString(R.string.settings_section_touchpad)),
        SettingsItem.SliderItem("touchpad_sensitivity", getString(R.string.settings_touchpad_sensitivity), null, 5, 1f, 10f, 1f),
        SettingsItem.SliderItem("cursor_speed", getString(R.string.settings_cursor_speed), null, 5, 1f, 10f, 1f),
        SettingsItem.SwitchItem("scroll_direction_natural", getString(R.string.settings_scroll_direction_natural), null, false),
        SettingsItem.SectionHeader(getString(R.string.settings_section_interaction)),
        SettingsItem.SwitchItem("haptic_feedback", getString(R.string.settings_haptic_feedback), getString(R.string.settings_haptic_feedback_subtitle), true),
        SettingsItem.SectionHeader(getString(R.string.settings_section_data)),
        SettingsItem.ButtonItem(getString(R.string.settings_reset_macros)),
        SettingsItem.SectionHeader(getString(R.string.settings_section_appearance)),
        SettingsItem.ToggleGroupItem("theme_mode_index", getString(R.string.settings_theme_mode), listOf(getString(R.string.settings_theme_system), getString(R.string.settings_theme_light), getString(R.string.settings_theme_dark)), 0),
        SettingsItem.SectionHeader(getString(R.string.settings_section_about)),
        SettingsItem.InfoItem(getString(R.string.settings_app_name_label), getString(R.string.app_name)),
        SettingsItem.InfoItem(getString(R.string.settings_version), versionName),
        SettingsItem.InfoItem(getString(R.string.settings_open_source), "", onClick = {
            // Placeholder for open source license
        })
    )
    adapter.submitList(items)
}
```

- [ ] **Step 3: Build and verify**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt
git commit -m "feat: add floating title card with blur, scroll shadow, remove TitleHeader"
```