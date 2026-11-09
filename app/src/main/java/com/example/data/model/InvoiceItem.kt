package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class InvoiceItem(
    val productId: Long = 0L,
    val name: String,
    val quantity: Double,
    val unit: String = "pc",
    val unitPrice: Double,
    val totalPrice: Double = quantity * unitPrice
) {
    fun toJson(): JSONObject {
        val obj = JSONObject()
        obj.put("productId", productId)
        obj.put("name", name)
        obj.put("quantity", quantity)
        obj.put("unit", unit)
        obj.put("unitPrice", unitPrice)
        obj.put("totalPrice", totalPrice)
        return obj
    }

    companion object {
        fun fromJson(json: JSONObject): InvoiceItem {
            val qty = json.optDouble("quantity", 1.0)
            val price = json.optDouble("unitPrice", 0.0)
            return InvoiceItem(
                productId = json.optLong("productId", 0L),
                name = json.optString("name", "Item"),
                quantity = qty,
                unit = json.optString("unit", "pc"),
                unitPrice = price,
                totalPrice = json.optDouble("totalPrice", qty * price)
            )
        }

        fun listToJson(items: List<InvoiceItem>): String {
            val array = JSONArray()
            items.forEach { array.put(it.toJson()) }
            return array.toString()
        }

        fun listFromJson(jsonStr: String): List<InvoiceItem> {
            if (jsonStr.isBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<InvoiceItem>()
                for (i in 0 until array.length()) {
                    list.add(fromJson(array.getJSONObject(i)))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
