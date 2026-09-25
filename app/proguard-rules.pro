# BatchKit ProGuard/R8 rules.
# The app talks to hidden framework APIs and Shizuku through reflection, so the
# reflective entry points and the small number of data holders touched by
# reflection must survive shrinking.

-keep class rikka.shizuku.** { *; }
-keep class moe.shizuku.** { *; }
-keep class org.lsposed.hiddenapibypass.** { *; }

# The privileged protocol is passed through Messengers/Bundles by key, never by type name.

# Room generates implementations that are looked up reflectively.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# Kotlin metadata used by the compiler generated code.
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations

# Framework classes referenced by name during reflection are never obfuscated, but
# they may be absent from the compile classpath of some OEM ROMs.
-dontwarn android.app.**
-dontwarn android.content.**
-dontwarn android.os.**
-dontwarn com.android.internal.**
