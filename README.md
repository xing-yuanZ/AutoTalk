# AutoTalk Android

实时对话助手 · 安卓版。提前告诉它即将进行的对话（人物、内容、目的），对话开始后监听对方说话并实时生成回复建议；还能学习你的说话风格，让建议更贴近你。

> 配套 iOS 版位于 `../AutoTalk`，二者共享相同的产品设计与 AI 提示词模板。

## 目录结构

```
AutoTalkAndroid/
├── app/
│   ├── build.gradle.kts              # 应用模块构建配置
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/autotalk/app/
│       │   ├── AutoTalkApp.kt        # Application 入口
│       │   ├── AppContainer.kt       # 依赖容器
│       │   ├── MainActivity.kt       # Activity + Compose 入口
│       │   ├── domain/               # 领域模型
│       │   ├── data/                 # Room + DataStore
│       │   ├── service/              # AI / 语音识别 / TTS / 引擎
│       │   └── ui/
│       │       ├── theme/            # Material3 主题
│       │       ├── components/       # SuggestionCard / TranscriptBubble
│       │       ├── screens/          # 6 个 Screen
│       │       ├── navigation/       # AppNavigation
│       │       └── viewmodels/       # ViewModel + 工厂
│       └── res/                      # 资源
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties
└── .github/workflows/build-apk.yml   # 云端编译
```

## 技术栈

- Kotlin 2.0.21 + Jetpack Compose（BOM 2024.09.02）
- Material 3
- Room 2.6.1（数据持久化）
- DataStore Preferences（设置存储）
- Navigation Compose
- Android SpeechRecognizer（语音识别）
- TextToSpeech（语音合成）
- compileSdk 34 / minSdk 24 / targetSdk 34
- AGP 8.5.2 / Gradle 8.9

## 编译方式

### 方式 A：GitHub Actions 云端编译（推荐，无需本机环境）

适合没有 Android Studio / JDK / Android SDK 的环境。

1. 在 GitHub 创建一个仓库（如 `AutoTalk`）。
2. 把整个 `AutoTalkAndroid` 目录的内容推上去：
   ```bash
   cd AutoTalkAndroid
   git init
   git add .
   git commit -m "init AutoTalk android"
   git branch -M main
   git remote add origin https://github.com/<你的用户名>/AutoTalk.git
   git push -u origin main
   ```
3. 推送后，仓库的 **Actions** 标签页会自动触发 `Build Android APK` 工作流。
4. 等待约 5–8 分钟，进入本次运行 → 滚动到底部 **Artifacts** → 下载 `AutoTalk-debug-apk`。
5. 解压得到 `app-debug.apk`，传到安卓手机安装即可。

> 也可以在 Actions 页面手动点 **Run workflow** 触发编译。

### 方式 B：本地 Android Studio 编译

适合需要调试、改代码的场景。

1. 安装 [Android Studio](https://developer.android.com/studio)（Ladybug 或更新版本自带 JDK 17 和 Android SDK）。
2. 打开 Android Studio → **Open** → 选择 `AutoTalkAndroid` 目录。
3. 首次打开会自动下载 Gradle、依赖和生成 Gradle Wrapper，等待同步完成。
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**。
5. 完成后点通知里的 **locate** 找到 APK：
   `app/build/outputs/apk/debug/app-debug.apk`

## 安装到手机

Debug APK 用的是 Android 默认 debug 证书，可直接安装测试：

1. 把 `app-debug.apk` 传到手机（微信/QQ/邮件/USB 均可）。
2. 手机 **设置 → 安全 → 允许安装未知来源应用**（不同机型路径略有差异）。
3. 用文件管理器打开 APK 安装。
4. 首次启动会请求 **麦克风** 权限（用于监听对方说话）和 **网络** 权限（调用云端 AI），请允许。

## 使用流程

1. **引导页**：选择 AI 后端（端侧 / 云端），如选云端填写 Base URL / 模型 / API Key（兼容 OpenAI 接口）。
2. **新建对话**：填写标题、内容、目的、语言、参与者。
3. **实时会话**：点「开始监听」，对方说话后自动转录并生成建议；可朗读 / 采纳 / 删除建议，也可手动输入对方发言。
4. **风格教练**：和 Agent 闲聊，让它学习你的说话方式；点右上角「更新画像」抽取风格画像。
5. **设置**：随时切换后端、识别模式、自动播报、界面语言，清除数据等。

## 上架 Google Play

### 1. 生成 Release 签名 APK

Debug 证书不能上架，必须用自有 keystore 签名：

```bash
keytool -genkey -v -keystore autotalk.jks -keyalg RSA -keysize 2048 -validity 10000 -alias autotalk
```

把 `autotalk.jks` 放到项目根目录，新增 `keystore.properties`（**勿提交到 git**，已在 .gitignore 排除）：

```properties
storeFile=autotalk.jks
storePassword=你的存储密码
keyAlias=autotalk
keyPassword=你的key密码
```

在 `app/build.gradle.kts` 的 `android { }` 内追加签名配置：

```kotlin
val keystoreProperties = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}
signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties.getProperty("storeFile") ?: "")
        storePassword = keystoreProperties.getProperty("storePassword") ?: ""
        keyAlias = keystoreProperties.getProperty("keyAlias") ?: ""
        keyPassword = keystoreProperties.getProperty("keyPassword") ?: ""
    }
}
buildTypes {
    release {
        isMinifyEnabled = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

然后：
```bash
./gradlew assembleRelease
```
产物在 `app/build/outputs/apk/release/app-release.apk`。

### 2. 注册 Google Play 开发者账号

- 访问 https://play.google.com/console
- 一次性支付 **$25** 注册费
- 完成开发者身份验证

### 3. 创建应用并上传

1. 控制台 → **创建应用**，填写名称 AutoTalk、默认语言等。
2. **测试 → 内部测试**（建议先内部测试，再正式发布）：上传 `app-release.apk`。
3. 填写商品详情、隐私政策、内容分级、目标受众。
4. 隐私政策需自备一个 URL（可放在 GitHub Pages），说明麦克风录音、网络传输用途。
5. 提交审核，通常 1–3 个工作日通过。

### 4. 持续迭代

每次发版：
1. 修改 `app/build.gradle.kts` 里的 `versionCode`（递增整数）和 `versionName`。
2. 重新 `assembleRelease`。
3. 在 Play Console 新建版本，上传 APK / AAB，发布。

> Google Play 现在推荐上传 **AAB**（Android App Bundle）而非 APK。若要出 AAB，把 `assembleRelease` 换成 `bundleRelease`，产物在 `app/build/outputs/bundle/release/app-release.aab`。

## 常见问题

**Q：端侧模型为什么用不了？**
A：Android 端 Gemini Nano 需要特定机型且接入复杂，当前默认回退到云端 API。若需端侧，可自行接入 AICore / Google ML Kit。

**Q：语音识别经常停？**
A：Android `SpeechRecognizer` 是单次识别，引擎已实现自动重启；若频繁停止，检查是否开启了省电模式或后台限制。

**Q：API Key 安全吗？**
A：Debug APK 直接内置 Key，仅用于自测。正式上架前应改为「让用户在 App 内自己填 Key」或自建中转服务器代理请求。

**Q：和 iOS 版功能一样吗？**
A：核心功能对齐：会话管理、实时监听、建议生成、风格学习、设置。端侧模型能力因平台而异。
