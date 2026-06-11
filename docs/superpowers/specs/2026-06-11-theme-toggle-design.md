# 主题切换功能设计

## 概述

在设置对话框中添加白天/黑夜模式切换功能，支持三种模式：跟随系统（默认）、白天模式、黑夜模式。

## 设计方案

### 设置对话框

使用自定义布局改造现有设置对话框，包含两个功能区域：

- **主题模式**：显示当前选中的主题，点击弹出单选对话框
- **重置宏**：保留现有的重置宏功能

主题选择使用 `MaterialAlertDialogBuilder.setSingleChoiceItems()`，提供三个选项：
- 跟随系统（默认）
- 白天模式
- 黑夜模式

### 主题持久化与应用

**存储**：使用 SharedPreferences 保存用户选择
- key: `theme_mode`
- 值: `0`（跟随系统）、`1`（白天）、`2`（黑夜）

**应用时机**：
- `MainActivity.onCreate()` 中读取设置并应用
- 用户在对话框中切换时立即应用

**实现方式**：调用 `AppCompatDelegate.setDefaultNightMode()`
- 跟随系统 → `MODE_NIGHT_FOLLOW_SYSTEM`
- 白天 → `MODE_NIGHT_NO`
- 黑夜 → `MODE_NIGHT_YES`

**Activity 重建**：`setDefaultNightMode()` 会自动触发 Activity 重建，无需手动处理。

### 字符串资源

新增以下字符串：
- `settings_theme_mode`：主题模式
- `theme_follow_system`：跟随系统
- `theme_light`：白天模式
- `theme_dark`：黑夜模式

### UI 细节

- 设置对话框使用 LinearLayout 垂直排列
- 主题模式行：左侧文字 + 右侧当前值（如"跟随系统"）
- 重置宏行：保留原有样式
- 主题切换无延迟，选中后立即生效

## 技术要点

1. 项目已使用 Material3 DayNight 主题（`Theme.Material3.DayNight.NoActionBar`）
2. 已有 `values/themes.xml` 和 `values-night/themes.xml`，颜色资源已区分深浅模式
3. 使用 `AppCompatDelegate.setDefaultNightMode()` 是 Android 标准做法，兼容性好
