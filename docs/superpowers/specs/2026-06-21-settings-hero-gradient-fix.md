# Settings Floating Title Card - Replace Blur with Hero Gradient

**Date:** 2026-06-21
**Status:** Approved

## Overview

取消悬浮标题卡片的毛玻璃模糊效果，改为与主页 Hero 卡片相同的蓝紫渐变背景。

## Changes

### 1. 布局 (`content_settings.xml`)
- 卡片 `android:background` 从 `@drawable/bg_settings_floating_card` 改为 `@drawable/bg_home_hero`

### 2. MainActivity.kt
- 移除 `RenderEffect` 模糊相关代码块（整个 `if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)` 块）
- 移除 `setBackgroundColor(0x88F0F0F5)` 调用

### 3. 删除文件
- `app/src/main/res/drawable/bg_settings_floating_card.xml`（不再使用）

### 保留不变
- 滚动阴影增强（4dp→12dp）
- 返回按钮（白色图标，嵌入卡片内）
- RecyclerView paddingTop=92dp
- 悬浮卡片整体结构

## Files Changed

| File | Change |
|------|--------|
| `content_settings.xml` | 更换 background drawable |
| `MainActivity.kt` | 移除 RenderEffect 和 setBackgroundColor |
| `bg_settings_floating_card.xml` | 删除