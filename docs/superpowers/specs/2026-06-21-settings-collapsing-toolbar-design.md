# Settings Page Collapsing Toolbar Design

**Date:** 2026-06-21
**Status:** Pending Approval

## Overview

Implement a collapsing toolbar for the settings page that hides the title bar when scrolling down, while keeping the back button fixed at the top.

## Current State

- Title bar: ConstraintLayout (72dp height, gradient background `bg_home_hero`)
- Content: RecyclerView with settings items
- Root: LinearLayout (vertical orientation)

## Design Goals

1. **Collapsing Behavior** - Title bar completely collapses when scrolling down
2. **Fixed Back Button** - Back button remains visible at top left during collapse
3. **Smooth Animation** - Material Design standard elastic animation

## Architecture

**Approach:** CoordinatorLayout + AppBarLayout + CollapsingToolbarLayout

### Current Structure
```
LinearLayout (vertical)
  ├── ConstraintLayout (title bar 72dp)
  └── RecyclerView
```

### New Structure
```
CoordinatorLayout
  ├── AppBarLayout
  │   └── CollapsingToolbarLayout
  │       ├── ConstraintLayout (title bar content)
  │       └── ImageButton (back button, pinned mode)
  └── RecyclerView (scroll behavior)
```

## Detailed Design

### 1. Root Layout Change

**File:** `content_settings.xml`

Replace `LinearLayout` root with `CoordinatorLayout`:
```xml
<CoordinatorLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_home_screen">
```

### 2. AppBarLayout Configuration

Wrap title bar in `AppBarLayout`:
```xml
<com.google.android.material.appbar.AppBarLayout
    android:id="@+id/app_bar_settings"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:background="@android:color/transparent"
    app:elevation="0dp">
```

Key attributes:
- `elevation="0dp"` - No shadow, keep gradient background clean
- `background="@android:color/transparent"` - Let gradient show through

### 3. CollapsingToolbarLayout Configuration

Wrap title bar content in `CollapsingToolbarLayout`:
```xml
<com.google.android.material.appbar.CollapsingToolbarLayout
    android:id="@+id/collapsing_toolbar_settings"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:layout_scrollFlags="scroll|exitUntilCollapsed"
    app:contentScrim="@android:color/transparent">
```

Key attributes:
- `layout_scrollFlags="scroll|exitUntilCollapsed"` - Scrolls with content, collapses to pinned view
- `contentScrim="@android:color/transparent"` - No scrim color

### 4. Title Bar Content

Move existing title bar content inside `CollapsingToolbarLayout`:
```xml
<ConstraintLayout
    android:layout_width="match_parent"
    android:layout_height="72dp"
    android:layout_marginHorizontal="20dp"
    android:layout_marginTop="20dp"
    android:background="@drawable/bg_home_hero"
    android:paddingHorizontal="20dp"
    android:paddingVertical="12dp"
    app:layout_collapseMode="parallax">
```

Key attributes:
- `layout_collapseMode="parallax"` - Title bar scrolls with parallax effect
- Keep existing margins and background

### 5. Back Button (Fixed)

Move back button outside title bar content, with pin mode:
```xml
<ImageButton
    android:id="@+id/btn_back_settings"
    android:layout_width="36dp"
    android:layout_height="36dp"
    android:layout_marginStart="20dp"
    android:layout_marginTop="20dp"
    android:background="@drawable/bg_home_icon_button"
    android:contentDescription="@string/nav_back"
    android:scaleType="centerInside"
    android:src="@drawable/baseline_arrow_back_24"
    app:layout_collapseMode="pin"
    app:layout_anchor="@id/app_bar_settings"
    app:layout_anchorGravity="top|start"
    app:tint="@color/home_on_hero" />
```

Key attributes:
- `layout_collapseMode="pin"` - Fixed at top
- `layout_anchor` - Anchored to AppBarLayout
- `layout_anchorGravity="top|start"` - Position at top-left

### 6. RecyclerView Configuration

Update RecyclerView with scroll behavior:
```xml
<androidx.recyclerview.widget.RecyclerView
    android:id="@+id/rv_settings"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:clipToPadding="false"
    android:paddingVertical="12dp"
    app:layout_behavior="@string/appbar_scrolling_view_behavior" />
```

Key attributes:
- `layout_behavior="@string/appbar_scrolling_view_behavior"` - Links to AppBarLayout

## Visual Behavior

### Scroll Down
1. Title bar starts collapsing
2. Title text and gradient background scroll up
3. Back button remains fixed at top-left
4. When fully collapsed: only back button visible

### Scroll Up
1. Title bar expands back to original height
2. Gradient background and title text reappear
3. Smooth elastic animation

## File Changes

| File | Changes |
|------|---------|
| `content_settings.xml` | Replace LinearLayout with CoordinatorLayout, restructure title bar |

## Dependencies

- Material Design Components library (already in project)
- No new dependencies required

## Testing

- Verify title bar collapses when scrolling down
- Verify back button remains fixed at top
- Verify title bar expands when scrolling up
- Verify back button click still works
- Verify smooth animation
- Verify dark mode compatibility

## Scope

Layout-only change. No new features, no behavioral changes to settings functionality. Only affects visual presentation and scroll behavior.
