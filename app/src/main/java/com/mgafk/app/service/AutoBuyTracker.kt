package com.mgafk.app.service

internal class AutoBuyTracker {

    private val bought = mutableSetOf<String>()

    fun newlyStocked(
        stockedKeys: List<String>,
        restockedShopTypes: Set<String>,
        isAutoBuyEnabled: (String) -> Boolean,
    ): List<String> {
        if (restockedShopTypes.isNotEmpty()) {
            bought.removeAll { shopTypeOf(it) in restockedShopTypes}
        }

        val newKeys = mutableListOf<String>()
        for (key in stockedKeys) {
            if (key in bought) continue
            if (!isAutoBuyEnabled(key)) continue
            bought.add(key)
            newKeys.add(key)
        }
        bought.retainAll(stockedKeys.toSet())
        return newKeys
    }

    private fun shopTypeOf(key: String): String = key.split(KEY_SEPARATOR).getOrElse(1) { "" }

    companion object {
        private const val KEY_SEPARATOR = ':'

        fun keyOf(shopType: String, itemName: String): String = "shop$KEY_SEPARATOR$shopType$KEY_SEPARATOR$itemName"
    }
}