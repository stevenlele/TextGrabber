# TextGrabber

[English](#english) | [中文](#中文)

---

## English

### Overview
TextGrabber is an Android application that captures visible screen text using Android's Accessibility Service API. Captures are grouped into projects, stored locally in SQLite, and can be exported as TXT files.

### Features
- 🔍 **Automatic Text Capture**: Monitors screen content changes and extracts text from all visible UI elements
- 🗂️ **Project-based Capture**: Create projects and keep every captured text item grouped by project
- ♾️ **Complete Capture History**: Repeated text is preserved and project history has no item-count cap
- 📄 **TXT Export**: Save the selected project's captured text as a UTF-8 text file
- 💾 **Local Storage**: SQLite database for persistent text storage
- 📊 **Real-time Dashboard**: Live per-project counter and scrollable capture history
- 🌍 **Internationalization**: Supports English and Chinese (auto-detected)
- 🚫 **Self-Exclusion**: Automatically excludes capturing text from TextGrabber itself
- 🎯 **ViewID Tracking**: Captures Android View resource IDs for better context

### Requirements
- Android 7.0 (API 24) or higher
- Accessibility Service permission (requires manual activation in system settings)

### Installation
1. Download the latest APK from [Releases](../../releases)
2. Install the APK on your device
3. Enable the Accessibility Service:
   - Settings → Accessibility → TextGrabber → Toggle ON
4. Open TextGrabber and toggle the switch to start capturing

### Usage
1. **Enable Service**: Turn on the toggle switch in the main interface
2. **Create or select a project**: Captures are saved into the selected project
3. **Navigate to other apps**: The service captures text as you scroll/interact
4. **Browse or export captures**: Review the full scrollable project history or tap **Save TXT**

### Database Inspection
You can inspect the captured data using Android Studio's Database Inspector:
1. Connect your device/emulator
2. Open **App Inspection** → **Database Inspector**
3. Select `com.screen.textgrabber` process
4. View `text_grabber.db` → `captured_text` table

### Building from Source
```bash
# Clone the repository
git clone https://github.com/yourusername/TextGrabber.git
cd TextGrabber

# Build debug APK with Gradle 8.2
gradle assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk
```

You can also run the **Build APK** GitHub Actions workflow manually from the Actions tab. The debug APK is uploaded as the `textgrabber-debug-apk` artifact.

### Architecture
```
MyAccessibilityService (Core)
    ├─ Event Listening (typeAllMask)
    ├─ Node Traversal (Recursive)
    └─ DBManager (SQLite)

MainActivity (UI)
    ├─ Service Toggle
    ├─ Counter Display (Auto-refresh: 1s)
    └─ Recent Texts List (Auto-refresh: 5s)
```

### Privacy & Security
- ⚠️ **Sensitive Permission**: Accessibility Service can read ALL screen content
- 🔒 **Local Only**: All data is stored locally on your device
- 🚫 **No Network**: This app does not transmit any data over the network
- 📝 **Transparency**: All source code is open and auditable

### License
This project is licensed under the MIT License.

---

## 中文

### 项目简介
TextGrabber 是一个 Android 应用程序，使用 Android 无障碍服务 API 捕获屏幕上的可见文本。记录按项目分类并保存在本地 SQLite 数据库中，也可导出为 TXT 文件。

### 主要功能
- 🔍 **自动文本抓取**：监控屏幕内容变化，提取所有可见 UI 元素的文本
- 🗂️ **项目分类**：创建多个项目，将抓取到的文本分别保存
- ♾️ **完整记录**：保留重复文本，不限制每个项目的条目数量
- 📄 **TXT 导出**：将当前项目的文本保存为 UTF-8 文本文件
- 💾 **本地存储**：使用 SQLite 数据库持久化存储文本
- 📊 **实时统计面板**：显示当前项目计数和可滚动的完整记录
- 🌍 **国际化支持**：支持中英文（自动检测系统语言）
- 🚫 **自我排除**：自动排除抓取 TextGrabber 自身的文本
- 🎯 **ViewID 追踪**：捕获 Android 视图资源 ID，提供更好的上下文信息

### 系统要求
- Android 7.0 (API 24) 及以上
- 无障碍服务权限（需在系统设置中手动激活）

### 安装步骤
1. 从 [Releases](../../releases) 下载最新 APK
2. 在设备上安装 APK
3. 启用无障碍服务：
   - 设置 → 无障碍 → TextGrabber → 开启
4. 打开 TextGrabber，点击开关开始抓取

### 使用说明
1. **启用服务**：在主界面打开开关
2. **创建或选择项目**：抓取内容会保存到当前选中的项目
3. **浏览其他应用**：服务会自动在你滚动/交互时抓取文本
4. **查看或导出记录**：滚动查看项目中的全部内容，或点击“保存 TXT”

### 数据库查看
可以使用 Android Studio 的 Database Inspector 查看捕获的数据：
1. 连接设备/模拟器
2. 打开 **App Inspection** → **Database Inspector**
3. 选择 `com.screen.textgrabber` 进程
4. 查看 `text_grabber.db` → `captured_text` 表

### 从源码构建
```bash
# 克隆仓库
git clone https://github.com/yourusername/TextGrabber.git
cd TextGrabber

# 使用 Gradle 8.2 构建 debug APK
gradle assembleDebug

# 输出位置: app/build/outputs/apk/debug/app-debug.apk
```

也可以在 GitHub Actions 页面手动运行 **Build APK** 工作流，构建的 APK 会作为 `textgrabber-debug-apk` artifact 上传。

### 架构设计
```
MyAccessibilityService (核心)
    ├─ 事件监听 (typeAllMask)
    ├─ 节点遍历 (递归)
    └─ DBManager (SQLite)

MainActivity (UI)
    ├─ 服务开关
    ├─ 计数器显示 (自动刷新: 1秒)
    └─ 最近文本列表 (自动刷新: 5秒)
```

### 隐私与安全
- ⚠️ **敏感权限**：无障碍服务可以读取屏幕上的所有内容
- 🔒 **仅本地存储**：所有数据仅存储在您的设备本地
- 🚫 **无网络传输**：本应用不会通过网络传输任何数据
- 📝 **透明开源**：所有源代码公开可审计

### 许可证
本项目使用 MIT 许可证。
