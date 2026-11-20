package com.rachajusto.domain.engine

enum class PixKeyType {
    EMAIL,
    PHONE,
    CPF,
    CNPJ,
    RANDOM_EVP,
    UNKNOWN
}

data class PixKeyValidationResult(
    val normalizedKey: String,
    val type: PixKeyType,
    val isValid: Boolean
)

object PixKeyValidator {

    private val emailRegex = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    private val e164PhoneRegex = Regex("^\\+[1-9]\\d{10,14}$")

    fun inspect(rawKey: String?): PixKeyValidationResult {
        val trimmed = rawKey?.trim().orEmpty()
        if (trimmed.isEmpty()) {
            return PixKeyValidationResult("", PixKeyType.UNKNOWN, false)
        }

        if (emailRegex.matches(trimmed)) {
            return PixKeyValidationResult(trimmed.lowercase(), PixKeyType.EMAIL, true)
        }

        if (uuidRegex.matches(trimmed)) {
            return PixKeyValidationResult(trimmed.lowercase(), PixKeyType.RANDOM_EVP, true)
        }

        if (e164PhoneRegex.matches(trimmed)) {
            return PixKeyValidationResult(trimmed, PixKeyType.PHONE, true)
        }

        val digitsOnly = trimmed.filter { it.isDigit() }
        if (digitsOnly.length == 11) {
            if (isValidCpf(digitsOnly)) {
                return PixKeyValidationResult(digitsOnly, PixKeyType.CPF, true)
            }
            // Brazilian mobile number with 2-digit DDD + 9 + 8 digits
            if (digitsOnly[2] == '9') {
                return PixKeyValidationResult("+55$digitsOnly", PixKeyType.PHONE, true)
            }
        }

        if (digitsOnly.length == 14 && isValidCnpj(digitsOnly)) {
            return PixKeyValidationResult(digitsOnly, PixKeyType.CNPJ, true)
        }

        return PixKeyValidationResult(trimmed, PixKeyType.UNKNOWN, false)
    }

    internal fun isValidCpf(digits: String): Boolean {
        if (digits.length != 11 || digits.all { it == digits[0] }) return false
        val nums = digits.map { it.digitToInt() }
        val firstSum = (0..8).sumOf { nums[it] * (10 - it) }
        val firstCheck = ((firstSum * 10) % 11).let { if (it == 10) 0 else it }
        if (firstCheck != nums[9]) return false

        val secondSum = (0..9).sumOf { nums[it] * (11 - it) }
        val secondCheck = ((secondSum * 10) % 11).let { if (it == 10) 0 else it }
        return secondCheck == nums[10]
    }

    internal fun isValidCnpj(digits: String): Boolean {
        if (digits.length != 14 || digits.all { it == digits[0] }) return false
        val nums = digits.map { it.digitToInt() }
        val weights1 = intArrayOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val weights2 = intArrayOf(6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)

        val rem1 = weights1.indices.sumOf { nums[it] * weights1[it] } % 11
        val check1 = if (rem1 < 2) 0 else 11 - rem1
        if (check1 != nums[12]) return false

        val rem2 = weights2.indices.sumOf { nums[it] * weights2[it] } % 11
        val check2 = if (rem2 < 2) 0 else 11 - rem2
        return check2 == nums[13]
    }
}
