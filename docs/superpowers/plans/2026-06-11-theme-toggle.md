# 主题切换功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在设置对话框中添加白天/黑夜模式切换功能，支持跟随系统、白天模式、黑夜模式三种选项。

**Architecture:** 使用 AppCompatDelegate.setDefaultNightMode() 实现主题切换，SharedPreferences 持久化用户选择，自定义设置对话框布局提供主题选择 UI。

**Tech Stack:** Android, Kotlin, Material3, AppCompat, SharedPreferences

---

## 文件结构

- Modify: `app/src/main/res/values/strings.xml` - 添加主题相关字符串资源
- Modify: `app/src/main/java/com/haoze/claudekeyboard/util/Constants.kt` - 添加 SharedPreferences key
- Create: `app/src/main/res/layout/dialog_settings.xml` - 设置对话框自定义布局
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt` - 实现主题切换逻辑

---

### Task 1: 添加字符串资源

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: 添加主题相关字符串**

在 `strings.xml` 的 Settings 区域添加以下字符串：

```xml
<!-- Settings -->
<string name="settings_title">设置</string>
<string name="settings_reset_macros">重置宏为默认值</string>
<string name="settings_theme_mode">主题模式</string>
<string name="theme_follow_system">跟随系统</string>
<string name="theme_light">白天模式</string>
<string name="theme_dark">黑夜模式</string>
```

- [ ] **Step 2: 验证字符串资源**

运行: `./gradlew assembleDebug`
Expected: 构建成功，无资源错误

- [ ] **Step 3: 提交**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add theme mode string resources"
```

---

### Task 2: 添加 SharedPreferences Key

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/util/Constants.kt`

- [ ] **Step 1: 添加主题模式常量**

在 `Constants.kt` 的 SharedPreferences keys 区域添加：

```kotlin
// SharedPreferences keys
const val PREFS_NAME_MACRO = "macro_prefs"
const val KEY_MACROS = "macros"
const val PREFS_NAME_SETTINGS = "settings_prefs"
const val KEY_THEME_MODE = "theme_mode"

// Theme mode values
const val THEME_FOLLOW_SYSTEM = 0
const val THEME_LIGHT = 1
const val THEME_DARK = 2
```

- [ ] **Step 2: 验证编译**

运行: `./gradlew assembleDebug`
Expected: 构建成功

- [ ] **Step 3: 提交**

```bash
git add app/src/main/java/com/haoze/claudekeyboard/util/Constants.kt
git commit -m "feat: add theme mode constants"
```

---

### Task 3: 创建设置对话框布局

**Files:**
- Create: `app/src/main/res/layout/dialog_settings.xml`

- [ ] **Step 1: 创建设置对话框布局**

创建 `dialog_settings.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="24dp">

    <!-- 主题模式选项 -->
    <LinearLayout
        android:id="@layout/ll_theme_mode"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:gravity="center_vertical"
        android:padding="12dp"
        android:background="?attr/selectableItemBackground">

        <TextView
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:text="@string/settings_theme_mode"
            android:textSize="16sp" />

        <TextView
            android:id="@+id/tv_theme_value"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/theme_follow_system"
            android:textSize="14sp"
            android:textColor="?android:attr/textColorSecondary" />

    </LinearLayout>

    <!-- 重置宏选项 -->
    <TextView
        android:id="@+id/tv_reset_macros"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="@string/settings_reset_macros"
        android:textSize="16sp"
        android:padding="12dp"
        android:background="?attr/selectableItemBackground" />

</LinearLayout>
```

- [ ] **Step 2: 修复布局 ID 错误**

将 `@layout/ll_theme_mode` 改为 `@+id/ll_theme_mode`：

```xml
<LinearLayout
    android:id="@+id/ll_theme_mode"
    ...
```

- [ ] **Step 3: 验证布局**

运行: `./gradlew assembleDebug`
Expected: 构建成功

- [ ] **Step 4: 提交**

```bash
git add app/src/main/res/layout/dialog_settings.xml
git commit -m "feat: add settings dialog layout"
```

---

### Task 4: 实现主题切换逻辑

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

- [ ] **Step 1: 添加主题初始化方法**

在 `MainActivity.kt` 的 `onCreate()` 方法开头添加主题初始化：

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    applyTheme()
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)
    // ... 其他代码
}
```

在类中添加 `applyTheme()` 方法：

```kotlin
private fun applyTheme() {
    val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
    val themeMode = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)
    AppCompatDelegate.setDefaultNightMode(when (themeMode) {
        Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    })
}
```

- [ ] **Step 2: 添加获取主题名称的辅助方法**

```kotlin
private fun getThemeModeName(mode: Int): String {
    return when (mode) {
        Constants.THEME_LIGHT -> getString(R.string.theme_light)
        Constants.THEME_DARK -> getString(R.string.theme_dark)
        else -> getString(R.string.theme_follow_system)
    }
}
```

- [ ] **Step 3: 修改 showSettingsDialog 方法**

替换现有的 `showSettingsDialog()` 方法：

```kotlin
private fun showSettingsDialog() {
    val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
    val tvThemeValue = dialogView.findViewById<TextView>(R.id.tv_theme_value)
    val llThemeMode = dialogView.findViewById<View>(R.id.ll_theme_mode)
    val tvResetMacros = dialogView.findViewById<TextView>(R.id.tv_reset_macros)

    val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
    val currentTheme = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)
    tvThemeValue.text = getThemeModeName(currentTheme)

    llThemeMode.setOnClickListener {
        showThemeSelectionDialog(tvThemeValue)
    }

    tvResetMacros.setOnClickListener {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.settings_reset_macros)
            .setMessage(R.string.dialog_reset_macros_confirm)
            .setPositiveButton(R.string.dialog_reset) { _, _ ->
                macroRepository.resetToDefaults()
                loadMacros()
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.settings_title)
        .setView(dialogView)
        .setNegativeButton(R.string.dialog_cancel, null)
        .show()
}
```

- [ ] **Step 4: 添加主题选择对话框方法**

```kotlin
private fun showThemeSelectionDialog(tvThemeValue: TextView) {
    val themes = arrayOf(
        getString(R.string.theme_follow_system),
        getString(R.string.theme_light),
        getString(R.string.theme_dark)
    )

    val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
    val currentTheme = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)

    MaterialAlertDialogBuilder(this)
        .setTitle(R.string.settings_theme_mode)
        .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
            prefs.edit().putInt(Constants.KEY_THEME_MODE, which).apply()
            tvThemeValue.text = themes[which]
            AppCompatDelegate.setDefaultNightMode(when (which) {
                Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            })
            dialog.dismiss()
        }
        .setNegativeButton(R.string.dialog_cancel, null)
        .show()
}
```

- [ ] **Step 5: 添加必要的 import**

在文件顶部添加：

```kotlin
import androidx.appcompat.app.AppCompatDelegate
```

- [ ] **Step 6: 验证功能**

运行: `./gradlew assembleDebug`
Expected: 构建成功

- [ ] **Step 7: 提交**

```bash
git add app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt
git commit -m "feat: implement theme toggle in settings dialog"
```

---

### Task 5: 最终验证

**Files:**
- None (testing only)

- [ ] **Step 1: 完整构建验证**

运行: `./gradlew clean assembleDebug`
Expected: 构建成功，无错误

- [ ] **Step 2: 功能验证清单**

手动验证以下功能：
- [ ] 打开设置对话框，显示主题模式选项
- [ ] 点击主题模式，弹出选择对话框
- [ ] 选择"跟随系统"，界面跟随系统主题
- [ ] 选择"白天模式"，界面切换为浅色主题
- [ ] 选择"黑夜模式"，界面切换为深色主题
- [ ] 重启 app，主题设置保持不变
- [ ] 重置宏功能仍然正常工作

- [ ] **Step 3: 最终提交**

```bash
git add -A
git commit -m "feat: complete theme toggle feature"
```
