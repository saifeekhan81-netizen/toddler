package com.example.security

import java.security.MessageDigest
import java.security.SecureRandom

object ParentAuthManager {
    private const val HASH_ALGORITHM = "SHA-256"

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt.toHex()
    }

    fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance(HASH_ALGORITHM)
        md.update(salt.toByteArray(Charsets.UTF_8))
        val hashedBytes = md.digest(pin.toByteArray(Charsets.UTF_8))
        return hashedBytes.toHex()
    }

    fun verifyPin(enteredPin: String, storedHash: String, storedSalt: String): Boolean {
        if (enteredPin.length != 4 || storedHash.isEmpty() || storedSalt.isEmpty()) return false
        val computedHash = hashPin(enteredPin, storedSalt)
        return MessageDigest.isEqual(
            computedHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    data class RecoveryChallenge(
        val question: String,
        val options: List<Int>,
        val correctAnswer: Int
    )

    fun createRecoveryChallenge(): RecoveryChallenge {
        val num1 = (12..49).random()
        val num2 = (11..39).random()
        val answer = num1 + num2
        val options = mutableSetOf(answer)
        while (options.size < 4) {
            val offset = listOf(-10, -2, -1, 1, 2, 10, 11).random()
            options.add(answer + offset)
        }
        return RecoveryChallenge(
            question = "Parent Verification: $num1 + $num2 = ?",
            options = options.shuffled(),
            correctAnswer = answer
        )
    }
}
