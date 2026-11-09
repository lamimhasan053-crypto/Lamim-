package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.InvoiceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HishabKhata", appName)
    }

    @Test
    fun `invoice item serialization test`() {
        val item = InvoiceItem(
            productId = 101L,
            name = "Miniket Rice",
            quantity = 2.0,
            unit = "kg",
            unitPrice = 75.0,
            totalPrice = 150.0
        )
        val json = item.toJson()
        val restored = InvoiceItem.fromJson(json)

        assertEquals("Miniket Rice", restored.name)
        assertEquals(2.0, restored.quantity, 0.01)
        assertEquals(150.0, restored.totalPrice, 0.01)
    }
}
