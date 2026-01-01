package com.myphotocloud.app.utils

import java.security.MessageDigest

object UserAuthUtils {
    /**
     * SHA-256을 사용하여 비밀번호 해싱
     */
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }
}
