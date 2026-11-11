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
            "receipt_title",
            "backup_restore",
            "export_backup",
            "import_backup"
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

    @Test
    fun testSingleKeyFilterDeduplication() {
        val filter = com.example.util.SingleKeyFilter(cooldownMs = 300L)

        // 1. Double batch insertion: user typed '5', but streaming emulator injected "55"
        val out1 = filter.filter("", "55")
        assertEquals("5", out1)

        // 2. Triple batch insertion
        val out2 = filter.filter("10", "10999")
        assertEquals("109", out2)

        // 3. Middle cursor insertion
        val out3 = filter.filter("12", "1552")
        assertEquals("152", out3)

        // 4. Rapid duplicate event of same character within 300ms -> ignored
        val rapidDup = filter.filter("5", "55")
        assertEquals("5", rapidDup)

        // 5. Subsequent different character: user types '0' (length +1) -> instant
        val outDiff = filter.filter("5", "50")
        assertEquals("50", outDiff)

        // 6. User backspaces: length decreases
        val outBack = filter.filter("50", "5")
        assertEquals("5", outBack)
    }
}
