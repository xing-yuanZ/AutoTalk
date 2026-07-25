# AutoTalk ProGuard 规则（默认未开启混淆）
# 如启用 isMinifyEnabled=true，保留 Room 生成的实现与反射相关类。

-keep class com.autotalk.app.data.db.** { *; }
-keep class com.autotalk.app.domain.** { *; }
