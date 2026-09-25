package com.batchkit.app.core.safety

import com.batchkit.app.core.model.BatchAction

/** Reason a package is on the protected list. */
enum class ProtectedReason {
    SELF,
    SHIZUKU,
    LAUNCHER,
    INPUT_METHOD,
    SYSTEM_CRITICAL,
    DEVICE_ADMIN,
    PERSISTENT,
}

/** Outcome of evaluating one action against one app. */
sealed interface SafetyDecision {
    /** Action may run. */
    data object Allowed : SafetyDecision

    /** Action may run, but the app is special and worth warning about. */
    data class Warn(val reason: ProtectedReason) : SafetyDecision

    /** Action must never run on this app. */
    data class Blocked(val reason: ProtectedReason) : SafetyDecision
}

/**
 * Decides whether an action is allowed for a package.
 *
 * This class is deliberately free of Android dependencies so that the safety
 * rules can be unit tested: the device specific knowledge (launcher, input
 * methods, device admins...) is resolved by
 * [ProtectedPackageResolver] and handed in as plain sets.
 */
class SafetyPolicy(
    protectedPackages: Map<String, ProtectedReason> = emptyMap(),
    deviceAdminPackages: Collection<String> = emptyList(),
) {
    private val protected: Map<String, ProtectedReason> = protectedPackages.toMap()
    private val admins: Set<String> = deviceAdminPackages.toSet()

    val protectedPackageNames: Set<String> get() = protected.keys
    val deviceAdminPackageNames: Set<String> get() = admins

    fun protectedReasonOf(packageName: String): ProtectedReason? = protected[packageName]

    fun isProtected(packageName: String): Boolean = protected.containsKey(packageName)

    fun isDeviceAdmin(packageName: String): Boolean = admins.contains(packageName)

    /** True when [action] can never be applied to [packageName]. */
    fun isBlocked(action: BatchAction, packageName: String): Boolean =
        evaluate(action, packageName) is SafetyDecision.Blocked

    fun evaluate(action: BatchAction, packageName: String): SafetyDecision {
        val protection = protected[packageName]
        if (protection != null) {
            return when {
                action.blockedForProtectedPackages -> SafetyDecision.Blocked(protection)
                action.blockedForDeviceAdmins && admins.contains(packageName) ->
                    SafetyDecision.Blocked(ProtectedReason.DEVICE_ADMIN)

                else -> SafetyDecision.Warn(protection)
            }
        }
        if (admins.contains(packageName) && action.blockedForDeviceAdmins) {
            return SafetyDecision.Blocked(ProtectedReason.DEVICE_ADMIN)
        }
        return SafetyDecision.Allowed
    }

    /** Splits a selection into apps that will run and apps that are blocked. */
    fun partition(
        action: BatchAction,
        packages: Collection<String>,
    ): Pair<List<String>, List<Pair<String, ProtectedReason>>> {
        val allowed = mutableListOf<String>()
        val blocked = mutableListOf<Pair<String, ProtectedReason>>()
        packages.forEach { packageName ->
            when (val decision = evaluate(action, packageName)) {
                is SafetyDecision.Allowed, is SafetyDecision.Warn -> allowed += packageName
                is SafetyDecision.Blocked -> blocked += packageName to decision.reason
            }
        }
        return allowed to blocked
    }
}
