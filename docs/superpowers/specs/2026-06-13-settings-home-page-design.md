# SyncTouch 设置风格首页设计文档

## 概述

将 SyncTouch 的主入口从底部导航栏（BottomNavigationView）替换为类似安卓系统设置的列表首页。用户在首页看到功能列表，点击条目进入对应功能页面，页面内有返回按钮回到首页。

## 目标

- 提供清晰的功能入口列表，替代原有的底部 tab 导航
- 将设备连接功能从 Claude 辅助中独立出来
- 将设置从弹窗迁移为独立页面
- 保持键盘和触控板的横屏体验不变

## 首页条目

### 功能组
| 图标 | 条目名称 | 点击行为 |
|---|---|---|
| ⌨️ | 键盘 | 进入键盘页面（横屏） |
| 🖱️ | 触控板 | 进入触控板页面（横屏） |
| 🤖 | Claude 辅助 | 进入 Claude 辅助页面（竖屏） |

### 系统组
| 图标 | 条目名称 | 点击行为 |
|---|---|---|
| 📡 | 设备连接 | 弹出设备列表 BottomSheet |
| ⚙️ | 设置 | 进入设置页面（竖屏） |

列表使用 RecyclerView + MaterialCardView，每个条目左侧图标 + 中间标题/副标题 + 右侧箭头。每组有分组标题，组间有间距。

## 导航流程

```
App Launch
    |
    v
SplashActivity (权限检查)
    |
    v
MainActivity
    |
    +-- content_home (默认页面，竖屏)
    |     ├── 键盘 ──────→ content_keyboard (横屏)
    |     ├── 触控板 ────→ content_touchpad (横屏)
    |     ├── Claude 辅助 → content_claude (竖屏)
    |     ├── 设备连接 ──→ DeviceListBottomSheetFragment (弹窗)
    |     └── 设置 ──────→ content_settings (竖屏)
    |
    +-- 各功能页面左上角有返回箭头，点击回到 content_home
```

### 关键行为

1. **默认页面**：app 启动后显示 content_home，不再显示 Claude tab
2. **方向切换**：进入键盘/触控板时自动切横屏，返回首页时自动切竖屏
3. **返回键**：在功能页面按返回键 → 回到首页；在首页按返回键 → 退出 app
4. **设备连接**：首页点击"设备连接"弹出 DeviceListBottomSheetFragment，连接状态显示在首页条目的副标题中
5. **设置页面**：从弹窗迁移为独立的 content_settings 页面，包含主题模式和重置宏

## 页面内导航

### 键盘页面
- 顶部 AppBar，左侧返回箭头，标题"键盘"
- 右侧可放"触控板"快捷跳转按钮
- 底部无导航栏

### 触控板页面
- 顶部 AppBar，左侧返回箭头，标题"触控板"
- 右侧可放"键盘"快捷跳转按钮
- 底部无导航栏

### Claude 辅助页面
- 顶部 AppBar，左侧返回箭头，标题"Claude 辅助"
- 保留现有功能：按钮组、文本输入、宏列表
- 移除设备连接相关 UI（设备名称栏、连接/断开按钮）

### 设置页面
- 顶部 AppBar，左侧返回箭头，标题"设置"
- 主题模式选择（跟随系统/白天/黑夜）
- 重置宏为默认值

## 文件变更清单

### 修改的文件
| 文件 | 变更说明 |
|---|---|
| `activity_main.xml` | 移除 BottomNavigationView，新增 content_home（RecyclerView）和 content_settings |
| `MainActivity.kt` | 移除底部导航逻辑，新增首页初始化、条目点击跳转、返回处理、设备状态显示 |
| `content_claude` 布局 | 移除设备连接相关 UI 元素 |
| `strings.xml` | 新增首页相关字符串资源 |

### 新增的文件
| 文件 | 说明 |
|---|---|
| `HomeAdapter.kt` | 首页列表 RecyclerView Adapter，支持分组标题和可点击条目 |
| `HomeItem.kt` | 数据类，包含 icon、title、subtitle、clickAction |
| `content_settings` 布局 | 从 dialog_settings.xml 迁移并扩展为独立页面布局 |

### 不变的文件
- `KeyboardFragment.kt`
- `TouchpadFragment.kt`
- `BluetoothHidService.kt`
- `KeyboardSender.kt` / `MouseSender.kt`

## 设计决策

1. **方案 A（内联布局）** 而非独立 Fragment — 与现有 content_claude 模式一致
2. **首页竖屏，功能页按需横屏** — 保持键盘/触控板的横屏体验
3. **设备连接从 Claude 辅助中独立** — 更符合设置风格的分组逻辑
4. **设置从弹窗改为独立页面** — 更好的可扩展性
