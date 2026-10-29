package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.voicenav.routing.OpenRouteServiceRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Voice Nav", appName)
    }

    @Test
    fun `test local api key persistence`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = OpenRouteServiceRepository(context)

        repo.saveApiKey("test-ors-api-key-12345")
        assertEquals("test-ors-api-key-12345", repo.getStoredApiKey())
        assertTrue(repo.hasValidApiKey())
    }
}
