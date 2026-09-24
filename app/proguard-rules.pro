# Shizuku rules
-keep class rikka.shizuku.** { *; }
-dontwarn rikka.shizuku.**

# Room rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# HiddenApiBypass rules
-keep class org.lsposed.hiddenapibypass.** { *; }
