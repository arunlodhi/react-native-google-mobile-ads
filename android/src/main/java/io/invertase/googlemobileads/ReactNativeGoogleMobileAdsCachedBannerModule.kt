package io.invertase.googlemobileads

/*
 * Copyright (c) 2016-present Invertase Limited & Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this library except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

import android.app.Activity
import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.facebook.react.bridge.UiThreadUtil
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.admanager.AppEventListener
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

@ReactModule(name = ReactNativeGoogleMobileAdsCachedBannerModule.NAME)
class ReactNativeGoogleMobileAdsCachedBannerModule(reactContext: ReactApplicationContext) :
    NativeCachedBannerModuleSpec(reactContext) {

    private val cachedBannerAds = ConcurrentHashMap<String, BaseAdView>()
    private val cachedAdInfo = ConcurrentHashMap<String, Map<String, Any>>()

    override fun getName(): String {
        return NAME
    }

    @ReactMethod
    override fun requestCachedBannerAd(config: ReadableMap, promise: Promise) {
        val unitId = config.getString("unitId")
        val isGAM = config.getBoolean("isGAM")

        if (unitId == null) {
            promise.reject("invalid_config", "unitId is required")
            return
        }

        // Always generate a new requestId for each request
        val requestId = generateRequestId()

        val currentActivity = getCurrentActivity()
        if (currentActivity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

        // Ensure all ad operations run on the main UI thread
        UiThreadUtil.runOnUiThread {
            try {
                val adView: BaseAdView = if (isGAM) {
                    AdManagerAdView(currentActivity)
                } else {
                    AdView(currentActivity)
                }

                adView.adUnitId = unitId

                // Initialize adView with initial dimensions if provided
                val maxHeight = if (config.hasKey("maxHeight")) config.getDouble("maxHeight") else 0.0
                val width = if (config.hasKey("width")) config.getDouble("width") else 0.0
                
                android.util.Log.d("CachedBannerModule", "=== INITIAL DIMENSIONS DEBUG ===")
                android.util.Log.d("CachedBannerModule", "MaxHeight from config: $maxHeight")
                android.util.Log.d("CachedBannerModule", "Width from config: $width")
                
                if (maxHeight > 0 || width > 0) {
                    // Convert DP to pixels for layout params
                    val density = currentActivity.resources.displayMetrics.density
                    val widthPx = if (width > 0) (width * density).toInt() else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    val heightPx = if (maxHeight > 0) (maxHeight * density).toInt() else android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                    
                    android.util.Log.d("CachedBannerModule", "Setting initial layout params:")
                    android.util.Log.d("CachedBannerModule", "  - widthPx: $widthPx")
                    android.util.Log.d("CachedBannerModule", "  - heightPx: $heightPx")
                    
                    val layoutParams = android.view.ViewGroup.LayoutParams(widthPx, heightPx)
                    adView.layoutParams = layoutParams
                    
                    // Force measure and layout the view so it has proper dimensions
                    val widthMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY)
                    val heightMeasureSpec = android.view.View.MeasureSpec.makeMeasureSpec(heightPx, android.view.View.MeasureSpec.EXACTLY)
                    adView.measure(widthMeasureSpec, heightMeasureSpec)
                    adView.layout(0, 0, adView.measuredWidth, adView.measuredHeight)
                    
                    android.util.Log.d("CachedBannerModule", "After measure/layout:")
                    android.util.Log.d("CachedBannerModule", "  - adView.width: ${adView.width}")
                    android.util.Log.d("CachedBannerModule", "  - adView.height: ${adView.height}")
                    android.util.Log.d("CachedBannerModule", "  - adView.measuredWidth: ${adView.measuredWidth}")
                    android.util.Log.d("CachedBannerModule", "  - adView.measuredHeight: ${adView.measuredHeight}")
                }

                // Set ad sizes
                android.util.Log.d("CachedBannerModule", "=== SIZE CONFIGURATION DEBUG ===")
                android.util.Log.d("CachedBannerModule", "IsGAM: $isGAM")
                android.util.Log.d("CachedBannerModule", "Config has 'sizes' key: ${config.hasKey("sizes")}")
                android.util.Log.d("CachedBannerModule", "Config has 'size' key: ${config.hasKey("size")}")
                
                if (isGAM && config.hasKey("sizes")) {
                    val sizes = config.getArray("sizes")
                    android.util.Log.d("CachedBannerModule", "Sizes array: $sizes")
                    android.util.Log.d("CachedBannerModule", "Sizes array size: ${sizes?.size()}")
                    
                    if (sizes != null && sizes.size() > 0) {
                        val adSizes = mutableListOf<AdSize>()
                        for (i in 0 until sizes.size()) {
                            val sizeString = sizes.getString(i)
                            android.util.Log.d("CachedBannerModule", "Processing size[$i]: '$sizeString'")
                            
                            if (sizeString != null) {
                                val adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, adView)
                                android.util.Log.d("CachedBannerModule", "  -> Parsed to AdSize: $adSize")
                                android.util.Log.d("CachedBannerModule", "  -> AdSize.width: ${adSize.width}")
                                android.util.Log.d("CachedBannerModule", "  -> AdSize.height: ${adSize.height}")
                                adSizes.add(adSize)
                            }
                        }
                        android.util.Log.d("CachedBannerModule", "Final adSizes list: $adSizes")
                        (adView as AdManagerAdView).setAdSizes(*adSizes.toTypedArray())
                        android.util.Log.d("CachedBannerModule", "Set adSizes on AdManagerAdView")
                    }
                } else if (config.hasKey("size")) {
                    val sizeString = config.getString("size")
                    android.util.Log.d("CachedBannerModule", "Single size string: '$sizeString'")
                    
                    if (sizeString != null) {
                        val adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, adView)
                        android.util.Log.d("CachedBannerModule", "  -> Parsed to AdSize: $adSize")
                        android.util.Log.d("CachedBannerModule", "  -> AdSize.width: ${adSize.width}")
                        android.util.Log.d("CachedBannerModule", "  -> AdSize.height: ${adSize.height}")
                        (adView as AdView).setAdSize(adSize)
                        android.util.Log.d("CachedBannerModule", "Set adSize on AdView")
                    }
                }

                // Create ad request
                val requestOptions = if (config.hasKey("requestOptions")) {
                    config.getMap("requestOptions")
                } else {
                    null
                }
                val adRequest = ReactNativeGoogleMobileAdsCommon.buildAdRequest(requestOptions)

                // Prepare configuration data for matching
                val size = config.getString("size") ?: ""
                val sizesString = if (isGAM && config.hasKey("sizes")) {
                    val sizes = config.getArray("sizes")
                    val sizesList = mutableListOf<String>()
                    if (sizes != null) {
                        for (i in 0 until sizes.size()) {
                            sizes.getString(i)?.let { sizesList.add(it) }
                        }
                    }
                    sizesList.sorted().joinToString(",")
                } else {
                    size
                }

                // Set up ad listener
                adView.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        android.util.Log.d("CachedBannerModule", "=== AD LOADED - SIZE CALCULATION DEBUG ===")
                        android.util.Log.d("CachedBannerModule", "RequestId: $requestId")
                        android.util.Log.d("CachedBannerModule", "UnitId: $unitId")
                        android.util.Log.d("CachedBannerModule", "IsGAM: $isGAM")
                        android.util.Log.d("CachedBannerModule", "SizesString: $sizesString")
                        
                        val adSize = adView.adSize
                        android.util.Log.d("CachedBannerModule", "AdView.adSize: $adSize")
                        android.util.Log.d("CachedBannerModule", "AdView.width: ${adView.width}")
                        android.util.Log.d("CachedBannerModule", "AdView.height: ${adView.height}")
                        
                        val width: Int
                        val height: Int
                        
                        if (adSize != null) {
                            android.util.Log.d("CachedBannerModule", "AdSize details:")
                            android.util.Log.d("CachedBannerModule", "  - AdSize.toString(): $adSize")
                            android.util.Log.d("CachedBannerModule", "  - AdSize.width: ${adSize.width}")
                            android.util.Log.d("CachedBannerModule", "  - AdSize.height: ${adSize.height}")
                            
                            // Use the exact same logic as ReactNativeGoogleMobileAdsBannerAdViewManager
                            val isFluid = adSize == AdSize.FLUID
                            android.util.Log.d("CachedBannerModule", "  - IsFluid: $isFluid")
                            
                            if (isFluid) {
                                // For fluid ads, use the view dimensions and add layout change listener
                                width = adView.width
                                height = adView.height
                                android.util.Log.d("CachedBannerModule", "Using FLUID logic - view dimensions:")
                                android.util.Log.d("CachedBannerModule", "  - width (from adView.width): $width")
                                android.util.Log.d("CachedBannerModule", "  - height (from adView.height): $height")
                                
                                // Add layout change listener for fluid ads (same as regular banner implementation)
                                adView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
                                    val newWidthDp = com.facebook.react.uimanager.PixelUtil.toDIPFromPixel((right - left).toFloat()).toDouble()
                                    val newHeightDp = com.facebook.react.uimanager.PixelUtil.toDIPFromPixel((bottom - top).toFloat()).toDouble()
                                    
                                    android.util.Log.d("CachedBannerModule", "Layout changed for fluid ad:")
                                    android.util.Log.d("CachedBannerModule", "  - newWidthDp: $newWidthDp")
                                    android.util.Log.d("CachedBannerModule", "  - newHeightDp: $newHeightDp")
                                    
                                    // Update cached ad info with new dimensions
                                    val updatedAdInfoData = mapOf(
                                        "requestId" to requestId,
                                        "unitId" to unitId,
                                        "isGAM" to isGAM,
                                        "sizesString" to sizesString,
                                        "isLoaded" to true,
                                        "width" to newWidthDp,
                                        "height" to newHeightDp
                                    )
                                    cachedAdInfo[requestId] = updatedAdInfoData
                                    
                                    // Note: For cached ads, we don't emit size change events directly
                                    // The consuming component should call getCachedAdInfo to get updated dimensions
                                }
                            } else {
                                // For all other ad sizes, use getWidthInPixels and getHeightInPixels
                                width = adSize.getWidthInPixels(currentActivity)
                                height = adSize.getHeightInPixels(currentActivity)
                                android.util.Log.d("CachedBannerModule", "Using NON-FLUID logic - adSize pixels:")
                                android.util.Log.d("CachedBannerModule", "  - width (from getWidthInPixels): $width")
                                android.util.Log.d("CachedBannerModule", "  - height (from getHeightInPixels): $height")
                            }
                        } else {
                            // Fallback if adSize is null
                            width = adView.width
                            height = adView.height
                            android.util.Log.d("CachedBannerModule", "AdSize is NULL - using fallback view dimensions:")
                            android.util.Log.d("CachedBannerModule", "  - width (from adView.width): $width")
                            android.util.Log.d("CachedBannerModule", "  - height (from adView.height): $height")
                        }
                        
                        android.util.Log.d("CachedBannerModule", "Final pixel dimensions:")
                        android.util.Log.d("CachedBannerModule", "  - width (pixels): $width")
                        android.util.Log.d("CachedBannerModule", "  - height (pixels): $height")
                        
                        // Convert pixels to DP using PixelUtil (exact same as regular banner implementation)
                        val widthDp = com.facebook.react.uimanager.PixelUtil.toDIPFromPixel(width.toFloat()).toDouble()
                        val heightDp = com.facebook.react.uimanager.PixelUtil.toDIPFromPixel(height.toFloat()).toDouble()
                        
                        android.util.Log.d("CachedBannerModule", "Converted to DP:")
                        android.util.Log.d("CachedBannerModule", "  - widthDp: $widthDp")
                        android.util.Log.d("CachedBannerModule", "  - heightDp: $heightDp")
                        
                        val adInfoData = mapOf(
                            "requestId" to requestId,
                            "unitId" to unitId,
                            "isGAM" to isGAM,
                            "sizesString" to sizesString,
                            "isLoaded" to true,
                            "width" to widthDp,
                            "height" to heightDp
                        )

                        cachedAdInfo[requestId] = adInfoData
                        
                        val adInfo = Arguments.createMap().apply {
                            putString("requestId", requestId)
                            putString("unitId", unitId)
                            putBoolean("isLoaded", true)
                            putDouble("width", widthDp)
                            putDouble("height", heightDp)
                        }
                        promise.resolve(adInfo)
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        val adInfoData = mapOf(
                            "requestId" to requestId,
                            "unitId" to unitId,
                            "isGAM" to isGAM,
                            "sizesString" to sizesString,
                            "isLoaded" to false,
                            "width" to 0.0,
                            "height" to 0.0
                        )

                        cachedAdInfo[requestId] = adInfoData
                        promise.reject("ad_load_failed", loadAdError.message)
                    }
                }

                // Set up app event listener for GAM ads
                if (isGAM) {
                    (adView as AdManagerAdView).appEventListener = AppEventListener { name, data ->
                        // App events will be handled by the view component
                    }
                }

                // Store the ad view
                cachedBannerAds[requestId] = adView

                // Load the ad
                adView.loadAd(adRequest)

            } catch (e: Exception) {
                promise.reject("ad_creation_failed", e.message)
            }
        }
    }

    @ReactMethod
    override fun getCachedAdInfo(requestId: String, promise: Promise) {
        val adInfoData = cachedAdInfo[requestId]
        if (adInfoData != null) {
            val adInfo = Arguments.createMap().apply {
                putString("requestId", adInfoData["requestId"] as String)
                putString("unitId", adInfoData["unitId"] as String)
                putBoolean("isLoaded", adInfoData["isLoaded"] as Boolean)
                putDouble("width", adInfoData["width"] as Double)
                putDouble("height", adInfoData["height"] as Double)
            }
            promise.resolve(adInfo)
        } else {
            promise.reject("not_found", "Cached ad not found")
        }
    }

    @ReactMethod
    override fun removeCachedAd(requestId: String, promise: Promise) {
        val adView = cachedBannerAds[requestId]
        if (adView != null) {
            // Ensure ad cleanup runs on the main UI thread
            UiThreadUtil.runOnUiThread {
                try {
                    adView.adListener = object : AdListener() {}
                    if (adView is AdManagerAdView) {
                        adView.appEventListener = AppEventListener { _, _ -> }
                    }
                    adView.destroy()
                } catch (e: Exception) {
                    // Ignore cleanup errors
                }
            }
        }
        cachedBannerAds.remove(requestId)
        cachedAdInfo.remove(requestId)
        promise.resolve(null)
    }

    @ReactMethod
    override fun getAllCachedAdIds(promise: Promise) {
        val requestIds = Arguments.createArray()
        for (requestId in cachedBannerAds.keys) {
            requestIds.pushString(requestId)
        }
        promise.resolve(requestIds)
    }

    @ReactMethod
    override fun clearAllCachedAds(promise: Promise) {
        val adViews = cachedBannerAds.values.toList()
        if (adViews.isNotEmpty()) {
            // Ensure ad cleanup runs on the main UI thread
            UiThreadUtil.runOnUiThread {
                for (adView in adViews) {
                    try {
                        adView.adListener = object : AdListener() {}
                        if (adView is AdManagerAdView) {
                            adView.appEventListener = AppEventListener { _, _ -> }
                        }
                        adView.destroy()
                    } catch (e: Exception) {
                        // Ignore cleanup errors
                    }
                }
            }
        }
        cachedBannerAds.clear()
        cachedAdInfo.clear()
        promise.resolve(null)
    }

    fun getCachedBannerView(requestId: String): BaseAdView? {
        return cachedBannerAds[requestId]
    }

    fun createNewViewForCachedAd(requestId: String): BaseAdView? {
        val adInfoData = cachedAdInfo[requestId]
        if (adInfoData == null || !(adInfoData["isLoaded"] as Boolean)) {
            android.util.Log.d("CachedBannerModule", "No cached ad info or ad not loaded for requestId: $requestId")
            return null
        }

        // For now, let's go back to reusing the original ad view but with better parent handling
        // The issue with creating new views is that they don't have the loaded ad content
        val originalAdView = cachedBannerAds[requestId]
        if (originalAdView == null) {
            android.util.Log.e("CachedBannerModule", "Original ad view not found for requestId: $requestId")
            return null
        }

        android.util.Log.d("CachedBannerModule", "Returning original cached ad view for requestId: $requestId")
        return originalAdView
    }

    private fun generateRequestId(): String {
        // Generate a unique UUID for each ad request
        return UUID.randomUUID().toString()
    }

    companion object {
        const val NAME = "RNGoogleMobileAdsCachedBannerModule"
    }
}
