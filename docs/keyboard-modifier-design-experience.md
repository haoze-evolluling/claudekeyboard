# 键盘修饰键独立化改造经验总结

> 项目：ClaudeKeyboard 蓝牙 HID 键盘  
> 日期：2026-06-11  
> 改造内容：左右 Win 键、左右 Shift 键行为独立化

---

## 1. 核心问题：共享状态导致联动

最初左右 Win 键共用一个 `isWinActive` 布尔值，左右 Shift 键也共用同一个 `shiftButtons` 列表，导致：

- 点左 Win → 两边同时高亮
- 点右 Win → 两边同时高亮
- 无法实现不同的行为模式

**教训：** 功能不同的按键必须用独立的状态变量追踪，即使它们外观相同、名称相同。共享状态只适用于行为完全一致的按键（如左右 Ctrl 同为切换模式）。

---

## 2. 行为模式的定义要清晰

在动手写代码前，先明确每个按键的完整行为生命周期（按下 → 触发 → 释放 → 后续状态），避免边写边改。

### 左 Win vs 右 Win 行为对比

| 维度 | 左 Win | 右 Win |
|------|--------|--------|
| 行为模式 | 单次触发（fire-and-forget） | 修饰键切换（toggle） |
| 点击后 | 发送 Win 键 HID 报告，无状态残留 | 激活修饰状态，按钮高亮 |
| 配合其他键 | 不需要（已发送完毕） | 需要（如 Win+R） |
| 按其他键后 | 无变化 | 保持高亮，直到再次点击关闭 |
| 视觉反馈 | 按下高亮，松开恢复 | 按下高亮，松开保持蓝色选中态 |

### 左 Shift vs 右 Shift 行为对比

| 维度 | 左 Shift | 右 Shift |
|------|----------|----------|
| 行为模式 | 一次性修饰（one-shot） | 符号锁定切换（symbol lock） |
| 点击后 | 激活 Shift，下一个键使用后自动释放 | 切换符号锁定，数字键显示符号 |
| 持续时间 | 单次按键后释放 | 持久，直到再次点击关闭 |
| 视觉反馈 | 按下高亮，下一个键后恢复 | 按下保持蓝色选中态 |

---

## 3. 三种修饰键行为模式

通过本次改造，总结出修饰键的三种典型行为模式：

### 模式一：即发即忘（Fire-and-Forget）

**适用：** 左 Win 键

直接发送 HID 报告，不修改任何状态变量，不需要自动释放逻辑。

```kotlin
KeyboardSender.MODIFIER_GUI_LEFT -> {
    val sender = getKeyboardSender()
    if (sender != null) {
        Thread { sender.sendKeyPress(KeyboardSender.MODIFIER_GUI_LEFT, 0x00) }.start()
    }
}
```

**优点：** 最简单，无状态残留，无视觉闪烁。

### 模式二：一次性修饰（One-Shot）

**适用：** 左 Shift 键

点击激活状态，下一个按键使用后自动释放。

```kotlin
// 激活
isShiftActive = !isShiftActive

// 在 onKeyPressed 中，非修饰键按下后自动释放
if (isShiftActive) {
    isShiftActive = false
    updateModifierVisuals()
    updateAllKeyLabels()
}
```

**注意：** 如果用自动释放来模拟"即发即忘"，会导致视觉闪烁（先激活高亮，再恢复）。能用模式一解决的就不要用模式二。

### 模式三：切换锁定（Toggle Lock）

**适用：** 右 Win 键、右 Shift 键（符号锁定）、Ctrl、Alt

点击激活，保持高亮，再次点击关闭。可配合其他按键使用。

```kotlin
KeyboardSender.MODIFIER_GUI_RIGHT -> {
    isWinRightActive = !isWinRightActive
}
```

**特点：** 不自动释放，状态持久，直到用户主动关闭。

---

## 4. 架构设计原则

### 4.1 独立状态变量

```kotlin
// ✅ 正确：各自独立
private var isWinLeftActive = false
private var isWinRightActive = false

// ❌ 错误：共享状态
private var isWinActive = false
```

### 4.2 buildModifierByte 中正确映射

```kotlin
// 左 Win 映射到 GUI_LEFT，右 Win 映射到 GUI_RIGHT
if (isWinLeftActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_GUI_LEFT.toInt()).toByte()
if (isWinRightActive) modifier = (modifier.toInt() or KeyboardSender.MODIFIER_GUI_RIGHT.toInt()).toByte()
```

### 4.3 updateModifierVisuals 中独立更新

```kotlin
winLeftButton?.let {
    it.setBackgroundResource(if (isWinLeftActive) activeBg else normalBg)
}
winRightButton?.let {
    it.setBackgroundResource(if (isWinRightActive) activeBg else normalBg)
}
```

---

## 5. 触摸监听器的可靠性

### 问题

Modifier 键最初用 `setOnClickListener` + `setOnTouchListener` 组合，touch listener 返回 `false` 让 click listener 触发。在某些设备上 click listener 不可靠触发。

### 解决方案

对于需要精确控制按下/释放行为的按键，直接在 `OnTouchListener` 中处理所有逻辑，返回 `true` 消费事件：

```kotlin
button.setOnTouchListener { v, event ->
    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            button.setBackgroundResource(R.drawable.bg_key_pressed)
            v.isPressed = true
            true  // 消费事件
        }
        MotionEvent.ACTION_UP -> {
            v.isPressed = false
            onKeyPressed(keyData, button)
            updateModifierVisuals()
            true  // 消费事件
        }
        MotionEvent.ACTION_CANCEL -> {
            v.isPressed = false
            updateModifierVisuals()
            true
        }
        else -> false
    }
}
```

**不要依赖 `OnClickListener` 处理修饰键。**

---

## 6. 反模式识别

当发现需要用"补丁逻辑"来修正行为时，说明初始设计有问题：

| 反模式 | 信号 | 正确做法 |
|--------|------|----------|
| 自动释放模拟即发即忘 | 需要在 onKeyPressed 中加 auto-release | 改用 fire-and-forget 模式 |
| 共享状态 + 条件分支 | toggleModifier 中用 if 区分左右 | 拆分为独立状态变量 |
| touch + click 双重监听 | touch listener 返回 false 依赖 click | 统一在 touch listener 中处理 |

---

## 7. 改造清单（供后续参考）

当需要为同一类按键实现不同行为时，按以下步骤操作：

1. **定义行为模式**：明确每个按键属于哪种模式（即发即忘 / 一次性修饰 / 切换锁定）
2. **拆分状态变量**：每个独立行为的按键对应独立的状态变量
3. **更新 toggleModifier**：按 modifierBit 分支处理
4. **更新 buildModifierByte**：每个状态变量正确映射到 HID 修饰符
5. **更新 updateModifierVisuals**：每个按钮独立更新视觉状态
6. **更新 onKeyPressed 自动释放**：只对 one-shot 模式的按键生效
7. **更新触摸监听器中的 effectiveModifier**：确保长按重复时使用正确的修饰符
8. **完整编译测试**：`./gradlew clean assembleDebug`
