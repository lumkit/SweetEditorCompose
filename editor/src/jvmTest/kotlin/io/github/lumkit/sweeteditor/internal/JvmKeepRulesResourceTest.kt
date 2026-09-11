package io.github.lumkit.sweeteditor.internal

import kotlin.test.Test
import kotlin.test.assertNotNull

class JvmKeepRulesResourceTest {
    @Test
    fun r8ConsumerRulesAreOnTheClasspath() {
        assertNotNull(
            JvmKeepRulesResourceTest::class.java.getResource(
                "/META-INF/com.android.tools/r8/sweeteditor-compose.pro",
            ),
        )
    }

    @Test
    fun proguardConsumerRulesAreOnTheClasspath() {
        assertNotNull(
            JvmKeepRulesResourceTest::class.java.getResource(
                "/META-INF/proguard/sweeteditor-compose.pro",
            ),
        )
    }
}
