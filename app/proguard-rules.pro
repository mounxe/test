# 小橘浏览器 ProGuard 规则

# WebView相关
-keep class android.webkit.** { *; }
-keep class com.xiaoju.browser.** { *; }
-keepattributes JavascriptInterface

# AppCompat
-keep class androidx.appcompat.** { *; }

# 数据库
-keep class android.database.** { *; }

# 移除日志
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
