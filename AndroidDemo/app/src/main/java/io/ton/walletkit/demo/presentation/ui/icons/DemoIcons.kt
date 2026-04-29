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
package io.ton.walletkit.demo.presentation.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

// ── Filled ────────────────────────────────────────────────────────────────

val Icons.Filled.AccountBalanceWallet: ImageVector
    get() = _accountBalanceWallet ?: materialIcon(name = "Filled.AccountBalanceWallet") {
        materialPath {
            moveTo(21.0f, 18.0f)
            verticalLineToRelative(1.0f)
            curveToRelative(0.0f, 1.1f, -0.9f, 2.0f, -2.0f, 2.0f)
            lineTo(5.0f, 21.0f)
            curveToRelative(-1.11f, 0.0f, -2.0f, -0.9f, -2.0f, -2.0f)
            lineTo(3.0f, 5.0f)
            curveToRelative(0.0f, -1.1f, 0.89f, -2.0f, 2.0f, -2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, 0.9f, 2.0f, 2.0f)
            verticalLineToRelative(1.0f)
            horizontalLineToRelative(-9.0f)
            curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(8.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(9.0f)
            close()
            moveTo(12.0f, 16.0f)
            horizontalLineToRelative(10.0f)
            lineTo(22.0f, 8.0f)
            lineTo(12.0f, 8.0f)
            verticalLineToRelative(8.0f)
            close()
            moveTo(16.0f, 13.5f)
            curveToRelative(-0.83f, 0.0f, -1.5f, -0.67f, -1.5f, -1.5f)
            reflectiveCurveToRelative(0.67f, -1.5f, 1.5f, -1.5f)
            reflectiveCurveToRelative(1.5f, 0.67f, 1.5f, 1.5f)
            reflectiveCurveToRelative(-0.67f, 1.5f, -1.5f, 1.5f)
            close()
        }
    }.also { _accountBalanceWallet = it }
private var _accountBalanceWallet: ImageVector? = null

val Icons.Filled.Code: ImageVector
    get() = _code ?: materialIcon(name = "Filled.Code") {
        materialPath {
            moveTo(9.4f, 16.6f)
            lineTo(4.8f, 12.0f)
            lineToRelative(4.6f, -4.6f)
            lineTo(8.0f, 6.0f)
            lineToRelative(-6.0f, 6.0f)
            lineToRelative(6.0f, 6.0f)
            lineToRelative(1.4f, -1.4f)
            close()
            moveTo(14.6f, 16.6f)
            lineToRelative(4.6f, -4.6f)
            lineToRelative(-4.6f, -4.6f)
            lineTo(16.0f, 6.0f)
            lineToRelative(6.0f, 6.0f)
            lineToRelative(-6.0f, 6.0f)
            lineToRelative(-1.4f, -1.4f)
            close()
        }
    }.also { _code = it }
private var _code: ImageVector? = null

val Icons.Filled.ContentCopy: ImageVector
    get() = _contentCopy ?: materialIcon(name = "Filled.ContentCopy") {
        materialPath {
            moveTo(16.0f, 1.0f)
            lineTo(4.0f, 1.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            horizontalLineToRelative(2.0f)
            lineTo(4.0f, 3.0f)
            horizontalLineToRelative(12.0f)
            lineTo(16.0f, 1.0f)
            close()
            moveTo(19.0f, 5.0f)
            lineTo(8.0f, 5.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(11.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 7.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(19.0f, 21.0f)
            lineTo(8.0f, 21.0f)
            lineTo(8.0f, 7.0f)
            horizontalLineToRelative(11.0f)
            verticalLineToRelative(14.0f)
            close()
        }
    }.also { _contentCopy = it }
private var _contentCopy: ImageVector? = null

val Icons.Filled.ContentPaste: ImageVector
    get() = _contentPaste ?: materialIcon(name = "Filled.ContentPaste") {
        materialPath {
            moveTo(19.0f, 2.0f)
            horizontalLineToRelative(-4.18f)
            curveTo(14.4f, 0.84f, 13.3f, 0.0f, 12.0f, 0.0f)
            curveToRelative(-1.3f, 0.0f, -2.4f, 0.84f, -2.82f, 2.0f)
            lineTo(5.0f, 2.0f)
            curveToRelative(-1.1f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(16.0f)
            curveToRelative(0.0f, 1.1f, 0.9f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            lineTo(21.0f, 4.0f)
            curveToRelative(0.0f, -1.1f, -0.9f, -2.0f, -2.0f, -2.0f)
            close()
            moveTo(12.0f, 2.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
            reflectiveCurveToRelative(-0.45f, 1.0f, -1.0f, 1.0f)
            reflectiveCurveToRelative(-1.0f, -0.45f, -1.0f, -1.0f)
            reflectiveCurveToRelative(0.45f, -1.0f, 1.0f, -1.0f)
            close()
            moveTo(19.0f, 20.0f)
            lineTo(5.0f, 20.0f)
            lineTo(5.0f, 4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(3.0f)
            horizontalLineToRelative(10.0f)
            lineTo(17.0f, 4.0f)
            horizontalLineToRelative(2.0f)
            verticalLineToRelative(16.0f)
            close()
        }
    }.also { _contentPaste = it }
private var _contentPaste: ImageVector? = null

val Icons.Filled.DataObject: ImageVector
    get() = _dataObject ?: materialIcon(name = "Filled.DataObject") {
        materialPath {
            moveTo(4.0f, 7.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
            horizontalLineTo(2.0f)
            verticalLineToRelative(4.0f)
            horizontalLineToRelative(1.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 1.65f, 1.35f, 3.0f, 3.0f, 3.0f)
            horizontalLineToRelative(3.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(7.0f)
            curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
            verticalLineToRelative(-2.0f)
            curveToRelative(0.0f, -1.3f, -0.84f, -2.42f, -2.0f, -2.83f)
            verticalLineToRelative(-0.34f)
            curveTo(5.16f, 11.42f, 6.0f, 10.3f, 6.0f, 9.0f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
            horizontalLineToRelative(3.0f)
            verticalLineTo(4.0f)
            horizontalLineTo(7.0f)
            curveTo(5.35f, 4.0f, 4.0f, 5.35f, 4.0f, 7.0f)
            close()
        }
        materialPath {
            moveTo(21.0f, 10.0f)
            curveToRelative(-0.55f, 0.0f, -1.0f, -0.45f, -1.0f, -1.0f)
            verticalLineTo(7.0f)
            curveToRelative(0.0f, -1.65f, -1.35f, -3.0f, -3.0f, -3.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.0f)
            curveToRelative(0.55f, 0.0f, 1.0f, 0.45f, 1.0f, 1.0f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 1.3f, 0.84f, 2.42f, 2.0f, 2.83f)
            verticalLineToRelative(0.34f)
            curveToRelative(-1.16f, 0.41f, -2.0f, 1.52f, -2.0f, 2.83f)
            verticalLineToRelative(2.0f)
            curveToRelative(0.0f, 0.55f, -0.45f, 1.0f, -1.0f, 1.0f)
            horizontalLineToRelative(-3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.0f)
            curveToRelative(1.65f, 0.0f, 3.0f, -1.35f, 3.0f, -3.0f)
            verticalLineToRelative(-2.0f)
            curveToRelative(0.0f, -0.55f, 0.45f, -1.0f, 1.0f, -1.0f)
            horizontalLineToRelative(1.0f)
            verticalLineToRelative(-4.0f)
            horizontalLineTo(21.0f)
            close()
        }
    }.also { _dataObject = it }
private var _dataObject: ImageVector? = null

val Icons.Filled.Language: ImageVector
    get() = _filledLanguage ?: materialIcon(name = "Filled.Language") {
        materialPath {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(18.92f, 8.0f)
            horizontalLineToRelative(-2.95f)
            curveToRelative(-0.32f, -1.25f, -0.78f, -2.45f, -1.38f, -3.56f)
            curveToRelative(1.84f, 0.63f, 3.37f, 1.91f, 4.33f, 3.56f)
            close()
            moveTo(12.0f, 4.04f)
            curveToRelative(0.83f, 1.2f, 1.48f, 2.53f, 1.91f, 3.96f)
            horizontalLineToRelative(-3.82f)
            curveToRelative(0.43f, -1.43f, 1.08f, -2.76f, 1.91f, -3.96f)
            close()
            moveTo(4.26f, 14.0f)
            curveTo(4.1f, 13.36f, 4.0f, 12.69f, 4.0f, 12.0f)
            reflectiveCurveToRelative(0.1f, -1.36f, 0.26f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(-0.08f, 0.66f, -0.14f, 1.32f, -0.14f, 2.0f)
            curveToRelative(0.0f, 0.68f, 0.06f, 1.34f, 0.14f, 2.0f)
            lineTo(4.26f, 14.0f)
            close()
            moveTo(5.08f, 16.0f)
            horizontalLineToRelative(2.95f)
            curveToRelative(0.32f, 1.25f, 0.78f, 2.45f, 1.38f, 3.56f)
            curveToRelative(-1.84f, -0.63f, -3.37f, -1.9f, -4.33f, -3.56f)
            close()
            moveTo(8.03f, 8.0f)
            lineTo(5.08f, 8.0f)
            curveToRelative(0.96f, -1.66f, 2.49f, -2.93f, 4.33f, -3.56f)
            curveTo(8.81f, 5.55f, 8.35f, 6.75f, 8.03f, 8.0f)
            close()
            moveTo(12.0f, 19.96f)
            curveToRelative(-0.83f, -1.2f, -1.48f, -2.53f, -1.91f, -3.96f)
            horizontalLineToRelative(3.82f)
            curveToRelative(-0.43f, 1.43f, -1.08f, 2.76f, -1.91f, 3.96f)
            close()
            moveTo(14.34f, 14.0f)
            lineTo(9.66f, 14.0f)
            curveToRelative(-0.09f, -0.66f, -0.16f, -1.32f, -0.16f, -2.0f)
            curveToRelative(0.0f, -0.68f, 0.07f, -1.35f, 0.16f, -2.0f)
            horizontalLineToRelative(4.68f)
            curveToRelative(0.09f, 0.65f, 0.16f, 1.32f, 0.16f, 2.0f)
            curveToRelative(0.0f, 0.68f, -0.07f, 1.34f, -0.16f, 2.0f)
            close()
            moveTo(14.59f, 19.56f)
            curveToRelative(0.6f, -1.11f, 1.06f, -2.31f, 1.38f, -3.56f)
            horizontalLineToRelative(2.95f)
            curveToRelative(-0.96f, 1.65f, -2.49f, 2.93f, -4.33f, 3.56f)
            close()
            moveTo(16.36f, 14.0f)
            curveToRelative(0.08f, -0.66f, 0.14f, -1.32f, 0.14f, -2.0f)
            curveToRelative(0.0f, -0.68f, -0.06f, -1.34f, -0.14f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(0.16f, 0.64f, 0.26f, 1.31f, 0.26f, 2.0f)
            reflectiveCurveToRelative(-0.1f, 1.36f, -0.26f, 2.0f)
            horizontalLineToRelative(-3.38f)
            close()
        }
    }.also { _filledLanguage = it }
private var _filledLanguage: ImageVector? = null

val Icons.Filled.Link: ImageVector
    get() = _filledLink ?: materialIcon(name = "Filled.Link") {
        materialPath {
            moveTo(3.9f, 12.0f)
            curveToRelative(0.0f, -1.71f, 1.39f, -3.1f, 3.1f, -3.1f)
            horizontalLineToRelative(4.0f)
            lineTo(11.0f, 7.0f)
            lineTo(7.0f, 7.0f)
            curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 5.0f)
            reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-1.9f)
            lineTo(7.0f, 15.1f)
            curveToRelative(-1.71f, 0.0f, -3.1f, -1.39f, -3.1f, -3.1f)
            close()
            moveTo(8.0f, 13.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(-2.0f)
            lineTo(8.0f, 11.0f)
            verticalLineToRelative(2.0f)
            close()
            moveTo(17.0f, 7.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(1.9f)
            horizontalLineToRelative(4.0f)
            curveToRelative(1.71f, 0.0f, 3.1f, 1.39f, 3.1f, 3.1f)
            reflectiveCurveToRelative(-1.39f, 3.1f, -3.1f, 3.1f)
            horizontalLineToRelative(-4.0f)
            lineTo(13.0f, 17.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
            reflectiveCurveToRelative(-2.24f, -5.0f, -5.0f, -5.0f)
            close()
        }
    }.also { _filledLink = it }
private var _filledLink: ImageVector? = null

val Icons.Filled.LinkOff: ImageVector
    get() = _linkOff ?: materialIcon(name = "Filled.LinkOff") {
        materialPath {
            moveTo(17.0f, 7.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(1.9f)
            horizontalLineToRelative(4.0f)
            curveToRelative(1.71f, 0.0f, 3.1f, 1.39f, 3.1f, 3.1f)
            curveToRelative(0.0f, 1.43f, -0.98f, 2.63f, -2.31f, 2.98f)
            lineToRelative(1.46f, 1.46f)
            curveTo(20.88f, 15.61f, 22.0f, 13.95f, 22.0f, 12.0f)
            curveToRelative(0.0f, -2.76f, -2.24f, -5.0f, -5.0f, -5.0f)
            close()
            moveTo(16.0f, 11.0f)
            horizontalLineToRelative(-2.19f)
            lineToRelative(2.0f, 2.0f)
            lineTo(16.0f, 13.0f)
            close()
            moveTo(2.0f, 4.27f)
            lineToRelative(3.11f, 3.11f)
            curveTo(3.29f, 8.12f, 2.0f, 9.91f, 2.0f, 12.0f)
            curveToRelative(0.0f, 2.76f, 2.24f, 5.0f, 5.0f, 5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-1.9f)
            lineTo(7.0f, 15.1f)
            curveToRelative(-1.71f, 0.0f, -3.1f, -1.39f, -3.1f, -3.1f)
            curveToRelative(0.0f, -1.59f, 1.21f, -2.9f, 2.76f, -3.07f)
            lineTo(8.73f, 11.0f)
            lineTo(8.0f, 11.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(2.73f)
            lineTo(13.0f, 15.27f)
            lineTo(13.0f, 17.0f)
            horizontalLineToRelative(1.73f)
            lineToRelative(4.01f, 4.0f)
            lineTo(20.0f, 19.74f)
            lineTo(3.27f, 3.0f)
            lineTo(2.0f, 4.27f)
            close()
        }
    }.also { _linkOff = it }
private var _linkOff: ImageVector? = null

val Icons.Filled.SwapHoriz: ImageVector
    get() = _swapHoriz ?: materialIcon(name = "Filled.SwapHoriz") {
        materialPath {
            moveTo(6.99f, 11.0f)
            lineTo(3.0f, 15.0f)
            lineToRelative(3.99f, 4.0f)
            verticalLineToRelative(-3.0f)
            horizontalLineTo(14.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(6.99f)
            verticalLineToRelative(-3.0f)
            close()
            moveTo(21.0f, 9.0f)
            lineToRelative(-3.99f, -4.0f)
            verticalLineToRelative(3.0f)
            horizontalLineTo(10.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(7.01f)
            verticalLineToRelative(3.0f)
            lineTo(21.0f, 9.0f)
            close()
        }
    }.also { _swapHoriz = it }
private var _swapHoriz: ImageVector? = null

val Icons.Filled.SwapVert: ImageVector
    get() = _swapVert ?: materialIcon(name = "Filled.SwapVert") {
        materialPath {
            moveTo(16.0f, 17.01f)
            verticalLineTo(10.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(7.01f)
            horizontalLineToRelative(-3.0f)
            lineTo(15.0f, 21.0f)
            lineToRelative(4.0f, -3.99f)
            horizontalLineToRelative(-3.0f)
            close()
            moveTo(9.0f, 3.0f)
            lineTo(5.0f, 6.99f)
            horizontalLineToRelative(3.0f)
            verticalLineTo(14.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(6.99f)
            horizontalLineToRelative(3.0f)
            lineTo(9.0f, 3.0f)
            close()
        }
    }.also { _swapVert = it }
private var _swapVert: ImageVector? = null

// ── Outlined ──────────────────────────────────────────────────────────────

val Icons.Outlined.Language: ImageVector
    get() = _outlinedLanguage ?: materialIcon(name = "Outlined.Language") {
        materialPath {
            moveTo(11.99f, 2.0f)
            curveTo(6.47f, 2.0f, 2.0f, 6.48f, 2.0f, 12.0f)
            reflectiveCurveToRelative(4.47f, 10.0f, 9.99f, 10.0f)
            curveTo(17.52f, 22.0f, 22.0f, 17.52f, 22.0f, 12.0f)
            reflectiveCurveTo(17.52f, 2.0f, 11.99f, 2.0f)
            close()
            moveTo(18.92f, 8.0f)
            horizontalLineToRelative(-2.95f)
            curveToRelative(-0.32f, -1.25f, -0.78f, -2.45f, -1.38f, -3.56f)
            curveToRelative(1.84f, 0.63f, 3.37f, 1.91f, 4.33f, 3.56f)
            close()
            moveTo(12.0f, 4.04f)
            curveToRelative(0.83f, 1.2f, 1.48f, 2.53f, 1.91f, 3.96f)
            horizontalLineToRelative(-3.82f)
            curveToRelative(0.43f, -1.43f, 1.08f, -2.76f, 1.91f, -3.96f)
            close()
            moveTo(4.26f, 14.0f)
            curveTo(4.1f, 13.36f, 4.0f, 12.69f, 4.0f, 12.0f)
            reflectiveCurveToRelative(0.1f, -1.36f, 0.26f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(-0.08f, 0.66f, -0.14f, 1.32f, -0.14f, 2.0f)
            reflectiveCurveToRelative(0.06f, 1.34f, 0.14f, 2.0f)
            lineTo(4.26f, 14.0f)
            close()
            moveTo(5.08f, 16.0f)
            horizontalLineToRelative(2.95f)
            curveToRelative(0.32f, 1.25f, 0.78f, 2.45f, 1.38f, 3.56f)
            curveToRelative(-1.84f, -0.63f, -3.37f, -1.9f, -4.33f, -3.56f)
            close()
            moveTo(8.03f, 8.0f)
            lineTo(5.08f, 8.0f)
            curveToRelative(0.96f, -1.66f, 2.49f, -2.93f, 4.33f, -3.56f)
            curveTo(8.81f, 5.55f, 8.35f, 6.75f, 8.03f, 8.0f)
            close()
            moveTo(12.0f, 19.96f)
            curveToRelative(-0.83f, -1.2f, -1.48f, -2.53f, -1.91f, -3.96f)
            horizontalLineToRelative(3.82f)
            curveToRelative(-0.43f, 1.43f, -1.08f, 2.76f, -1.91f, 3.96f)
            close()
            moveTo(14.34f, 14.0f)
            lineTo(9.66f, 14.0f)
            curveToRelative(-0.09f, -0.66f, -0.16f, -1.32f, -0.16f, -2.0f)
            reflectiveCurveToRelative(0.07f, -1.35f, 0.16f, -2.0f)
            horizontalLineToRelative(4.68f)
            curveToRelative(0.09f, 0.65f, 0.16f, 1.32f, 0.16f, 2.0f)
            reflectiveCurveToRelative(-0.07f, 1.34f, -0.16f, 2.0f)
            close()
            moveTo(14.59f, 19.56f)
            curveToRelative(0.6f, -1.11f, 1.06f, -2.31f, 1.38f, -3.56f)
            horizontalLineToRelative(2.95f)
            curveToRelative(-0.96f, 1.65f, -2.49f, 2.93f, -4.33f, 3.56f)
            close()
            moveTo(16.36f, 14.0f)
            curveToRelative(0.08f, -0.66f, 0.14f, -1.32f, 0.14f, -2.0f)
            reflectiveCurveToRelative(-0.06f, -1.34f, -0.14f, -2.0f)
            horizontalLineToRelative(3.38f)
            curveToRelative(0.16f, 0.64f, 0.26f, 1.31f, 0.26f, 2.0f)
            reflectiveCurveToRelative(-0.1f, 1.36f, -0.26f, 2.0f)
            horizontalLineToRelative(-3.38f)
            close()
        }
    }.also { _outlinedLanguage = it }
private var _outlinedLanguage: ImageVector? = null

val Icons.Outlined.Link: ImageVector
    get() = _outlinedLink ?: materialIcon(name = "Outlined.Link") {
        materialPath {
            moveTo(17.0f, 7.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(1.65f, 0.0f, 3.0f, 1.35f, 3.0f, 3.0f)
            reflectiveCurveToRelative(-1.35f, 3.0f, -3.0f, 3.0f)
            horizontalLineToRelative(-4.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(4.0f)
            curveToRelative(2.76f, 0.0f, 5.0f, -2.24f, 5.0f, -5.0f)
            reflectiveCurveToRelative(-2.24f, -5.0f, -5.0f, -5.0f)
            close()
            moveTo(11.0f, 15.0f)
            lineTo(7.0f, 15.0f)
            curveToRelative(-1.65f, 0.0f, -3.0f, -1.35f, -3.0f, -3.0f)
            reflectiveCurveToRelative(1.35f, -3.0f, 3.0f, -3.0f)
            horizontalLineToRelative(4.0f)
            lineTo(11.0f, 7.0f)
            lineTo(7.0f, 7.0f)
            curveToRelative(-2.76f, 0.0f, -5.0f, 2.24f, -5.0f, 5.0f)
            reflectiveCurveToRelative(2.24f, 5.0f, 5.0f, 5.0f)
            horizontalLineToRelative(4.0f)
            verticalLineToRelative(-2.0f)
            close()
            moveTo(8.0f, 11.0f)
            horizontalLineToRelative(8.0f)
            verticalLineToRelative(2.0f)
            lineTo(8.0f, 13.0f)
            close()
        }
    }.also { _outlinedLink = it }
private var _outlinedLink: ImageVector? = null

// ── AutoMirrored.Filled ───────────────────────────────────────────────────

val Icons.AutoMirrored.Filled.CallMade: ImageVector
    get() = _callMade ?: materialIcon(name = "AutoMirrored.Filled.CallMade", autoMirror = true) {
        materialPath {
            moveTo(9.0f, 5.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(6.59f)
            lineTo(4.0f, 18.59f)
            lineTo(5.41f, 20.0f)
            lineTo(17.0f, 8.41f)
            verticalLineTo(15.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(5.0f)
            close()
        }
    }.also { _callMade = it }
private var _callMade: ImageVector? = null

val Icons.AutoMirrored.Filled.CallReceived: ImageVector
    get() = _callReceived ?: materialIcon(name = "AutoMirrored.Filled.CallReceived", autoMirror = true) {
        materialPath {
            moveTo(20.0f, 5.41f)
            lineTo(18.59f, 4.0f)
            lineTo(7.0f, 15.59f)
            verticalLineTo(9.0f)
            horizontalLineTo(5.0f)
            verticalLineToRelative(10.0f)
            horizontalLineToRelative(10.0f)
            verticalLineToRelative(-2.0f)
            horizontalLineTo(8.41f)
            close()
        }
    }.also { _callReceived = it }
private var _callReceived: ImageVector? = null

val Icons.AutoMirrored.Filled.OpenInNew: ImageVector
    get() = _openInNew ?: materialIcon(name = "AutoMirrored.Filled.OpenInNew", autoMirror = true) {
        materialPath {
            moveTo(19.0f, 19.0f)
            horizontalLineTo(5.0f)
            verticalLineTo(5.0f)
            horizontalLineToRelative(7.0f)
            verticalLineTo(3.0f)
            horizontalLineTo(5.0f)
            curveToRelative(-1.11f, 0.0f, -2.0f, 0.9f, -2.0f, 2.0f)
            verticalLineToRelative(14.0f)
            curveToRelative(0.0f, 1.1f, 0.89f, 2.0f, 2.0f, 2.0f)
            horizontalLineToRelative(14.0f)
            curveToRelative(1.1f, 0.0f, 2.0f, -0.9f, 2.0f, -2.0f)
            verticalLineToRelative(-7.0f)
            horizontalLineToRelative(-2.0f)
            verticalLineToRelative(7.0f)
            close()
            moveTo(14.0f, 3.0f)
            verticalLineToRelative(2.0f)
            horizontalLineToRelative(3.59f)
            lineToRelative(-9.83f, 9.83f)
            lineToRelative(1.41f, 1.41f)
            lineTo(19.0f, 6.41f)
            verticalLineTo(10.0f)
            horizontalLineToRelative(2.0f)
            verticalLineTo(3.0f)
            horizontalLineToRelative(-7.0f)
            close()
        }
    }.also { _openInNew = it }
private var _openInNew: ImageVector? = null
