// BatchKit root build file.
//
// AGP 9 compiles Kotlin through its built-in Kotlin support and ships with Kotlin
// Gradle plugin 2.2.10. BatchKit uses Kotlin 2.4.20 and KSP 2.3.12, so both are
// raised on the buildscript classpath here, as documented in the AGP 9 release
// notes ("Upgrade to a higher KGP version"). Subprojects inherit this classpath.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.20")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:2.3.12")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
