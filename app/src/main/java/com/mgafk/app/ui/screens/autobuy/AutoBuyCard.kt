package com.mgafk.app.ui.screens.autobuy

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mgafk.app.data.model.Session
import com.mgafk.app.data.repository.MgApi
import com.mgafk.app.data.repository.ShopItemBuyState
import com.mgafk.app.data.repository.buyState
import com.mgafk.app.ui.components.AppCard
import com.mgafk.app.ui.components.SpriteImage
import com.mgafk.app.ui.theme.Accent
import com.mgafk.app.ui.theme.SurfaceDark
import com.mgafk.app.ui.theme.TextMuted
import com.mgafk.app.ui.theme.TextPrimary
import com.mgafk.app.ui.theme.TextSecondary
import com.mgafk.app.ui.theme.rarityBorder

private val CORE_SHOP_CATEGORIES = listOf(
    "Seeds" to "seed",
    "Tools" to "tool",
    "Eggs" to "egg",
    "Decors" to "decor",
)

private val CORE_SHOP_KEYS = CORE_SHOP_CATEGORIES.map { it.second }.toSet()

/**
 * Key shape shared with [com.mgafk.app.service.AutoBuyTracker] and
 * [com.mgafk.app.data.model.AppSettings.autoBuyItems]: "shop:<shopType>:<itemId>" - the same
 * format ShopAlertTracker already uses for shop alerts.
 */
private fun autoBuyKey(shopType: String, itemId: String) = "shop:$shopType:$itemId"

/** One collapsible card per shop category. Call inside a Column with spacedBy. */
@Composable
fun AutoBuyCards(
    session: Session,
    apiReady: Boolean,
    autoBuyItems: Set<String>,
    onToggle: (key: String, enabled: Boolean) -> Unit,
) {
    if (!apiReady) {
        AppCard {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Loading game data…", fontSize = 13.sp, color = TextMuted)
            }
        }
        return
    }

    val allCategories = remember(MgApi.isReady) {
        val allEntries = MgApi.getPlants().values + MgApi.getEggs().values +
                MgApi.getItems().values + MgApi.getDecors().values
        val dynamic = allEntries
            .flatMap { it.eligibleShops }
            .distinct()
            .map { it.lowercase() }
            .filter { it !in CORE_SHOP_KEYS }
            .sorted()
            .map { key -> "${key.replaceFirstChar { c -> c.uppercase() }} Shop" to key }
        CORE_SHOP_CATEGORIES + dynamic
    }

    allCategories.forEach { (label, category) ->
        AutoBuyCategoryCard(
            label = label,
            category = category,
            session = session,
            autoBuyItems = autoBuyItems,
            onToggle = onToggle,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutoBuyCategoryCard(
    label: String,
    category: String,
    session: Session,
    autoBuyItems: Set<String>,
    onToggle: (String, Boolean) -> Unit,
) {
    val items = remember(category, MgApi.isReady) {
        when (category) {
            "seed" -> MgApi.getPlants().values.filter { it.eligibleShops.isEmpty() || "Seed" in it.eligibleShops }
            "tool" -> MgApi.getItems().values.toList()
            "egg" -> MgApi.getEggs().values.filter { it.eligibleShops.isEmpty() || "Egg" in it.eligibleShops }
            "decor" -> MgApi.getDecors().values.toList()
            else -> {
                val shopName = category.replaceFirstChar { it.uppercase() }
                (MgApi.getPlants().values + MgApi.getEggs().values +
                        MgApi.getItems().values + MgApi.getDecors().values)
                    .filter { shopName in it.eligibleShops }
            }
        }
    }
    val activeCount = items.count { entry -> autoBuyKey(category, entry.id) in autoBuyItems }

    AppCard(
        title = label,
        collapsible = true,
        persistKey = "autobuy.$category",
        trailing = {
            if (activeCount > 0) {
                Text(
                    text = "$activeCount active",
                    fontSize = 11.sp,
                    color = Accent,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
        },
    ) {
        Text(
            "Tap an item to buy it automatically the moment it's in stock.",
            fontSize = 11.sp,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (items.isEmpty()) {
            Text(if (MgApi.isReady) "No items." else "Loading...", fontSize = 11.sp, color = TextMuted)
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items.forEach { entry ->
                    val key = autoBuyKey(category, entry.id)
                    val enabled = key in autoBuyItems
                    val buyState = remember(entry.id, session.inventory, session.availableStorages) {
                        session.buyState(entry.id)
                    }
                    AutoBuyItemTile(
                        spriteUrl = entry.sprite,
                        name = entry.name,
                        rarity = entry.rarity,
                        enabled = enabled,
                        buyState = buyState,
                        onClick = { onToggle(key, !enabled) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoBuyItemTile(
    spriteUrl: String?,
    name: String,
    rarity: String?,
    enabled: Boolean,
    buyState: ShopItemBuyState,
    onClick: () -> Unit,
) {
    val notBuyable = buyState != ShopItemBuyState.Buyable
    val bgColor = if (enabled) Accent.copy(alpha = 0.1f) else SurfaceDark
    val tileAlpha = if (notBuyable) 0.5f else 1f

    Box(modifier = Modifier.size(76.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .then(
                    if (enabled) Modifier.border(1.5.dp, Accent, RoundedCornerShape(10.dp))
                    else Modifier.rarityBorder(rarity = rarity, width = 1.5.dp, shape = RoundedCornerShape(10.dp), alpha = 0.5f)
                )
                .background(bgColor)
                .clickable { onClick() }
                .alpha(tileAlpha)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SpriteImage(url = spriteUrl, size = 32.dp, contentDescription = name)

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = name,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) TextPrimary else TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 11.sp,
            )
        }

        if (enabled) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Auto-buy on",
                    tint = SurfaceDark,
                    modifier = Modifier.size(12.dp),
                )
            }
        }

        if (notBuyable) {
            val label = when (buyState) {
                ShopItemBuyState.Owned -> "OWNED"
                ShopItemBuyState.MaxReached -> "MAX"
                ShopItemBuyState.Buyable -> ""
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-4).dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 10.sp,
                )
            }
        }
    }
}