# 触控板功能设计

**日期**: 2026-06-11
**状态**: 已批准
**范围**: 为底栏新增"触控板"标签页，支持手指滑动模拟鼠标功能

---

## 1. 目标

在现有"Claude"和"键盘"两个底栏标签的基础上，新增"触控板"标签页。用户可以通过手指在触控板区域滑动来控制电脑上的鼠标光标，支持移动、左键/右键点击和滚轮滚动。

## 2. 功能需求

### 2.1 核心功能
- **光标移动**: 单指在触控板区域滑动，控制电脑鼠标光标的相对移动
- **左键点击**: 底部左键按钮，或单指轻触（tap）触控板区域
- **右键点击**: 底部右键按钮
- **滚轮滚动**: 双指在触控板区域上下滑动

### 2.2 灵敏度调节
- 触控板界面顶部提供灵敏度滑块（范围 1~10，默认 5）
- 实时生效，值保存到 SharedPreferences

### 2.3 界面布局
```
┌─────────────────────────────────────┐
│ [← Claude]           [灵敏度: ━━●━] │  ← 顶栏
├─────────────────────────────────────┤
│                                     │
│         触控板区域（大面积）           │  ← 单指滑动=移动, 双指滑动=滚动
│                                     │
├─────────────────────────────────────┤
│      [左键]              [右键]      │  ← 底部按钮
└─────────────────────────────────────┘
```

### 2.4 屏幕方向
- 横屏（与键盘标签页一致）

## 3. 技术方案

### 3.1 HID 层：复合描述符

**修改 `DescriptorCollection.kt`**：新增 `COMBINED` 描述符，包含两个 TLC：

**TLC 1: 键盘 (Report ID 1)**
- 与现有键盘描述符相同，但添加 Report ID
- 报告格式: `[ReportID(1) + Modifier(1) + Reserved(1) + Keys(6)]` = 9 字节

**TLC 2: 鼠标 (Report ID 2)**
- Usage Page: Generic Desktop, Usage: Mouse
- 报告格式: `[ReportID(1) + Buttons(1) + X(1) + Y(1) + Wheel(1)]` = 5 字节
- Buttons: bit0=左键, bit1=右键, bit2=中键
- X/Y: 有符号字节 (-127 ~ +127)，相对移动
- Wheel: 有符号字节，正=上滚，负=下滚

### 3.2 报告类

**修改 `KeyboardReport.kt`**：
- Report ID 从 0 改为 1
- 报告大小从 8 字节改为 9 字节（首字节为 Report ID）
- 所有字段偏移 +1

**新建 `MouseReport.kt`**：
- 5 字节值类
- 字段: reportId(1) + buttons(1) + deltaX(1) + deltaY(1) + wheel(1)
- 按键状态: leftButton, rightButton, middleButton
- reset() 方法

### 3.3 发送层

**新建 `MouseSender.kt`**：
```kotlin
class MouseSender(
    val hidDevice: BluetoothHidDevice,
    val host: BluetoothDevice
) {
    val mouseReport = MouseReport()

    fun sendMouseReport()          // 发送当前报告
    fun sendMouseMove(dx: Int, dy: Int)  // 发送相对移动
    fun sendMouseClick(button: Int)      // 发送按键点击 (0=左, 1=右, 2=中)
    fun sendMouseScroll(amount: Int)     // 发送滚轮滚动
}
```

**修改 `BluetoothHidService.kt`**：
- 注册时使用 `DescriptorCollection.COMBINED`
- 新增 `mouseSender: MouseSender?` 字段
- 连接时同时创建 `KeyboardSender` 和 `MouseSender`
- 新增 `getMouseSender(): MouseSender?` 方法

### 3.4 UI 层

**新建 `fragment_touchpad.xml`**：
- 顶栏: 返回 Claude 按钮 + 灵敏度 Slider
- 触控板区域: 大面积可触摸 View
- 底部: 左键/右键 MaterialButton

**新建 `TouchpadFragment.kt`**：
- 继承 Fragment
- 处理触摸事件:
  - `ACTION_DOWN`: 记录起始位置，启动 tap 检测计时器
  - `ACTION_MOVE`: 计算相对移动 × 灵敏度系数，调用 `sendMouseMove()`
  - `ACTION_UP`: 如果移动距离 < 阈值且时间 < 阈值，判定为 tap → `sendMouseClick(0)`
  - 双指检测: 通过 `pointerCount` 判断，双指移动 → `sendMouseScroll()`
- 灵敏度 Slider 监听: 保存到 SharedPreferences，实时更新系数
- `setTouchpadEnabled(enabled)` 方法: 连接状态变化时启用/禁用

**修改 `activity_main.xml`**：
- 在 `content_container` 中添加 `content_touchpad` FrameLayout
- 包含 `FragmentContainerView` 托管 `TouchpadFragment`

**修改 `bottom_nav_menu.xml`**：
- 添加 `nav_touchpad` 标签，图标 `ic_tab_touchpad`，标题 "触控板"

**修改 `MainActivity.kt`**：
- `setupBottomNavigation()` 添加 `nav_touchpad` 处理
- 切换逻辑: 隐藏其他内容，显示 `content_touchpad`，横屏
- `getMouseSender()` 方法
- `switchToClaudeTab()` 保持不变

### 3.5 图标

**新建 `ic_tab_touchpad.xml`**：
- 触控板风格的 VectorDrawable 图标
- 与现有图标风格一致

### 3.6 字符串资源

**修改 `strings.xml`**：
```xml
<string name="tab_touchpad">触控板</string>
<string name="touchpad_left_click">左键</string>
<string name="touchpad_right_click">右键</string>
<string name="touchpad_sensitivity">灵敏度</string>
```

## 4. 数据流

```
用户手指滑动 → TouchpadFragment.onTouchEvent()
  → 计算相对移动 (dx, dy) × 灵敏度系数
  → MouseSender.sendMouseMove(dx, dy)
  → hidDevice.sendReport(host, MOUSE_REPORT_ID, mouseReport.bytes)
  → 蓝牙发送到电脑
  → 电脑操作系统移动光标
```

## 5. 代码复用

| 复用项 | 来源 | 用途 |
|--------|------|------|
| `HapticHelper.performKeyClick()` | `util/HapticHelper.kt` | 按钮触觉反馈 |
| `KeyboardFragment` 的启用/禁用模式 | `KeyboardFragment.kt` | 连接状态管理 |
| `MainActivity.switchToClaudeTab()` | `MainActivity.kt` | 返回 Claude 标签 |
| `BluetoothHidService` 连接生命周期 | `BluetoothHidService.kt` | 蓝牙连接管理 |
| `KeyboardSender` 的 `hidDevice`/`host` | `KeyboardSender.kt` | `MouseSender` 共享连接 |

## 6. 文件清单

### 新建文件
- `app/src/main/java/com/haoze/claudekeyboard/bluetooth/MouseReport.kt`
- `app/src/main/java/com/haoze/claudekeyboard/bluetooth/MouseSender.kt`
- `app/src/main/java/com/haoze/claudekeyboard/ui/touchpad/TouchpadFragment.kt`
- `app/src/main/res/layout/fragment_touchpad.xml`
- `app/src/main/res/drawable/ic_tab_touchpad.xml`

### 修改文件
- `app/src/main/java/com/haoze/claudekeyboard/bluetooth/DescriptorCollection.kt` — 添加 COMBINED 描述符
- `app/src/main/java/com/haoze/claudekeyboard/bluetooth/KeyboardReport.kt` — Report ID 改为 1，报告大小改为 9
- `app/src/main/java/com/haoze/claudekeyboard/bluetooth/KeyboardSender.kt` — 使用新的 Report ID
- `app/src/main/java/com/haoze/claudekeyboard/bluetooth/BluetoothHidService.kt` — 使用 COMBINED 描述符，添加 MouseSender
- `app/src/main/java/com/haoze/claudekeyboard/MainActivity.kt` — 添加触控板标签页处理
- `app/src/main/res/layout/activity_main.xml` — 添加 content_touchpad 容器
- `app/src/main/res/menu/bottom_nav_menu.xml` — 添加 nav_touchpad 标签
- `app/src/main/res/values/strings.xml` — 添加触控板相关字符串

## 7. 注意事项

### 7.1 兼容性
- 修改 HID 描述符后，已配对的设备需要重新配对
- 在版本更新说明中提示用户

### 7.2 性能
- 触摸事件处理在主线程，HID 报告发送在后台线程
- 移动事件节流：每 8ms 最多发送一次（~120Hz），避免蓝牙拥塞

### 7.3 双指滚动实现
- 通过 `MotionEvent.pointerCount` 检测双指
- 双指移动的 Y 方向差值作为滚轮值
- 需要区分单指移动和双指滚动的触摸事件
- 实现方式：
  - `ACTION_POINTER_DOWN`（pointerCount == 2）时进入滚动模式
  - `ACTION_POINTER_UP`（pointerCount 从 2 变为 1）时退出滚动模式
  - 滚动模式下，忽略 X 方向移动，只处理 Y 方向作为滚轮值
  - 单指模式下，处理 X/Y 方向移动作为光标移动
