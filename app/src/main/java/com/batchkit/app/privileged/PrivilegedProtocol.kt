package com.batchkit.app.privileged

/** Wire format between the UI process and the privileged executor process. */
internal object PrivilegedProtocol {

    const val MSG_EXECUTE = 1
    const val MSG_RESULT = 2
    const val MSG_DIAGNOSE = 3
    const val MSG_DIAGNOSE_RESULT = 4

    const val KEY_REQUEST_ID = "request_id"
    const val KEY_ACTION_ID = "action_id"
    const val KEY_PACKAGE = "package_name"
    const val KEY_LABEL = "label"
    const val KEY_UID = "uid"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_SYSTEM = "is_system"
    const val KEY_IS_ADMIN = "is_admin"

    const val KEY_SUCCESS = "success"
    const val KEY_REASON = "reason"
    const val KEY_DETAIL = "detail"
    const val KEY_DIAGNOSTICS = "diagnostics"
}
