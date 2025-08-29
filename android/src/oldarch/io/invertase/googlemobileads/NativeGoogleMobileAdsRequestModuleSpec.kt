package io.invertase.googlemobileads

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReadableArray

abstract class NativeGoogleMobileAdsRequestModuleSpec(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
  abstract fun requestBannerAd(
    unitId: String,
    size: String,
    maxHeight: Double?,
    width: Double?,
    requestOptions: String?,
    promise: Promise
  )
  
  abstract fun requestGAMBannerAd(
    unitId: String,
    sizes: ReadableArray,
    maxHeight: Double?,
    width: Double?,
    requestOptions: String?,
    manualImpressionsEnabled: Boolean?,
    promise: Promise
  )
  
  abstract fun releaseAd(requestId: String)
  
  abstract fun hasAd(requestId: String): Boolean
}
