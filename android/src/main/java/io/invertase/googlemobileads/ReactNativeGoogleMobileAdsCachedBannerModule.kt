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

        // Check if an ad with the same configuration already exists
        val existingRequestId = findExistingAdForConfig(config)
        if (existingRequestId != null) {
            val existingInfo = cachedAdInfo[existingRequestId]!!
            val adInfo = Arguments.createMap().apply {
                putString("requestId", existingInfo["requestId"] as String)
                putString("unitId", existingInfo["unitId"] as String)
                putBoolean("isLoaded", existingInfo["isLoaded"] as Boolean)
                putDouble("width", existingInfo["width"] as Double)
                putDouble("height", existingInfo["height"] as Double)
            }
            promise.resolve(adInfo)
            return
        }

        // Generate new requestId for this ad configuration
        val requestId = generateRequestId(config)

        val currentActivity = currentActivity
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

                // Set ad sizes
                if (isGAM && config.hasKey("sizes")) {
                    val sizes = config.getArray("sizes")
                    if (sizes != null && sizes.size() > 0) {
                        val adSizes = mutableListOf<AdSize>()
                        for (i in 0 until sizes.size()) {
                            val sizeString = sizes.getString(i)
                            if (sizeString != null) {
                                val adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, adView)
                                adSizes.add(adSize)
                            }
                        }
                        (adView as AdManagerAdView).setAdSizes(*adSizes.toTypedArray())
                    }
                } else if (config.hasKey("size")) {
                    val sizeString = config.getString("size")
                    if (sizeString != null) {
                        val adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, adView)
                        (adView as AdView).setAdSize(adSize)
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
                        val adSize = adView.adSize
                        val width = adSize?.getWidthInPixels(currentActivity) ?: 0
                        val height = adSize?.getHeightInPixels(currentActivity) ?: 0

                        val adInfoData = mapOf(
                            "requestId" to requestId,
                            "unitId" to unitId,
                            "isGAM" to isGAM,
                            "sizesString" to sizesString,
                            "isLoaded" to true,
                            "width" to width.toDouble(),
                            "height" to height.toDouble()
                        )

                        cachedAdInfo[requestId] = adInfoData
                        
                        val adInfo = Arguments.createMap().apply {
                            putString("requestId", requestId)
                            putString("unitId", unitId)
                            putBoolean("isLoaded", true)
                            putDouble("width", width.toDouble())
                            putDouble("height", height.toDouble())
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

    fun getRequestIdForConfig(config: ReadableMap): String? {
        return generateRequestId(config)
    }

    private fun findExistingAdForConfig(config: ReadableMap): String? {
        val unitId = config.getString("unitId") ?: return null
        val isGAM = config.getBoolean("isGAM")
        val size = config.getString("size") ?: ""
        
        // For GAM ads, get sizes array
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

        // Check all cached ads to see if any match this configuration
        for ((requestId, adInfo) in cachedAdInfo) {
            if (adInfo["unitId"] == unitId && 
                adInfo["isGAM"] == isGAM &&
                adInfo["sizesString"] == sizesString) {
                return requestId
            }
        }
        
        return null
    }

    private fun generateRequestId(config: ReadableMap): String {
        // Generate a unique UUID for each ad request
        return UUID.randomUUID().toString()
    }

    companion object {
        const val NAME = "RNGoogleMobileAdsCachedBannerModule"
    }
}
