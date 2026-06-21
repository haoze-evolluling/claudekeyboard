# Settings Page Collapsing Toolbar Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a collapsing toolbar for the settings page that hides the title bar when scrolling down, while keeping the back button fixed at the top.

**Architecture:** Replace LinearLayout root with CoordinatorLayout, wrap title bar in AppBarLayout + CollapsingToolbarLayout, configure scroll flags and collapse modes.

**Tech Stack:** Android XML layouts, Material Design Components, CoordinatorLayout

## Global Constraints

- Preserve all existing functionality (back button click, settings list scrolling)
- Support both light and dark modes
- Maintain accessibility (content descriptions, touch targets)
- Follow existing code patterns and naming conventions

---

### Task 1: Restructure Settings Layout with CoordinatorLayout

**Files:**
- Modify: `app/src/main/res/layout/content_settings.xml`

**Interfaces:**
- Consumes: Existing layout structure
- Produces: New CoordinatorLayout-based structure

- [ ] **Step 1: Replace root LinearLayout with CoordinatorLayout**

Replace the entire `content_settings.xml` with the new structure:

```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@drawable/bg_home_screen">

    <com.google.android.material.appbar.AppBarLayout
        android:id="@+id/app_bar_settings"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:background="@android:color/transparent"
        app:elevation="0dp">

        <com.google.android.material.appbar.CollapsingToolbarLayout
            android:id="@+id/collapsing_toolbar_settings"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            app:layout_scrollFlags="scroll|exitUntilCollapsed"
            app:contentScrim="@android:color/transparent">

            <androidx.constraintlayout.widget.ConstraintLayout
                android:layout_width="match_parent"
                android:layout_height="72dp"
                android:layout_marginHorizontal="20dp"
                android:layout_marginTop="20dp"
                android:background="@drawable/bg_home_hero"
                android:paddingHorizontal="20dp"
                android:paddingVertical="12dp"
                app:layout_collapseMode="parallax">

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

        </com.google.android.material.appbar.CollapsingToolbarLayout>

    </com.google.android.material.appbar.AppBarLayout>

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_settings"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:clipToPadding="false"
        android:paddingVertical="12dp"
        app:layout_behavior="@string/appbar_scrolling_view_behavior" />

</androidx.coordinatorlayout.widget.CoordinatorLayout>
```

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/content_settings.xml
git commit -m "feat: implement collapsing toolbar for settings page"
```

---

### Task 2: Verify Back Button Functionality

**Files:**
- None (verification only)

**Interfaces:**
- Consumes: New layout structure
- Produces: Verified back button works

- [ ] **Step 1: Verify back button click handler**

Check that `MainActivity.kt` still finds the back button correctly:
- The button ID `btn_back_settings` remains the same
- The `findViewById` call should still work

- [ ] **Step 2: Build and verify**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit if any changes needed**

If any changes were needed:
```bash
git add -A
git commit -m "fix: ensure back button works with new layout structure"
```

---

### Task 3: Final Verification

**Files:**
- None (verification only)

**Interfaces:**
- Consumes: All previous changes
- Produces: Verified working collapsing toolbar

- [ ] **Step 1: Full build verification**

Run: `./gradlew clean assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Verify no compilation errors**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Final commit**

```bash
git add -A
git commit -m "feat: settings page collapsing toolbar - complete implementation"
```

---

## Summary

| Task | Files Modified | Key Changes |
|------|----------------|-------------|
| 1 | 1 layout file | Replace LinearLayout with CoordinatorLayout + AppBarLayout + CollapsingToolbarLayout |
| 2 | None | Verify back button functionality |
| 3 | None | Final build verification |

**Total files modified:** 1 (XML layout)
**Estimated time:** 10-15 minutes
