/*
 * Copyright (C) 2026 Scrolless
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.scrolless.app.ui.onboarding

import android.provider.Settings
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The supplied SVGs use CSS keyframes, so they need a browser renderer rather than an image decoder. */
@Composable
internal fun OnboardingAnimation(asset: String, blocking: Boolean, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var webView by remember { mutableStateOf<WebView?>(null) }
    var loaded by remember { mutableStateOf(false) }
    val reducedMotion = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    val html by produceState<String?>(null, asset, blocking, reducedMotion) {
        value = withContext(Dispatchers.IO) {
            val svg = context.assets.open("onboarding/$asset.svg").bufferedReader().use { it.readText() }
            animationDocument(svg, blocking, reducedMotion)
        }
    }

    DisposableEffect(lifecycle, webView) {
        val view = webView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> view?.onResume()
                Lifecycle.Event.ON_PAUSE -> view?.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) view?.onPause()
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier.semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        val document = html
        if (document == null) {
            CircularProgressIndicator()
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageCommitVisible(view: WebView, url: String) {
                                loaded = true
                            }
                        }
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        settings.apply {
                            javaScriptEnabled = false
                            allowFileAccess = false
                            allowContentAccess = false
                            blockNetworkLoads = true
                            setSupportZoom(false)
                        }
                        // The demonstration is decorative; controls and accessibility live in Compose.
                        isFocusable = false
                        isLongClickable = false
                        webView = this
                    }
                },
                update = { view ->
                    if (view.tag != document) {
                        loaded = false
                        view.tag = document
                        view.loadDataWithBaseURL("https://onboarding.scrolless.invalid/", document, "text/html", "UTF-8", null)
                    }
                },
                onRelease = { view ->
                    webView = null
                    view.stopLoading()
                    view.destroy()
                },
            )
            if (!loaded) CircularProgressIndicator()
        }
    }
}

internal fun animationDocument(svg: String, blocking: Boolean, reducedMotion: Boolean): String {
    val allowedStyle = if (blocking) "" else """
        .anim-blocked-badge, .anim-blocked-pulse, .anim-feed-cover { display: none !important; }
        @keyframes onboardingAllowed {
            0%, 24% { opacity: 0; transform: translateY(800px); }
            32%, 85% { opacity: 1; transform: translateY(0); }
            95%, 100% { opacity: 0; transform: translateY(800px); }
        }
        .anim-viewer, .anim-reels, .anim-fullscreen-reel {
            animation: onboardingAllowed 8s ease-in-out infinite !important;
        }
    """
    val motionStyle = if (reducedMotion) "* { animation-delay: -3.2s !important; animation-play-state: paused !important; }" else ""
    return """
        <!doctype html><html><head>
        <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
        <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'">
        <style>html,body{margin:0;width:100%;height:auto;overflow:hidden;background:transparent;user-select:none}
        svg{display:block;width:100%;height:auto}</style></head><body>
        $svg
        <style>$allowedStyle $motionStyle</style>
        </body></html>
    """.trimIndent()
}
