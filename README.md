# 小橘浏览器 (XiaoJu Browser) - 构建与使用说明

## 项目结构

```
XiaoJuBrowser/
├── app/
│   ├── src/main/
│   │   ├── java/com/xiaoju/browser/
│   │   │   ├── MainActivity.java          # 主界面 + 多标签管理
│   │   │   ├── BookmarkManager.java       # 书签数据库管理
│   │   │   ├── HistoryManager.java        # 历史记录管理
│   │   │   ├── BookmarkActivity.java      # 书签列表页面
│   │   │   ├── BookmarkAdapter.java       # 书签列表适配器
│   │   │   ├── HistoryActivity.java       # 历史记录页面
│   │   │   ├── HistoryAdapter.java        # 历史记录适配器
│   │   │   └── SettingsActivity.java      # 设置页面
│   │   ├── res/
│   │   │   ├── layout/                    # 布局文件
│   │   │   ├── drawable/                  # 矢量图标
│   │   │   ├── menu/                      # 菜单
│   │   │   ├── values/                    # 颜色/字符串/样式
│   │   │   └── xml/                       # FileProvider配置
│   │   └── AndroidManifest.xml
│   ├── build.gradle
│   └── proguard-rules.pro
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## 功能清单

### ✅ 已实现功能

| 功能 | 描述 |
|------|------|
| 网页前进/后退 | 工具栏按钮，自动启用/禁用状态 |
| 页面缩放 | 双指捏合缩放，内置缩放控件已关闭 |
| 刷新/停止 | 加载中显示停止按钮，加载完显示刷新 |
| 进度条 | 橘色进度条显示加载进度 |
| 书签保存 | 点击收藏图标即可保存 |
| 书签列表 | 支持查看、点击打开、长按删除 |
| 历史记录 | 自动保存，最多500条，按时间排序 |
| 历史删除 | 长按单条删除，菜单可清空全部 |
| 地址栏 | 默认百度搜索，输入URL自动识别 |
| URL复制 | 长按地址栏复制当前URL |
| 多标签页 | 横向标签栏，新建/切换/关闭标签 |
| 主页按钮 | 一键回到百度首页 |
| 搜索引擎 | 支持百度/Bing/Google/搜狗/自定义 |
| 桌面版网站 | 菜单切换PC端UA |
| 分享链接 | 调用系统分享功能 |
| 清除数据 | 清除历史记录和缓存 |
| 外部链接 | 支持从其他APP打开链接 |
| 深色状态栏 | 橘色主题状态栏 |

## 如何编译

### 方式一：Android Studio（推荐）

1. 安装 [Android Studio](https://developer.android.com/studio)
2. 打开 `XiaoJuBrowser` 文件夹
3. 等待 Gradle 同步
4. 点击 **Build → Build Bundle(s)/APK(s) → Build APK(s)**
5. APK位置：`app/build/outputs/apk/release/app-release.apk`

### 方式二：命令行

```bash
# 进入项目目录
cd XiaoJuBrowser

# 给权限（Mac/Linux）
chmod +x gradlew

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（需要签名）
./gradlew assembleRelease
```

### 签名APK（发布版）

```bash
# 生成密钥
keytool -genkey -v -keystore xiaoju.keystore \
  -alias xiaoju -keyalg RSA -keysize 2048 -validity 36500

# 在 app/build.gradle 中配置签名：
# signingConfigs {
#     release {
#         storeFile file('../xiaoju.keystore')
#         storePassword 'your_password'
#         keyAlias 'xiaoju'
#         keyPassword 'your_password'
#     }
# }
```

## 技术说明

- **内核**: Android系统WebView（不内嵌Chromium，体积小）
- **最低版本**: Android 5.0 (API 21)
- **目标版本**: Android 14 (API 34)
- **依赖**: 仅 AppCompat + Material，无第三方重型库
- **预估体积**: Debug ~3MB，Release压缩后 ~1.5MB
- **数据库**: SQLite（内置，零依赖）
- **颜色主题**: 橘色 (#FF6B00)

## 扩展说明

如需进一步扩展，可以考虑：
- 夜间模式（暗色主题）
- 广告过滤（hosts拦截）
- 全屏浏览模式
- 下载管理
- 手势导航
