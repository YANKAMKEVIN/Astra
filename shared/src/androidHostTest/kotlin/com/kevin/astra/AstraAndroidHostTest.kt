package com.kevin.astra

import com.kevin.astra.core.navigation.AstraDestination
import com.kevin.astra.core.navigation.AstraNavigator
import kotlin.test.Test
import kotlin.test.assertEquals

class AstraAndroidHostTest {
    @Test
    fun navigatorStateIsAvailableOnAndroid() {
        val navigator = AstraNavigator(AstraDestination.ProjectOverview)

        assertEquals(AstraDestination.ProjectOverview, navigator.currentDestination.value)
    }
}
