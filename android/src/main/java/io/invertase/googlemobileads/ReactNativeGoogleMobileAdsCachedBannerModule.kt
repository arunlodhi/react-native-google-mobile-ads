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
import com.google.android.gms.ads.*
import com.google.android.gms.ads.admanager.AdManagerAdView
import com.google.android.gms.ads.admanager.AppEventListener
import java.util.concurrent.ConcurrentHashMap

class ReactNativeGoogleMobileAdsCachedBannerModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val cachedBannerAds = ConcurrentHashMap<String, BaseAdView>()
    private val cachedAdInfo = ConcurrentHashMap<String, WritableMap>()

    override fun getName(): String {
        return "RNGoogleMobileAdsCachedBannerModule"
    }

    @ReactMethod
    fun requestCachedBannerAd(config: ReadableMap, promise: Promise) {
        val requestId = config.getString("requestId")
        val unitId = config.getString("unitId")
        val isGAM = config.getBoolean("isGAM")

        if (requestId == null || unitId == null) {
            promise.reject("invalid_config", "requestId and unitId are required")
            return
        }

        // Check if ad already exists
        cachedAdInfo[requestId]?.let { existingInfo ->
            promise.resolve(existingInfo)
            return
        }

        val currentActivity = currentActivity
        if (currentActivity == null) {
            promise.reject("no_activity", "No current activity available")
            return
        }

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
                            val adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, currentActivity)
                            adSizes.add(adSize)
                        }
                    }
                    (adView as AdManagerAdView).setAdSizes(*adSizes.toTypedArray())
                }
            } else if (config.hasKey("size")) {
                val sizeString = config.getString("size")
                if (sizeString != null) {
                    val adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, currentActivity)
                    adView.adSize = adSize
                }
            }

            // Create ad request
            val requestOptions = if (config.hasKey("requestOptions")) {
                config.getMap("requestOptions")
            } else {
                null
            }
            val adRequest = ReactNativeGoogleMobileAdsCommon.buildAdRequest(requestOptions)

            // Set up ad listener
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    val adSize = adView.adSize
                    val width = adSize?.getWidthInPixels(currentActivity) ?: 0
                    val height = adSize?.getHeightInPixels(currentActivity) ?: 0

                    val adInfo = Arguments.createMap().apply {
                        putString("requestId", requestId)
                        putString("unitId", unitId)
                        putBoolean("isLoaded", true)
                        putDouble("width", width.toDouble())
                        putDouble("height", height.toDouble())
                    }

                    cachedAdInfo[requestId] = adInfo
                    promise.resolve(adInfo)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    val adInfo = Arguments.createMap().apply {
                        putString("requestId", requestId)
                        putString("unitId", unitId)
                        putBoolean("isLoaded", false)
                        putDouble("width", 0.0)
                        putDouble("height", 0.0)
                    }

                    cachedAdInfo[requestId] = adInfo
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

    @ReactMethod
    fun getCachedAdInfo(requestId: String, promise: Promise) {
        val adInfo = cachedAdInfo[requestId]
        if (adInfo != null) {
            promise.resolve(adInfo)
        } else {
            promise.reject("not_found", "Cached ad not found")
        }
    }

    @ReactMethod
    fun removeCachedAd(requestId: String, promise: Promise) {
        val adView = cachedBannerAds[requestId]
        adView?.let {
            it.adListener = null
            if (it is AdManagerAdView) {
                it.appEventListener = null
            }
            it.destroy()
        }
        cachedBannerAds.remove(requestId)
        cachedAdInfo.remove(requestId)
        promise.resolve(null)
    }

    @ReactMethod
    fun getAllCachedAdIds(promise: Promise) {
        val requestIds = Arguments.createArray()
        for (requestId in cachedBannerAds.keys) {
            requestIds.pushString(requestId)
        }
        promise.resolve(requestIds)
    }

    @ReactMethod
    fun clearAllCachedAds(promise: Promise) {
        for (adView in cachedBannerAds.values) {
            adView.adListener = null
            if (adView is AdManagerAdView) {
                adView.appEventListener = null
            }
            adView.destroy()
        }
        cachedBannerAds.clear()
        cachedAdInfo.clear()
        promise.resolve(null)
    }

    fun getCachedBannerView(requestId: String): BaseAdView? {
        return cachedBannerAds[requestId]
    }
}
