package com.kevin.astra

import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.navigation.AstraNavigator
import kotlin.test.Test
import kotlin.test.assertEquals

class AstraIosTest {
    @Test
    fun navigatorStateIsAvailableOnIos() {
        val navigator = AstraNavigator(AstraDestination.Dashboard)

        assertEquals(AstraDestination.Dashboard, navigator.currentDestination.value)
    }
}
