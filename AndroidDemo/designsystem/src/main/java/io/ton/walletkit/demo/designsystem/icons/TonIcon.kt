/*
 * Copyright (c) 2025 TonTech
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.ton.walletkit.demo.designsystem.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp

// Mirrors iOS `Icons/TONIcon.swift` 1-for-1.
//
// Each case is backed by a vector drawable in
// `:designsystem/src/main/res/drawable/<resourceName>.xml`. Resource names are flat
// lowercase to match the conversion the iOS SVGs were dropped through. Two iOS
// names ("Switch", "New") collide with Kotlin/AAPT reserved or near-reserved names
// — those drawables ship as `resource_switch` / `resource_new`.
enum class TonIcon(val resourceName: String, val category: Category) {
    // Tabbar
    DiscoverFilled("discoverfilled", Category.Tabbar),
    DiscoverOutline("discoveroutline", Category.Tabbar),
    HoldingsFilled("holdingsfilled", Category.Tabbar),
    HoldingsOutline("holdingsoutline", Category.Tabbar),
    HomeFilled("homefilled", Category.Tabbar),
    HomeOutline("homeoutline", Category.Tabbar),

    // 24 Standalone
    ArrowDownCircle("arrowdowncircle", Category.Icons24),
    ArrowRightCircle("arrowrightcircle", Category.Icons24),
    ArrowRightUpCircle("arrowrightupcircle", Category.Icons24),
    ArrowUpCircle("arrowupcircle", Category.Icons24),
    Calendar("calendar", Category.Icons24),
    CalendarDays("calendardays", Category.Icons24),
    Chat("chat", Category.Icons24),
    ChevronDown("chevrondown", Category.Icons24),
    ChevronRight("chevronright", Category.Icons24),
    ChevronUp("chevronup", Category.Icons24),
    CircleMinus("circleminus", Category.Icons24),
    CirclePlus("circleplus", Category.Icons24),
    Coin("coin", Category.Icons24),
    Filter("filter", Category.Icons24),
    Gas("gas", Category.Icons24),
    Globus("globus", Category.Icons24),
    HeaderArrowShare("headerarrowshare", Category.Icons24),
    HeaderStar("headerstar", Category.Icons24),
    HeaderStarOutline("headerstaroutline", Category.Icons24),
    Holders("holders", Category.Icons24),
    PlusLarge("pluslarge", Category.Icons24),
    SellIcon("sellicon", Category.Icons24),
    Star24("star24", Category.Icons24),
    SwitchVertical24("switchvertical24", Category.Icons24),
    TonFill("tonfill", Category.Icons24),
    Trend("trend", Category.Icons24),
    Volume("volume", Category.Icons24),
    Volume2("volume2", Category.Icons24),
    Volume3("volume3", Category.Icons24),
    Wallet("wallet", Category.Icons24),
    Wallet4("wallet4", Category.Icons24),

    // 20 Action
    ChangeValue("changevalue", Category.Icons20),
    ChevronBackSmall("chevronbacksmall", Category.Icons20),
    ChevronDownSmall("chevrondownsmall", Category.Icons20),
    ChevronForwardSmall("chevronforwardsmall", Category.Icons20),
    ChevronTopSmall("chevrontopsmall", Category.Icons20),
    Clear("clear", Category.Icons20),
    Close("close", Category.Icons20),
    Copy("copy", Category.Icons20),
    Doc("doc", Category.Icons20),
    Done("done", Category.Icons20),
    Failed("failed", Category.Icons20),
    Github("github", Category.Icons20),
    Globe("globe", Category.Icons20),
    InProgress("inprogress", Category.Icons20),
    Link20("link20", Category.Icons20),
    OpenLink("openlink", Category.Icons20),
    Switch("resource_switch", Category.Icons20),
    Telegram("telegram", Category.Icons20),

    // 16 Badge / Status
    ActiveCheck("activecheck", Category.Icons16),
    ActiveDot("activedot", Category.Icons16),
    HeaderShare("headershare", Category.Icons16),
    Hot("hot", Category.Icons16),
    Info("info", Category.Icons16),
    Loading("loading", Category.Icons16),
    New("resource_new", Category.Icons16),
    NewSparkle("newsparkle", Category.Icons16),
    Padlock("padlock", Category.Icons16),
    PadlockOpen("padlockopen", Category.Icons16),
    Present("present", Category.Icons16),
    Settings("settings", Category.Icons16),
    Star("star", Category.Icons16),
    StarFilled("starfilled", Category.Icons16),
    Switch16("switch16", Category.Icons16),
    Tick("tick", Category.Icons16),
    Trending("trending", Category.Icons16),
    UpArrow("uparrow", Category.Icons16),
    VerifiedBadge("verifiedbadge", Category.Icons16),

    // 12 Micro
    TrendDown("trenddown", Category.Icons12),
    TrendUp("trendup", Category.Icons12),

    // 40 Illustrative
    BankCard40("bankcard40", Category.Icons40),
    BankCard40Alt("bankcard40alt", Category.Icons40),
    Fee40("fee40", Category.Icons40),
    Fee40Alt("fee40alt", Category.Icons40),
    Hand40("hand40", Category.Icons40),
    Holders40("holders40", Category.Icons40),
    Present40("present40", Category.Icons40),
    QrCode40("qrcode40", Category.Icons40),
    Reward40("reward40", Category.Icons40),
    Share40("share40", Category.Icons40),
    Share40Alt("share40alt", Category.Icons40),
    TelegramWallet40("telegramwallet40", Category.Icons40),
    Toncoin40("toncoin40", Category.Icons40),
    ;

    enum class Category { Tabbar, Icons24, Icons20, Icons16, Icons12, Icons40 }
}

// Resolves the drawable backing this icon. Throws if the matching
// `<resourceName>.xml` is missing under `res/drawable/` — matching iOS
// `Image("AssetName")` which crashes when an asset is missing. We'd rather fail
// loudly at first composition than silently ship a screen with blank icons.
@Composable
fun TonIcon.painter(): Painter {
    val context = LocalContext.current
    val resId = remember(resourceName) {
        @Suppress("DiscouragedApi")
        context.resources.getIdentifier(resourceName, "drawable", context.packageName)
    }
    require(resId != 0) {
        "Missing vector drawable for TonIcon.$name — expected res/drawable/$resourceName.xml"
    }
    return painterResource(id = resId)
}

// Iconography shorthand — replaces the iOS `icon.image.resizable().scaledToFit().size(d)
// .foregroundStyle(c)` chain that every component reaches for.
@Composable
fun TonIconImage(
    icon: TonIcon,
    size: Dp,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = null,
) {
    Image(
        painter = icon.painter(),
        contentDescription = contentDescription,
        modifier = modifier.size(size),
        colorFilter = tint?.let { ColorFilter.tint(it) },
    )
}
