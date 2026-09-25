package com.batchkit.app.privileged

/**
 * Maps the AIDL stub classes BatchKit talks to onto the names the system uses to
 * publish those services. The name is what [rikka.shizuku.SystemServiceHelper]
 * needs to look the service binder up from `ServiceManager`.
 */
internal object SystemServices {

    private val byStub = mapOf(
        "android.app.IActivityManager\$Stub" to "activity",
        "android.content.pm.IPackageManager\$Stub" to "package",
        "com.android.internal.app.IAppOpsService\$Stub" to "appops",
        "android.os.IDeviceIdleController\$Stub" to "deviceidle",
        "android.app.INotificationManager\$Stub" to "notification",
    )

    fun nameFor(stubClassName: String): String? = byStub[stubClassName]

    /** Every stub BatchKit uses, for the self-test in the Shizuku screen. */
    fun all(): Map<String, String> = byStub
}
