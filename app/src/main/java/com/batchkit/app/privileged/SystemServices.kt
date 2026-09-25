package com.batchkit.app.privileged

/**
 * The AIDL stubs BatchKit talks to, and the names the system publishes those
 * services under.
 *
 * Both halves are needed for the same call: the stub class is what
 * `asInterface()` is called on, the service name is what the binder is looked up
 * by. Keeping them in one place means a call site cannot use a stub that has no
 * service name, which would otherwise fail at runtime with an unrelated-looking
 * message.
 */
internal object SystemServices {

    const val ACTIVITY_MANAGER = "android.app.IActivityManager\$Stub"
    const val PACKAGE_MANAGER = "android.content.pm.IPackageManager\$Stub"
    const val APP_OPS = "com.android.internal.app.IAppOpsService\$Stub"
    const val DEVICE_IDLE = "android.os.IDeviceIdleController\$Stub"
    const val NOTIFICATION_MANAGER = "android.app.INotificationManager\$Stub"

    private val byStub = mapOf(
        ACTIVITY_MANAGER to "activity",
        PACKAGE_MANAGER to "package",
        APP_OPS to "appops",
        DEVICE_IDLE to "deviceidle",
        NOTIFICATION_MANAGER to "notification",
    )

    /** The `ServiceManager` name for a stub, or null when the stub is not mapped. */
    fun nameFor(stubClassName: String): String? = byStub[stubClassName]

    /** Every stub BatchKit uses, for the self-test in the Shizuku screen. */
    fun all(): Map<String, String> = byStub
}
