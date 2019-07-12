package com.adamratzman.octagon.web.api

import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.security.spec.InvalidKeySpecException
import java.util.*
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


/**
 * A utility class to hash passwords and check passwords vs hashed values. It uses a combination of hashing and unique
 * salt. The algorithm used is PBKDF2WithHmacSHA1 which, although not the best for hashing password (vs. bcrypt) is
 * still considered robust and [ recommended by NIST ](https://security.stackexchange.com/a/6415/12614).
 * The hashed value has 256 bits.
 */
private val random = SecureRandom()
private val iterations = 10000
private val keyLength = 256

/**
 * Returns a random salt to be used to hash a password.
 *
 * @return a 16 bytes random salt
 */
val nextSalt: ByteArray
    get() {
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

/**
 * Returns a salted and hashed password using the provided hash.<br></br>
 * Note - side effect: the password is destroyed (the char[] is filled with zeros)
 *
 * @param password the password to be hashed
 * @param salt     a 16 bytes salt, ideally obtained with the getNextSalt method
 *
 * @return the hashed password with a pinch of salt
 */
fun hash(password: CharArray, salt: ByteArray): ByteArray {
    val spec = PBEKeySpec(password, salt, iterations, keyLength)
    Arrays.fill(password, Character.MIN_VALUE)
    try {
        val skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
        return skf.generateSecret(spec).encoded
    } catch (e: NoSuchAlgorithmException) {
        throw AssertionError("Error while hashing a password: " + e.message, e)
    } catch (e: InvalidKeySpecException) {
        throw AssertionError("Error while hashing a password: " + e.message, e)
    } finally {
        spec.clearPassword()
    }
}

/**
 * Returns true if the given password and salt match the hashed value, false otherwise.<br></br>
 * Note - side effect: the password is destroyed (the char[] is filled with zeros)
 *
 * @param password     the password to check
 * @param salt         the salt used to hash the password
 * @param expectedHash the expected hashed value of the password
 *
 * @return true if the given password and salt match the hashed value, false otherwise
 */
fun isExpectedPassword(password: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean {
    val pwdHash = hash(password, salt)
    Arrays.fill(password, Character.MIN_VALUE)
    if (pwdHash.size != expectedHash.size) return false
    for (i in pwdHash.indices) {
        if (pwdHash[i] != expectedHash[i]) return false
    }
    return true
}

class Password(val salt: ByteArray, val expectedHash: ByteArray)