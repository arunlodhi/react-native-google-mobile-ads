package io.invertase.googlemobileads

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReadableMap

abstract class NativeCachedBannerModuleSpec(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  abstract fun requestCachedBannerAd(config: ReadableMap, promise: Promise)
  abstract fun getCachedAdInfo(requestId: String, promise: Promise)
  abstract fun removeCachedAd(requestId: String, promise: Promise)
  abstract fun getAllCachedAdIds(promise: Promise)
  abstract fun clearAllCachedAds(promise: Promise)
}
