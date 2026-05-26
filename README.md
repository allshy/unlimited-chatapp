# 无限制 Chat · Android

[![Build](https://github.com/allshy/unlimited-chatapp/actions/workflows/android.yml/badge.svg)](https://github.com/allshy/unlimited-chatapp/actions/workflows/android.yml)

基于开源项目 [unlimited-ai](https://github.com/MallocPointer/unlimited-ai) 重写的安卓原生版本。
**只需要填入 API Key 即可开始聊天**，不需要部署 Cloudflare Worker。

> APK 直接从 [Actions](https://github.com/allshy/unlimited-chatapp/actions) 的最新 run 里下载（`app-debug` artifact）。

## 特性

- ✅ Kotlin + Jetpack Compose + Material 3，支持深色/动态取色
- ✅ 首次启动只问一件事：**API Key**
- ✅ 多会话列表（原版网页只有一个滚动会话）
- ✅ 流式输出，可随时 **停止**
- ✅ **重新生成** 最后一条回复
- ✅ 消息长按可 **复制 / 删除**
- ✅ AI 回复支持 **Markdown + 代码块** 渲染
- ✅ 每个对话独立的 **system prompt** 与模型（不再是全局一份）
- ✅ API Key 用 `EncryptedSharedPreferences` 加密保存，不上传
- ✅ Base URL 可自定义 —— OpenAI / DeepSeek / OpenRouter / NVIDIA NIM 等任意兼容接口都能用

## 默认接口

默认指向 **NVIDIA NIM**：`https://integrate.api.nvidia.com/v1`
（与原项目同源；NVIDIA 开发者账户可领免费额度）

切换示例：
| 平台 | Base URL |
| --- | --- |
| OpenAI 官方 | `https://api.openai.com/v1` |
| DeepSeek 官方 | `https://api.deepseek.com/v1` |
| OpenRouter | `https://openrouter.ai/api/v1` |
| Together | `https://api.together.xyz/v1` |
| 任意 OpenAI 兼容 | `https://your-host/v1` |

## 怎么构建

### 方式 A：Android Studio（推荐）
1. 打开 Android Studio (Hedgehog / Iguana 以上)
2. `File → Open` 选择 `android-app/` 目录
3. 等 Gradle Sync 完成（首次会自动下载 wrapper JAR 与依赖）
4. 接上手机或开模拟器 → Run

### 方式 B：命令行
```bash
cd android-app
# 首次需要本地装一份 Gradle (>= 8.10) 生成 wrapper：
gradle wrapper --gradle-version 8.10.2
./gradlew assembleDebug
# APK 路径：app/build/outputs/apk/debug/app-debug.apk
```

## 第一次使用

1. 装好打开 App → 自动进入 **欢迎/配置** 页
2. 粘贴 API Key
3. 如果不用 NVIDIA，把 Base URL 改成你想用的厂商
4. 点 **开始聊天**
5. 右下角 **+** 新建对话；每个对话顶部 `⚙️` 可单独设置模型与人设

## 改了原项目哪些点

| 原项目问题 | 这里怎么改 |
| --- | --- |
| 必须部署 Cloudflare Worker 才能用 | 客户端直连，不需要服务端 |
| 网页里硬编码 `123456` 密码 | 端上加密保存 API Key 即可 |
| 全局只有一个会话，刷新易丢 | 多会话列表，本地 JSON 持久化 |
| jailbreak 人设硬塞进 Worker，普通用户改不了 | 改成 **每个对话** 可选预设/自定义 system prompt |
| AI 输出纯文本，没有 markdown/代码块 | 内置轻量 markdown 渲染 + 代码块样式 |
| 流式响应无法中断 | 顶部 `■` 按钮立即停止 |
| 没有"重新生成" | 每个对话末尾一键 regen |
| 没有"复制/删除"消息 | 长按消息菜单 |
| `localStorage` 明文存 Key | `EncryptedSharedPreferences`（AES-256） |
| 用 NVIDIA 模型名都是假的（`deepseek-v4-pro` 等） | 默认列表替换为 NVIDIA 实际可用的模型 ID |
| 一个 `app.js` 412 行什么都干 | 分 data / network / ui 三层 |

## 文件结构

```
android-app/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/wuxianzhi/chat/
│   │   ├── App.kt              # Application + 手动 DI
│   │   ├── MainActivity.kt     # 入口 + 导航
│   │   ├── data/
│   │   │   ├── Models.kt           # 数据类、默认模型/人设
│   │   │   ├── SettingsStore.kt    # 加密配置
│   │   │   └── ConversationStore.kt# 对话持久化
│   │   ├── network/
│   │   │   └── ChatApi.kt          # OpenAI 兼容 SSE 客户端
│   │   └── ui/
│   │       ├── theme/Theme.kt
│   │       ├── component/Markdown.kt
│   │       └── screen/
│   │           ├── ConversationListScreen.kt
│   │           ├── ChatScreen.kt
│   │           ├── ChatViewModel.kt
│   │           └── SettingsScreen.kt
│   └── res/...
├── build.gradle.kts
├── settings.gradle.kts
└── app/build.gradle.kts
```

## 注意

- 项目里 **不再** 携带原项目里的 jailbreak 系统提示词。如果你需要某种特定人设，在对话设置里自己填或者用预设里的"通用助手 / 代码搭子 / 翻译 / 写作伙伴"。
- minSdk = 26（Android 8.0+），targetSdk = 35。
- App Key 与对话内容仅存于本机；卸载 App 即删除。
