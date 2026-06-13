# Settings-Style Home Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the bottom navigation bar with a settings-style list home page that navigates to Keyboard, Touchpad, Claude Helper, Device Connection, and Settings.

**Architecture:** Add a `content_home` inline layout with a RecyclerView to `activity_main.xml`. Remove `BottomNavigationView`. Each home item navigates to its feature page via visibility toggling (existing pattern). Settings moves from a dialog to an inline `content_settings` layout. Device connection is extracted from the Claude page to the home page.

**Tech Stack:** Kotlin, Android XML layouts, RecyclerView, Material Design 3, SharedPreferences

---

## File Structure

### New Files
| File | Responsibility |
|---|---|
| `app/src/main/java/com/haoze/claudekeyboard/ui/home/HomeItem.kt` | Data class for home list items |
| `app/src/main/java/com/haoze/claudekeyboard/ui/home/HomeAdapter.kt` | RecyclerView adapter with section headers and clickable items |
| `app/src/main/res/layout/item_home_section.xml` | Layout for section header |
| `app/src/main/res/layout/item_home_entry.xml` | Layout for clickable entry (icon + title + subtitle + arrow) |
| `app/src/main/res/layout/content_settings.xml` | Settings page layout (migrated from dialog) |
| `app/src/main/res/drawable/ic_home_keyboard.xml` | Keyboard icon for home list |
| `app/src/main/res/drawable/ic_home_touchpad.xml` | Touchpad icon for home list |
| `app/src/main/res/drawable/ic_home_claude.xml` | Claude icon for home list |
| `app/src/main/res/drawable/ic_home_device.xml` | Device/bluetooth icon for home list |
| `app/src/main/res/drawable/ic_home_settings.xml` | Settings gear icon for home list |
| `app/src/main/res/drawable/ic_arrow_right.xml` | Right arrow chevron for list items |
| `app/src/main/res/drawable/ic_back.xml` | Back arrow for toolbar |

### Modified Files
| File | Changes |
|---|---|
| `app/src/main/res/layout/activity_main.xml` | Remove BottomNavigationView + divider; add content_home and content_settings |
| `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt` | Remove bottom nav logic; add home page init, navigation, settings page, back handling |
| `app/src/main/res/layout/content_claude.xml` (inline in activity_main.xml) | Remove device_bar (device name + connect button) |
| `app/src/main/java/com/haoze/claudekeyboard/ui/keyboard/KeyboardFragment.kt` | Change `switchToClaudeTab()` → `switchToHome()` |
| `app/src/main/res/layout/fragment_touchpad.xml` | Change back button text from "Claude" to home icon |
| `app/src/main/java/com/haoze/claudekeyboard/ui/touchpad/TouchpadFragment.kt` | Change `switchToClaudeTab()` → `switchToHome()` |
| `app/src/main/res/values/strings.xml` | Add home page string resources |
| `app/src/main/res/menu/bottom_nav_menu.xml` | Delete (no longer needed) |

---

### Task 1: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add new strings**

Add these strings before the closing `</resources>` tag:

```xml
    <!-- Home page -->
    <string name="home_functions">功能</string>
    <string name="home_system">系统</string>
    <string name="home_keyboard_title">键盘</string>
    <string name="home_keyboard_subtitle">蓝牙 HID 键盘输入</string>
    <string name="home_touchpad_title">触控板</string>
    <string name="home_touchpad_subtitle">蓝牙 HID 鼠标控制</string>
    <string name="home_claude_title">Claude 辅助</string>
    <string name="home_claude_subtitle">快捷命令与文本输入</string>
    <string name="home_device_title">设备连接</string>
    <string name="home_device_subtitle">管理蓝牙设备</string>
    <string name="home_settings_title">设置</string>
    <string name="home_settings_subtitle">主题与数据管理</string>
    <string name="nav_back">返回</string>
    <string name="settings_theme_mode_label">主题模式</string>
    <string name="settings_theme_current">当前: %s</string>
```

- [ ] **Step 2: Verify strings.xml is valid**

Run: `grep -c "<string" app/src/main/res/values/strings.xml`
Expected: count increases by ~14

---

### Task 2: Create Drawable Icons

**Files:**
- Create: `app/src/main/res/drawable/ic_home_keyboard.xml`
- Create: `app/src/main/res/drawable/ic_home_touchpad.xml`
- Create: `app/src/main/res/drawable/ic_home_claude.xml`
- Create: `app/src/main/res/drawable/ic_home_device.xml`
- Create: `app/src/main/res/drawable/ic_home_settings.xml`
- Create: `app/src/main/res/drawable/ic_arrow_right.xml`
- Create: `app/src/main/res/drawable/ic_back.xml`

- [ ] **Step 1: Create ic_home_keyboard.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M20,5H4C2.9,5 2,5.9 2,7v10c0,1.1 0.9,2 2,2h16c1.1,0 2,-0.9 2,-2V7C22,5.9 21.1,5 20,5zM20,17H4V7h16V17zM11,8h2v2h-2V8zM7,8h2v2H7V8zM15,12h2v2h-2V12zM11,12h2v2h-2V12zM7,12h2v2H7V12zM15,8h2v2h-2V8zM4,15h16v2H4V15z" />
</vector>
```

- [ ] **Step 2: Create ic_home_touchpad.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M13,1.07V9h7c0,-4.08 -3.05,-7.44 -7,-7.93zM4,15c0,4.42 3.58,8 8,8s8,-3.58 8,-8v-4H4V15zM13,1.07V9h7c0,-4.08 -3.05,-7.44 -7,-7.93zM4,15c0,4.42 3.58,8 8,8s8,-3.58 8,-8v-4H4V15z" />
</vector>
```

- [ ] **Step 3: Create ic_home_claude.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M21,11.5a8.38,8.38 0,0 1,-0.9 3.8 8.5,8.5 0,0 1,-7.6 4.7 8.38,8.38 0,0 1,-3.8 -0.9L3,21l1.9,-5.7a8.38,8.38 0,0 1,-0.9 -3.8 8.5,8.5 0,0 1,4.7 -7.6 8.38,8.38 0,0 1,3.8 -0.9h0.5a8.48,8.48 0,0 1,8 8v0.5z" />
</vector>
```

- [ ] **Step 4: Create ic_home_device.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M17.71,7.71L12,2h-1v7.59L6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 11,14.41V22h1l5.71,-5.71 -4.3,-4.29 4.3,-4.29zM13,5.83l1.88,1.88L13,9.59V5.83zM14.88,13.12L13,15l1.88,1.88L13,18.76 14.88,13.12z" />
</vector>
```

- [ ] **Step 5: Create ic_home_settings.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M19.14,12.94c0.04,-0.31 0.06,-0.63 0.06,-0.94c0,-0.31 -0.02,-0.63 -0.06,-0.94l2.03,-1.58c0.18,-0.14 0.23,-0.41 0.12,-0.61l-1.92,-3.32c-0.12,-0.22 -0.37,-0.29 -0.59,-0.22l-2.39,0.96c-0.5,-0.38 -1.03,-0.7 -1.62,-0.94L14.4,2.81c-0.04,-0.24 -0.24,-0.41 -0.48,-0.41h-3.84c-0.24,0 -0.43,0.17 -0.47,0.41L9.25,5.35C8.66,5.59 8.12,5.92 7.63,6.29L5.24,5.33c-0.22,-0.08 -0.47,0 -0.59,0.22L2.74,8.87C2.62,9.08 2.66,9.34 2.86,9.48l2.03,1.58C4.84,11.36 4.8,11.69 4.8,12s0.02,0.64 0.07,0.94l-2.03,1.58c-0.18,0.14 -0.23,0.41 -0.12,0.61l1.92,3.32c0.12,0.22 0.37,0.29 0.59,0.22l2.39,-0.96c0.5,0.38 1.03,0.7 1.62,0.94l0.36,2.54c0.05,0.24 0.24,0.41 0.48,0.41h3.84c0.24,0 0.44,-0.17 0.47,-0.41l0.36,-2.54c0.59,-0.24 1.13,-0.56 1.62,-0.94l2.39,0.96c0.22,0.08 0.47,0 0.59,-0.22l1.92,-3.32c0.12,-0.22 0.07,-0.47 -0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0 -3.6,-1.62 -3.6,-3.6s1.62,-3.6 3.6,-3.6s3.6,1.62 3.6,3.6S13.98,15.6 12,15.6z" />
</vector>
```

- [ ] **Step 6: Create ic_arrow_right.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M10,6L8.59,7.41 13.17,12l-4.58,4.59L10,18l6,-6z" />
</vector>
```

- [ ] **Step 7: Create ic_back.xml**

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="24"
    android:viewportHeight="24">
    <path
        android:fillColor="@color/on_surface_variant"
        android:pathData="M20,11H7.83l5.59,-5.59L12,4l-8,8 8,8 1.41,-1.41L7.83,13H20v-2z" />
</vector>
```

- [ ] **Step 8: Verify all drawable files exist**

Run: `ls app/src/main/res/drawable/ic_home_*.xml app/src/main/res/drawable/ic_arrow_right.xml app/src/main/res/drawable/ic_back.xml`
Expected: 7 files listed

---

### Task 3: Create Home Item Layouts

**Files:**
- Create: `app/src/main/res/layout/item_home_section.xml`
- Create: `app/src/main/res/layout/item_home_entry.xml`

- [ ] **Step 1: Create item_home_section.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/tv_section_title"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingHorizontal="16dp"
    android:paddingTop="24dp"
    android:paddingBottom="8dp"
    android:textAppearance="?attr/textAppearanceTitleSmall"
    android:textColor="?attr/colorPrimary" />
```

- [ ] **Step 2: Create item_home_entry.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:gravity="center_vertical"
    android:orientation="horizontal"
    android:paddingHorizontal="16dp"
    android:background="?attr/selectableItemBackground">

    <ImageView
        android:id="@+id/iv_icon"
        android:layout_width="32dp"
        android:layout_height="32dp"
        android:scaleType="centerInside"
        app:tint="?attr/colorOnSurfaceVariant" />

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:layout_marginStart="16dp"
        android:orientation="vertical">

        <TextView
            android:id="@+id/tv_title"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface" />

        <TextView
            android:id="@+id/tv_subtitle"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:textColor="?attr/colorOnSurfaceVariant" />

    </LinearLayout>

    <ImageView
        android:id="@+id/iv_arrow"
        android:layout_width="24dp"
        android:layout_height="24dp"
        android:src="@drawable/ic_arrow_right"
        app:tint="?attr/colorOnSurfaceVariant" />

</LinearLayout>
```

---

### Task 4: Create HomeItem Data Class

**Files:**
- Create: `app/src/main/java/com/haoze/claudekeyboard/ui/home/HomeItem.kt`

- [ ] **Step 1: Create HomeItem.kt**

```kotlin
package com.haoze.claudekeyboard.ui.home

/**
 * Data types for the settings-style home page list.
 */
sealed class HomeItem {
    /**
     * Section header (e.g., "功能", "系统").
     */
    data class Section(val title: String) : HomeItem()

    /**
     * Clickable entry (e.g., Keyboard, Settings).
     */
    data class Entry(
        val id: String,
        val title: String,
        val subtitle: String,
        val iconRes: Int
    ) : HomeItem()
}
```

---

### Task 5: Create HomeAdapter

**Files:**
- Create: `app/src/main/java/com/haoze/claudekeyboard/ui/home/HomeAdapter.kt`

- [ ] **Step 1: Create HomeAdapter.kt**

```kotlin
package com.haoze.claudekeyboard.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.haoze.claudekeyboard.R

/**
 * RecyclerView adapter for the settings-style home page.
 * Supports two view types: section headers and clickable entries.
 */
class HomeAdapter(
    private val onEntryClick: (HomeItem.Entry) -> Unit
) : ListAdapter<HomeItem, RecyclerView.ViewHolder>(HomeItemDiffCallback()) {

    companion object {
        private const val TYPE_SECTION = 0
        private const val TYPE_ENTRY = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is HomeItem.Section -> TYPE_SECTION
            is HomeItem.Entry -> TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_SECTION -> {
                val view = inflater.inflate(R.layout.item_home_section, parent, false)
                SectionViewHolder(view)
            }
            else -> {
                val view = inflater.inflate(R.layout.item_home_entry, parent, false)
                EntryViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is HomeItem.Section -> (holder as SectionViewHolder).bind(item)
            is HomeItem.Entry -> (holder as EntryViewHolder).bind(item)
        }
    }

    inner class SectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_section_title)

        fun bind(section: HomeItem.Section) {
            tvTitle.text = section.title
        }
    }

    inner class EntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
        private val tvSubtitle: TextView = itemView.findViewById(R.id.tv_subtitle)

        fun bind(entry: HomeItem.Entry) {
            ivIcon.setImageResource(entry.iconRes)
            tvTitle.text = entry.title
            tvSubtitle.text = entry.subtitle
            itemView.setOnClickListener { onEntryClick(entry) }
        }
    }

    /**
     * Update the subtitle of an entry by its id.
     */
    fun updateSubtitle(entryId: String, newSubtitle: String) {
        val currentList = currentList.toMutableList()
        val index = currentList.indexOfFirst { it is HomeItem.Entry && it.id == entryId }
        if (index >= 0) {
            val entry = currentList[index] as HomeItem.Entry
            currentList[index] = entry.copy(subtitle = newSubtitle)
            submitList(currentList)
        }
    }

    private class HomeItemDiffCallback : DiffUtil.ItemCallback<HomeItem>() {
        override fun areItemsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return when {
                oldItem is HomeItem.Section && newItem is HomeItem.Section ->
                    oldItem.title == newItem.title
                oldItem is HomeItem.Entry && newItem is HomeItem.Entry ->
                    oldItem.id == newItem.id
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: HomeItem, newItem: HomeItem): Boolean {
            return oldItem == newItem
        }
    }
}
```

---

### Task 6: Create Settings Page Layout

**Files:**
- Create: `app/src/main/res/layout/content_settings.xml`

- [ ] **Step 1: Create content_settings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="?attr/colorSurface">

    <!-- Toolbar -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:gravity="center_vertical"
        android:orientation="horizontal"
        android:paddingHorizontal="4dp">

        <ImageButton
            android:id="@+id/btn_back_settings"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/nav_back"
            android:src="@drawable/ic_back"
            android:scaleType="centerInside"
            app:tint="?attr/colorOnSurface" />

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginStart="4dp"
            android:text="@string/home_settings_title"
            android:textAppearance="?attr/textAppearanceTitleLarge"
            android:textColor="?attr/colorOnSurface" />

    </LinearLayout>

    <!-- Divider -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="?attr/colorOutlineVariant" />

    <!-- Theme mode -->
    <LinearLayout
        android:id="@+id/settings_theme_mode"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="16dp"
        android:paddingVertical="16dp"
        android:background="?attr/selectableItemBackground">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_theme_mode_label"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface" />

        <TextView
            android:id="@+id/tv_theme_current"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="2dp"
            android:textAppearance="?attr/textAppearanceBodySmall"
            android:textColor="?attr/colorOnSurfaceVariant" />

    </LinearLayout>

    <!-- Divider -->
    <View
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:layout_marginStart="16dp"
        android:background="?attr/colorOutlineVariant" />

    <!-- Reset macros -->
    <LinearLayout
        android:id="@+id/settings_reset_macros"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:paddingHorizontal="16dp"
        android:paddingVertical="16dp"
        android:background="?attr/selectableItemBackground">

        <TextView
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:text="@string/settings_reset_macros"
            android:textAppearance="?attr/textAppearanceBodyLarge"
            android:textColor="?attr/colorOnSurface" />

    </LinearLayout>

</LinearLayout>
```

---

### Task 7: Modify activity_main.xml - Add Home and Settings, Remove Bottom Nav

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`

- [ ] **Step 1: Read current activity_main.xml**

Read the file to confirm current state.

- [ ] **Step 2: Add content_home and content_settings inside content_container**

After the `content_touchpad` FrameLayout closing tag and before `</FrameLayout>` (content_container closing), add:

```xml
        <!-- Home Page Content -->
        <LinearLayout
            android:id="@+id/content_home"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:orientation="vertical"
            android:background="?attr/colorSurface">

            <!-- Toolbar -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="56dp"
                android:gravity="center_vertical"
                android:orientation="horizontal"
                android:paddingHorizontal="16dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/app_name"
                    android:textAppearance="?attr/textAppearanceHeadlineMedium"
                    android:textColor="?attr/colorOnSurface" />

            </LinearLayout>

            <!-- Divider -->
            <View
                android:layout_width="match_parent"
                android:layout_height="1dp"
                android:background="?attr/colorOutlineVariant" />

            <!-- Home list RecyclerView -->
            <androidx.recyclerview.widget.RecyclerView
                android:id="@+id/rv_home"
                android:layout_width="match_parent"
                android:layout_height="match_parent"
                android:clipToPadding="false"
                android:paddingBottom="16dp" />

        </LinearLayout>

        <!-- Settings Page Content -->
        <FrameLayout
            android:id="@+id/content_settings"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            android:visibility="gone">

            <include layout="@layout/content_settings" />

        </FrameLayout>
```

- [ ] **Step 3: Remove BottomNavigationView and divider**

Remove these two elements from the end of the file (before `</LinearLayout>`):

```xml
    <!-- Divider above bottom nav -->
    <View
        android:id="@+id/divider_above_nav"
        android:layout_width="match_parent"
        android:layout_height="1dp"
        android:background="?attr/colorOutlineVariant" />

    <!-- Bottom Navigation Bar -->
    <com.google.android.material.bottomnavigation.BottomNavigationView
        android:id="@+id/bottom_nav"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:visibility="invisible"
        android:background="?attr/colorSurface"
        app:labelVisibilityMode="labeled"
        app:menu="@menu/bottom_nav_menu"
        app:itemIconTint="@color/bottom_nav_color"
        app:itemTextColor="@color/bottom_nav_color" />
```

- [ ] **Step 4: Remove device_bar from content_claude**

Inside `content_claude`, remove the device bar section (lines 64-94 in the original):

```xml
                <!-- Row 2: Device Name + Connect/Disconnect -->
                <LinearLayout
                    android:id="@+id/device_bar"
                    ...>
                    ...
                </LinearLayout>
```

- [ ] **Step 5: Set content_claude visibility to gone by default**

Change `content_claude` from `android:visibility="visible"` (implicit) to `android:visibility="gone"`.

Add `android:visibility="gone"` to the `content_claude` LinearLayout.

- [ ] **Step 6: Set content_home as default visible**

The `content_home` LinearLayout should NOT have `android:visibility="gone"` (it's visible by default).

---

### Task 8: Modify MainActivity.kt - Core Navigation Logic

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

- [ ] **Step 1: Update imports**

Add new imports:
```kotlin
import androidx.recyclerview.widget.LinearLayoutManager
import com.haoze.claudekeyboard.ui.home.HomeAdapter
import com.haoze.claudekeyboard.ui.home.HomeItem
```

Remove unused import:
```kotlin
import com.google.android.material.bottomnavigation.BottomNavigationView
```

- [ ] **Step 2: Replace member variables**

Remove:
```kotlin
    // Bottom navigation
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var contentClaude: View
    private lateinit var contentKeyboard: View
    private var keyboardFragment: KeyboardFragment? = null
    private lateinit var contentTouchpad: View
    private var touchpadFragment: TouchpadFragment? = null
```

Add:
```kotlin
    // Navigation
    private lateinit var contentHome: View
    private lateinit var contentClaude: View
    private lateinit var contentKeyboard: View
    private lateinit var contentTouchpad: View
    private lateinit var contentSettings: View
    private var keyboardFragment: KeyboardFragment? = null
    private var touchpadFragment: TouchpadFragment? = null
    private lateinit var homeAdapter: HomeAdapter
```

- [ ] **Step 3: Update initViews()**

Remove:
```kotlin
        bottomNav = findViewById(R.id.bottom_nav)
        contentClaude = findViewById(R.id.content_claude)
        contentKeyboard = findViewById(R.id.content_keyboard)
        contentTouchpad = findViewById(R.id.content_touchpad)

        tvDeviceAction.setOnClickListener {
            it.performKeyClick()
            val service = hidService
            if (service != null && service.isConnected()) {
                service.disconnect()
            } else {
                showDeviceListDialog()
            }
        }
        btnSettings.setOnClickListener {
            it.performKeyClick()
            showSettingsDialog()
        }
```

Add:
```kotlin
        contentHome = findViewById(R.id.content_home)
        contentClaude = findViewById(R.id.content_claude)
        contentKeyboard = findViewById(R.id.content_keyboard)
        contentTouchpad = findViewById(R.id.content_touchpad)
        contentSettings = findViewById(R.id.content_settings)
```

Remove `tvDeviceAction` and `btnSettings` from initViews since they are removed or relocated.

Remove these member variable declarations:
```kotlin
    private lateinit var tvDeviceName: TextView
    private lateinit var tvDeviceAction: TextView
    private lateinit var btnSettings: ImageButton
```

Remove these from initViews:
```kotlin
        tvDeviceName = findViewById(R.id.tv_device_name)
        tvDeviceAction = findViewById(R.id.tv_device_action)
        btnSettings = findViewById(R.id.btn_settings)
```

- [ ] **Step 4: Replace setupBottomNavigation() with setupHomePage()**

Remove the entire `setupBottomNavigation()` method.

Add new method:
```kotlin
    private fun setupHomePage() {
        val rvHome = findViewById<RecyclerView>(R.id.rv_home)
        homeAdapter = HomeAdapter { entry ->
            when (entry.id) {
                "keyboard" -> navigateToPage(contentKeyboard, landscape = true)
                "touchpad" -> navigateToPage(contentTouchpad, landscape = true)
                "claude" -> navigateToPage(contentClaude, landscape = false)
                "device" -> showDeviceListDialog()
                "settings" -> navigateToPage(contentSettings, landscape = false)
            }
        }
        rvHome.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = homeAdapter
        }
        loadHomeItems()
    }

    private fun loadHomeItems() {
        val items = listOf(
            HomeItem.Section(getString(R.string.home_functions)),
            HomeItem.Entry(
                id = "keyboard",
                title = getString(R.string.home_keyboard_title),
                subtitle = getString(R.string.home_keyboard_subtitle),
                iconRes = R.drawable.ic_home_keyboard
            ),
            HomeItem.Entry(
                id = "touchpad",
                title = getString(R.string.home_touchpad_title),
                subtitle = getString(R.string.home_touchpad_subtitle),
                iconRes = R.drawable.ic_home_touchpad
            ),
            HomeItem.Entry(
                id = "claude",
                title = getString(R.string.home_claude_title),
                subtitle = getString(R.string.home_claude_subtitle),
                iconRes = R.drawable.ic_home_claude
            ),
            HomeItem.Section(getString(R.string.home_system)),
            HomeItem.Entry(
                id = "device",
                title = getString(R.string.home_device_title),
                subtitle = getString(R.string.home_device_subtitle),
                iconRes = R.drawable.ic_home_device
            ),
            HomeItem.Entry(
                id = "settings",
                title = getString(R.string.home_settings_title),
                subtitle = getString(R.string.home_settings_subtitle),
                iconRes = R.drawable.ic_home_settings
            )
        )
        homeAdapter.submitList(items)
    }
```

- [ ] **Step 5: Add navigateToPage() and navigateToHome() methods**

```kotlin
    private fun navigateToPage(targetContent: View, landscape: Boolean) {
        contentHome.visibility = View.GONE
        contentClaude.visibility = View.GONE
        contentKeyboard.visibility = View.GONE
        contentTouchpad.visibility = View.GONE
        contentSettings.visibility = View.GONE
        targetContent.visibility = View.VISIBLE

        if (landscape) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }

        if (targetContent == contentKeyboard) {
            keyboardFragment = supportFragmentManager.findFragmentById(R.id.keyboard_fragment_container) as? KeyboardFragment
            updateKeyboardEnabled()
        } else if (targetContent == contentTouchpad) {
            touchpadFragment = supportFragmentManager.findFragmentById(R.id.touchpad_fragment_container) as? TouchpadFragment
            updateTouchpadEnabled()
        }
    }

    fun navigateToHome() {
        contentHome.visibility = View.VISIBLE
        contentClaude.visibility = View.GONE
        contentKeyboard.visibility = View.GONE
        contentTouchpad.visibility = View.GONE
        contentSettings.visibility = View.GONE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        updateDeviceSubtitle()
    }

    private fun updateDeviceSubtitle() {
        val isConnected = hidService?.isConnected() == true
        val deviceName = hidService?.getConnectedDeviceName()
        val subtitle = if (isConnected && deviceName != null) {
            getString(R.string.device_name_status, deviceName, getString(R.string.status_connected_label))
        } else {
            getString(R.string.home_device_subtitle)
        }
        homeAdapter.updateSubtitle("device", subtitle)
    }
```

- [ ] **Step 6: Replace switchToClaudeTab/switchToTouchpadTab/switchToKeyboardTab**

Remove:
```kotlin
    fun switchToClaudeTab() {
        bottomNav.selectedItemId = R.id.nav_claude
    }

    fun switchToTouchpadTab() {
        bottomNav.selectedItemId = R.id.nav_touchpad
    }

    fun switchToKeyboardTab() {
        bottomNav.selectedItemId = R.id.nav_keyboard
    }
```

Replace with:
```kotlin
    fun switchToClaudeTab() {
        navigateToPage(contentClaude, landscape = false)
    }

    fun switchToTouchpadTab() {
        navigateToPage(contentTouchpad, landscape = true)
    }

    fun switchToKeyboardTab() {
        navigateToPage(contentKeyboard, landscape = true)
    }
```

- [ ] **Step 7: Remove animateBottomNavIn() method**

Remove the entire method:
```kotlin
    private fun animateBottomNavIn(nav: View, divider: View) { ... }
```

- [ ] **Step 8: Update setupWindowInsets()**

Remove the bottom nav inset listener:
```kotlin
        // Let bottom nav handle the bottom system bar inset
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
```

- [ ] **Step 9: Update onCreate()**

Replace `setupBottomNavigation()` with `setupHomePage()`:
```kotlin
        setupHomePage()
```

- [ ] **Step 10: Update updateStatusUI() to update home page device subtitle**

In `updateStatusUI()`, after the existing logic, add:
```kotlin
        updateDeviceSubtitle()
```

- [ ] **Step 11: Add back button handling**

Add `onBackPressedDispatcher` in `onCreate()`:
```kotlin
        onBackPressedDispatcher.addCallback(this) {
            if (contentHome.visibility != View.VISIBLE) {
                navigateToHome()
            } else {
                finish()
            }
        }
```

- [ ] **Step 12: Setup settings page back button and interactions**

Add method:
```kotlin
    private fun setupSettingsPage() {
        val btnBack = findViewById<ImageButton>(R.id.btn_back_settings)
        btnBack.setOnClickListener {
            it.performKeyClick()
            navigateToHome()
        }

        val themeModeLayout = findViewById<View>(R.id.settings_theme_mode)
        val tvThemeCurrent = findViewById<TextView>(R.id.tv_theme_current)
        val prefs = getSharedPreferences(Constants.PREFS_NAME_SETTINGS, Context.MODE_PRIVATE)
        val currentTheme = prefs.getInt(Constants.KEY_THEME_MODE, Constants.THEME_FOLLOW_SYSTEM)
        tvThemeCurrent.text = getThemeModeName(currentTheme)

        themeModeLayout.setOnClickListener {
            showThemeSelectionDialog(tvThemeCurrent)
        }

        val resetMacrosLayout = findViewById<View>(R.id.settings_reset_macros)
        resetMacrosLayout.setOnClickListener {
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
    }
```

Call it in `onCreate()` after `setupHomePage()`:
```kotlin
        setupSettingsPage()
```

- [ ] **Step 13: Update showThemeSelectionDialog to accept TextView**

Change the parameter type from `TextView` to `TextView` (it already is), but update the method to also call `updateDeviceSubtitle` when theme changes (no change needed, just verify).

- [ ] **Step 14: Remove showSettingsDialog() method**

Delete the entire `showSettingsDialog()` method since settings is now a page, not a dialog.

- [ ] **Step 15: Remove getThemeModeName() duplication**

The `getThemeModeName()` method is still needed for the settings page. Keep it.

---

### Task 9: Update KeyboardFragment Back Buttons

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/ui/keyboard/KeyboardFragment.kt`

- [ ] **Step 1: Change switchToClaudeTab() to navigateToHome()**

Find all occurrences of:
```kotlin
(activity as? MainActivity)?.switchToClaudeTab()
```

Replace with:
```kotlin
(activity as? MainActivity)?.navigateToHome()
```

There should be ~1 occurrence in the back button click listener.

---

### Task 10: Update TouchpadFragment Back Buttons

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/ui/touchpad/TouchpadFragment.kt`

- [ ] **Step 1: Change switchToClaudeTab() to navigateToHome()**

Find all occurrences of:
```kotlin
(activity as? MainActivity)?.switchToClaudeTab()
```

Replace with:
```kotlin
(activity as? MainActivity)?.navigateToHome()
```

- [ ] **Step 2: Update back button text in fragment_touchpad.xml**

Change the back button text from `@string/tab_claude` to `@string/nav_back`:
```xml
<TextView
    android:id="@+id/tv_back_to_claude"
    ...
    android:text="@string/nav_back"
    ... />
```

---

### Task 11: Clean Up Unused Resources

**Files:**
- Delete: `app/src/main/res/menu/bottom_nav_menu.xml`
- Modify: `app/src/main/res/values/strings.xml` (optional: remove unused tab strings)

- [ ] **Step 1: Delete bottom_nav_menu.xml**

```bash
rm app/src/main/res/menu/bottom_nav_menu.xml
```

- [ ] **Step 2: Verify build succeeds**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: replace bottom nav with settings-style home page"
```

---

## Verification Checklist

After all tasks are complete, verify:

1. App launches to home page (not Claude tab)
2. Home page shows 5 entries in 2 sections (功能: 键盘/触控板/Claude辅助; 系统: 设备连接/设置)
3. Tapping "键盘" navigates to keyboard page in landscape with back button
4. Tapping "触控板" navigates to touchpad page in landscape with back button
5. Tapping "Claude 辅助" navigates to Claude page in portrait with back button
6. Tapping "设备连接" opens device list bottom sheet
7. Tapping "设置" navigates to settings page with theme mode and reset macros
8. Back button on any page returns to home page
9. System back button returns to home from sub-pages, exits from home
10. Device connection status shows in home page "设备连接" subtitle
11. Keyboard/Touchpad cross-navigation still works (via their existing buttons)
12. No bottom navigation bar visible anywhere
