# Fix Custom Macro & Add Enter Toggle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix the broken "add custom macro" button and add a per-macro "send Enter" toggle.

**Architecture:** The `Macro` data class gains a `sendEnter` field (default false). The dialog gets a Material3 Switch. The click handler in `MainActivity` is fixed to detect the "add_custom" sentinel and open the dialog instead of sending an empty command. Persistence handles the new field with backward compatibility.

**Tech Stack:** Kotlin, Android Material3, SharedPreferences + JSON

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `app/src/main/java/com/haoze/claudekeyboard/macro/Macro.kt` | Modify | Add `sendEnter` field, update factory methods |
| `app/src/main/res/values/strings.xml` | Modify | Add `switch_send_enter` string resource |
| `app/src/main/res/layout/dialog_macro_edit.xml` | Modify | Add MaterialSwitch below command input |
| `app/src/main/java/com/haoze/claudekeyboard/ui/macro/MacroEditDialogFragment.kt` | Modify | Read/write Switch state, update listener signature |
| `app/src/main/java/com/haoze/claudekeyboard/macro/MacroRepository.kt` | Modify | Persist `sendEnter` in JSON, backward compat |
| `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt` | Modify | Fix add_custom click, pass sendEnter to send logic |

---

### Task 1: Add `sendEnter` field to Macro data class

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/macro/Macro.kt`

- [ ] **Step 1: Add `sendEnter` property to data class**

In `Macro.kt`, add `sendEnter: Boolean = false` after `sortOrder`:

```kotlin
data class Macro(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val command: String,
    val isPreset: Boolean = false,
    val sortOrder: Int = 0,
    val sendEnter: Boolean = false
)
```

- [ ] **Step 2: Update `custom()` factory method**

```kotlin
fun custom(label: String, command: String, sendEnter: Boolean = false): Macro {
    return Macro(
        label = label,
        command = command,
        isPreset = false,
        sortOrder = 1000,
        sendEnter = sendEnter
    )
}
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 2: Add string resource for Switch label

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add the string resource**

Add before the closing `</resources>` tag:

```xml
<string name="switch_send_enter">发送回车</string>
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 3: Add Switch to dialog layout

**Files:**
- Modify: `app/src/main/res/layout/dialog_macro_edit.xml`

- [ ] **Step 1: Add MaterialSwitch after the command TextInputLayout**

Add a `com.google.android.material.materialswitch.MaterialSwitch` element after the second `TextInputLayout` (the command field), still inside the root `LinearLayout`:

```xml
<com.google.android.material.materialswitch.MaterialSwitch
    android:id="@+id/switch_send_enter"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:layout_marginTop="8dp"
    android:text="@string/switch_send_enter"
    android:textAppearance="?attr/textAppearanceBodyLarge"
    android:textColor="?attr/colorOnSurface" />
```

The full file should be:

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

    <com.google.android.material.materialswitch.MaterialSwitch
        android:id="@+id/switch_send_enter"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="8dp"
        android:text="@string/switch_send_enter"
        android:textAppearance="?attr/textAppearanceBodyLarge"
        android:textColor="?attr/colorOnSurface" />

</LinearLayout>
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 4: Update MacroEditDialogFragment to handle Switch

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/ui/macro/MacroEditDialogFragment.kt`

- [ ] **Step 1: Add ARG constant and update newInstance methods**

Add `ARG_MACRO_SEND_ENTER` constant and update both `newInstance` methods:

```kotlin
companion object {
    private const val ARG_MACRO_ID = "macro_id"
    private const val ARG_MACRO_LABEL = "macro_label"
    private const val ARG_MACRO_COMMAND = "macro_command"
    private const val ARG_MACRO_SEND_ENTER = "macro_send_enter"
    private const val ARG_IS_EDIT = "is_edit"

    fun newInstance(): MacroEditDialogFragment {
        return MacroEditDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_EDIT, false)
            }
        }
    }

    fun newInstance(macro: Macro): MacroEditDialogFragment {
        return MacroEditDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(ARG_IS_EDIT, true)
                putString(ARG_MACRO_ID, macro.id)
                putString(ARG_MACRO_LABEL, macro.label)
                putString(ARG_MACRO_COMMAND, macro.command)
                putBoolean(ARG_MACRO_SEND_ENTER, macro.sendEnter)
            }
        }
    }
}
```

- [ ] **Step 2: Update listener signature to include sendEnter**

Change `onSaveListener` type and setter:

```kotlin
private var onSaveListener: ((String?, String, String, Boolean) -> Unit)? = null

fun setOnSaveListener(listener: (String?, String, String, Boolean) -> Unit) {
    onSaveListener = listener
}
```

- [ ] **Step 3: Update onCreateDialog to wire Switch**

Update `onCreateDialog` to find the Switch, set its initial state, and pass it in the save callback:

```kotlin
override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val isEdit = arguments?.getBoolean(ARG_IS_EDIT) ?: false
    val macroId = arguments?.getString(ARG_MACRO_ID)
    val macroLabel = arguments?.getString(ARG_MACRO_LABEL) ?: ""
    val macroCommand = arguments?.getString(ARG_MACRO_COMMAND) ?: ""
    val macroSendEnter = arguments?.getBoolean(ARG_MACRO_SEND_ENTER) ?: false

    val inflater = LayoutInflater.from(requireContext())
    val view = inflater.inflate(R.layout.dialog_macro_edit, null)

    val etLabel = view.findViewById<EditText>(R.id.et_macro_label)
    val etCommand = view.findViewById<EditText>(R.id.et_macro_command)
    val switchSendEnter = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switch_send_enter)

    if (isEdit) {
        etLabel.setText(macroLabel)
        etCommand.setText(macroCommand)
        switchSendEnter.isChecked = macroSendEnter
    }

    val dialogBuilder = MaterialAlertDialogBuilder(requireContext())
        .setTitle(if (isEdit) R.string.dialog_edit_macro else R.string.dialog_add_macro)
        .setView(view)
        .setPositiveButton(R.string.dialog_save) { _, _ ->
            val label = etLabel.text.toString().trim()
            val command = etCommand.text.toString().trim()
            val sendEnter = switchSendEnter.isChecked

            if (label.isNotEmpty() && command.isNotEmpty()) {
                onSaveListener?.invoke(macroId, label, command, sendEnter)
            }
        }
        .setNegativeButton(R.string.dialog_cancel, null)

    if (isEdit && macroId != null) {
        dialogBuilder.setNeutralButton(R.string.dialog_delete) { _, _ ->
            onDeleteListener?.invoke(macroId)
        }
    }

    return dialogBuilder.create()
}
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 5: Update MacroRepository to persist sendEnter

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/macro/MacroRepository.kt`

- [ ] **Step 1: Update `addCustomMacro` to accept sendEnter**

```kotlin
fun addCustomMacro(label: String, command: String, sendEnter: Boolean = false): Macro {
    val macro = Macro.custom(label, command, sendEnter)
    val macros = getAllMacros().toMutableList()
    macros.add(macro)
    saveMacrosToStorage(macros)
    return macro
}
```

- [ ] **Step 2: Update `updateCustomMacro` to accept sendEnter**

```kotlin
fun updateCustomMacro(id: String, label: String, command: String, sendEnter: Boolean = false): Boolean {
    val macros = getAllMacros().toMutableList()
    val index = macros.indexOfFirst { it.id == id && !it.isPreset }

    if (index == -1) return false

    macros[index] = macros[index].copy(label = label, command = command, sendEnter = sendEnter)
    saveMacrosToStorage(macros)
    return true
}
```

- [ ] **Step 3: Update JSON deserialization to read sendEnter**

In `loadMacrosFromStorage()`, update the `Macro` constructor call inside the try block:

```kotlin
Macro(
    id = obj.getString("id"),
    label = obj.getString("label"),
    command = obj.getString("command"),
    isPreset = obj.getBoolean("isPreset"),
    sortOrder = obj.getInt("sortOrder"),
    sendEnter = obj.optBoolean("sendEnter", false)
)
```

Note: `optBoolean` with default `false` ensures backward compatibility with stored JSON that lacks the field.

- [ ] **Step 4: Update JSON serialization to write sendEnter**

In `saveMacrosToStorage()`, add `put("sendEnter", macro.sendEnter)` to the JSONObject:

```kotlin
val obj = JSONObject().apply {
    put("id", macro.id)
    put("label", macro.label)
    put("command", macro.command)
    put("isPreset", macro.isPreset)
    put("sortOrder", macro.sortOrder)
    put("sendEnter", macro.sendEnter)
}
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 6: Fix MainActivity click handler and wire sendEnter

**Files:**
- Modify: `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt`

- [ ] **Step 1: Fix onMacroClick to handle add_custom button**

In `setupMacroRecyclerView()`, update the `onMacroClick` lambda (line 148):

```kotlin
onMacroClick = { macro ->
    if (macro.id == "add_custom") {
        showAddMacroDialog()
    } else {
        hidService?.getKeyboardSender()?.let { s ->
            Thread {
                if (macro.sendEnter) s.sendMacro(macro.command)
                else s.sendText(macro.command)
            }.start()
        }
    }
},
```

- [ ] **Step 2: Update showEditMacroDialog to pass sendEnter**

Update the listener in `showEditMacroDialog()` (line 236):

```kotlin
private fun showEditMacroDialog(macro: Macro) {
    val dialog = MacroEditDialogFragment.newInstance(macro)
    dialog.setOnSaveListener { id, label, command, sendEnter ->
        if (id != null) {
            macroRepository.updateCustomMacro(id, label, command, sendEnter)
            Toast.makeText(this, R.string.toast_macro_updated, Toast.LENGTH_SHORT).show()
        }
        loadMacros()
    }
    dialog.setOnDeleteListener { id -> showDeleteConfirmationDialog(id) }
    dialog.show(supportFragmentManager, "edit_macro")
}
```

- [ ] **Step 3: Update showAddMacroDialog to pass sendEnter**

Update the listener in `showAddMacroDialog()` (line 246):

```kotlin
private fun showAddMacroDialog() {
    val dialog = MacroEditDialogFragment.newInstance()
    dialog.setOnSaveListener { _, label, command, sendEnter ->
        macroRepository.addCustomMacro(label, command, sendEnter)
        Toast.makeText(this, R.string.toast_macro_added, Toast.LENGTH_SHORT).show()
        loadMacros()
    }
    dialog.show(supportFragmentManager, "add_macro")
}
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

---

### Task 7: Final build and verify

- [ ] **Step 1: Full debug build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 2: Commit all changes**

```bash
git add -A
git commit -m "fix: wire add_custom macro button and add sendEnter toggle to macro dialog"
```
