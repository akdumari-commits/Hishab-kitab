package com.example

import com.example.localization.AppLanguage
import com.example.localization.AppStrings
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFinancialBalanceCalculations() {
        val openingBalance = 500.0
        val openingType = "GET"
        var netBalance = if (openingType == "GET") openingBalance else -openingBalance

        // 1. You gave (GAVE) 1200 to customer
        val gaveAmount = 1200.0
        netBalance += gaveAmount
        assertEquals(1700.0, netBalance, 0.001)

        // 2. Customer paid (GOT) 1000
        val gotAmount = 1000.0
        netBalance -= gotAmount
        assertEquals(700.0, netBalance, 0.001)

        // Customer still owes 700 (You will get / लेना है)
        assertTrue(netBalance > 0)
    }

    @Test
    fun testBilingualDictionaryCompleteness() {
        val essentialKeys = listOf(
            "app_name",
            "login_title",
            "gave_money",
            "got_money",
            "you_will_get",
            "you_will_give",
            "dashboard_title",
            "customers_title",
            "reminder_title",
            "receipt_title"
        )

        for (key in essentialKeys) {
            val en = AppStrings.get(key, AppLanguage.ENGLISH)
            val hi = AppStrings.get(key, AppLanguage.HINDI)

            assertNotNull("English key missing: $key", en)
            assertNotNull("Hindi key missing: $key", hi)
            assertNotEquals("Hindi key returned key placeholder: $key", key, hi)
            assertNotEquals("English key returned key placeholder: $key", key, en)
        }
    }
}
