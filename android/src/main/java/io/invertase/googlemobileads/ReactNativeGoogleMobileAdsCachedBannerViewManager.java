package io.invertase.googlemobileads;

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

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.PixelUtil;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.UIManagerHelper;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.EventDispatcher;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AppEventListener;
import io.invertase.googlemobileads.common.ReactNativeAdView;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ReactNativeGoogleMobileAdsCachedBannerViewManager
    extends SimpleViewManager<ReactNativeAdView> {
  private static final String REACT_CLASS = "RNGoogleMobileAdsCachedBannerView";
  private final String EVENT_AD_LOADED = "onAdLoaded";
  private final String EVENT_AD_IMPRESSION = "onAdImpression";
  private final String EVENT_AD_CLICKED = "onAdClicked";
  private final String EVENT_AD_FAILED_TO_LOAD = "onAdFailedToLoad";
  private final String EVENT_AD_OPENED = "onAdOpened";
  private final String EVENT_AD_CLOSED = "onAdClosed";
  private final String EVENT_PAID = "onPaid";
  private final String EVENT_SIZE_CHANGE = "onSizeChange";
  private final String EVENT_APP_EVENT = "onAppEvent";
  private final String COMMAND_ID_RECORD_MANUAL_IMPRESSION = "recordManualImpression";

  @Nonnull
  @Override
  public String getName() {
    return REACT_CLASS;
  }

  @Nonnull
  @Override
  public ReactNativeAdView createViewInstance(@Nonnull ThemedReactContext themedReactContext) {
    return new ReactNativeAdView(themedReactContext);
  }

  @Override
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    builder.put(OnNativeEvent.EVENT_NAME, MapBuilder.of("registrationName", "onNativeEvent"));
    return builder.build();
  }

  @Override
  public void receiveCommand(
      @NonNull ReactNativeAdView reactViewGroup, String commandId, @Nullable ReadableArray args) {
    super.receiveCommand(reactViewGroup, commandId, args);

    if (commandId.equals(COMMAND_ID_RECORD_MANUAL_IMPRESSION)) {
      BaseAdView adView = getCachedAdView(reactViewGroup);
      if (adView instanceof AdManagerAdView) {
        ((AdManagerAdView) adView).recordManualImpression();
      }
    }
  }

  @ReactProp(name = "requestId")
  public void setRequestId(ReactNativeAdView reactViewGroup, String requestId) {
    android.util.Log.d("CachedBannerView", "setRequestId called with: " + requestId);
    
    if (requestId != null) {
      // Check if we already have the same requestId attached to avoid unnecessary re-setup
      BaseAdView currentAdView = getCachedAdView(reactViewGroup);
      android.util.Log.d("CachedBannerView", "Current ad view: " + (currentAdView != null ? "exists" : "null"));
      
      if (currentAdView != null) {
        // Get the current requestId from the view tag or compare with cached module
        ReactContext reactContext = (ReactContext) reactViewGroup.getContext();
        ReactNativeGoogleMobileAdsCachedBannerModule cachedBannerModule = 
            reactContext.getNativeModule(ReactNativeGoogleMobileAdsCachedBannerModule.class);
        
        if (cachedBannerModule != null) {
          BaseAdView cachedAdView = cachedBannerModule.getCachedBannerView(requestId);
          android.util.Log.d("CachedBannerView", "Cached ad view: " + (cachedAdView != null ? "exists" : "null"));
          android.util.Log.d("CachedBannerView", "Same ad view? " + (currentAdView == cachedAdView));
          
          // If the current ad view is the same as the cached one, no need to re-setup
          if (currentAdView == cachedAdView) {
            android.util.Log.d("CachedBannerView", "Same requestId and ad view, skipping re-setup");
            
            // But we should still trigger onAdLoaded event to ensure JS gets the dimensions
            WritableMap payload = Arguments.createMap();
            payload.putDouble("width", PixelUtil.toDIPFromPixel(currentAdView.getWidth()));
            payload.putDouble("height", PixelUtil.toDIPFromPixel(currentAdView.getHeight()));
            sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
            
            return;
          }
        }
      }
      
      android.util.Log.d("CachedBannerView", "Proceeding with setupCachedAdView");
      setupCachedAdView(reactViewGroup, requestId);
    }
  }

  private void setupCachedAdView(ReactNativeAdView reactViewGroup, String requestId) {
    // Remove existing ad view
    BaseAdView existingAdView = getCachedAdView(reactViewGroup);
    if (existingAdView != null) {
      existingAdView.setAdListener(null);
      if (existingAdView instanceof AdManagerAdView) {
        ((AdManagerAdView) existingAdView).setAppEventListener(null);
      }
      reactViewGroup.removeView(existingAdView);
    }

    // Get cached banner module
    ReactContext reactContext = (ReactContext) reactViewGroup.getContext();
    ReactNativeGoogleMobileAdsCachedBannerModule cachedBannerModule = 
        reactContext.getNativeModule(ReactNativeGoogleMobileAdsCachedBannerModule.class);

    if (cachedBannerModule != null) {
      BaseAdView cachedAdView = cachedBannerModule.getCachedBannerView(requestId);
      
      if (cachedAdView != null) {
        attachCachedAdView(reactViewGroup, cachedAdView);
      } else {
        // Ad not ready yet, send a failed to load event to indicate the ad is not available
        WritableMap payload = Arguments.createMap();
        payload.putString("code", "cached-ad-not-ready");
        payload.putString("message", "Cached ad with requestId '" + requestId + "' is not ready yet. Make sure to call requestGAMBannerAd first and wait for it to load.");
        sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
      }
    } else {
      // Module not available
      WritableMap payload = Arguments.createMap();
      payload.putString("code", "cached-banner-module-unavailable");
      payload.putString("message", "Cached banner module is not available");
      sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
    }
  }

  private void attachCachedAdView(ReactNativeAdView reactViewGroup, BaseAdView cachedAdView) {
    // Remove from previous parent if any - this is critical for reusing cached ads
    ViewGroup parent = (ViewGroup) cachedAdView.getParent();
    android.util.Log.d("CachedBannerView", "Initial parent: " + (parent != null ? parent.getClass().getSimpleName() : "null"));
    
    if (parent != null) {
      try {
        parent.removeView(cachedAdView);
        android.util.Log.d("CachedBannerView", "Removed cached ad from previous parent: " + parent.getClass().getSimpleName());
        
        // Force a layout pass to ensure the removal is processed
        parent.requestLayout();
        parent.invalidate();
        
      } catch (Exception e) {
        android.util.Log.w("CachedBannerView", "Error removing cached ad from parent: " + e.getMessage());
      }
    }
    
    // Multiple attempts to ensure parent is null
    int attempts = 0;
    while (cachedAdView.getParent() != null && attempts < 3) {
      attempts++;
      android.util.Log.d("CachedBannerView", "Attempt " + attempts + " to remove parent");
      
      try {
        ViewGroup currentParent = (ViewGroup) cachedAdView.getParent();
        currentParent.removeView(cachedAdView);
        currentParent.requestLayout();
        currentParent.invalidate();
        
        // Small delay to allow the removal to process
        try {
          Thread.sleep(10);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
        
      } catch (Exception ex) {
        android.util.Log.w("CachedBannerView", "Attempt " + attempts + " failed: " + ex.getMessage());
        break;
      }
    }
    
    // Final check - if still has parent, force proceed but log warning
    if (cachedAdView.getParent() != null) {
      android.util.Log.w("CachedBannerView", "Ad view still has parent after " + attempts + " attempts, forcing attachment anyway");
      // Don't return here - try to proceed with attachment
    } else {
      android.util.Log.d("CachedBannerView", "Successfully removed parent after " + attempts + " attempts");
    }

    // Set up event listeners
    cachedAdView.setOnPaidEventListener(
        new OnPaidEventListener() {
          @Override
          public void onPaidEvent(AdValue adValue) {
            WritableMap payload = Arguments.createMap();
            payload.putDouble("value", 1e-6 * adValue.getValueMicros());
            payload.putDouble("precision", adValue.getPrecisionType());
            payload.putString("currency", adValue.getCurrencyCode());
            sendEvent(reactViewGroup, EVENT_PAID, payload);
          }
        });

    cachedAdView.setAdListener(
        new AdListener() {
          @Override
          public void onAdLoaded() {
            WritableMap payload = Arguments.createMap();
            payload.putDouble("width", PixelUtil.toDIPFromPixel(cachedAdView.getWidth()));
            payload.putDouble("height", PixelUtil.toDIPFromPixel(cachedAdView.getHeight()));
            sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            int errorCode = loadAdError.getCode();
            WritableMap payload = ReactNativeGoogleMobileAdsCommon.errorCodeToMap(errorCode);
            sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
          }

          @Override
          public void onAdOpened() {
            sendEvent(reactViewGroup, EVENT_AD_OPENED, null);
          }

          @Override
          public void onAdClosed() {
            sendEvent(reactViewGroup, EVENT_AD_CLOSED, null);
          }

          @Override
          public void onAdImpression() {
            sendEvent(reactViewGroup, EVENT_AD_IMPRESSION, null);
          }

          @Override
          public void onAdClicked() {
            sendEvent(reactViewGroup, EVENT_AD_CLICKED, null);
          }
        });

    if (cachedAdView instanceof AdManagerAdView) {
      ((AdManagerAdView) cachedAdView)
          .setAppEventListener(
              new AppEventListener() {
                @Override
                public void onAppEvent(@NonNull String name, @Nullable String data) {
                  WritableMap payload = Arguments.createMap();
                  payload.putString("name", name);
                  payload.putString("data", data);
                  sendEvent(reactViewGroup, EVENT_APP_EVENT, payload);
                }
              });
    }

    // Add to view hierarchy with proper layout parameters
    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    );
    cachedAdView.setLayoutParams(layoutParams);
    
    // Try to add the view, if it fails due to parent issues, force remove and try again
    try {
      reactViewGroup.addView(cachedAdView);
      android.util.Log.d("CachedBannerView", "Successfully added cached ad view to new parent");
    } catch (IllegalStateException e) {
      if (e.getMessage() != null && e.getMessage().contains("already has a parent")) {
        android.util.Log.w("CachedBannerView", "Child already has parent error, forcing removal and retrying");
        
        // Force remove from any parent using multiple approaches
        ViewGroup currentParent = (ViewGroup) cachedAdView.getParent();
        if (currentParent != null) {
          try {
            currentParent.removeView(cachedAdView);
            android.util.Log.d("CachedBannerView", "Force removed from parent: " + currentParent.getClass().getSimpleName());
          } catch (Exception removeEx) {
            android.util.Log.e("CachedBannerView", "Failed to force remove: " + removeEx.getMessage());
          }
        }
        
        // Use reflection to forcefully clear the parent reference if still exists
        if (cachedAdView.getParent() != null) {
          try {
            java.lang.reflect.Field parentField = android.view.View.class.getDeclaredField("mParent");
            parentField.setAccessible(true);
            parentField.set(cachedAdView, null);
            android.util.Log.d("CachedBannerView", "Forcefully cleared parent using reflection");
          } catch (Exception reflectionEx) {
            android.util.Log.w("CachedBannerView", "Failed to clear parent via reflection: " + reflectionEx.getMessage());
          }
        }
        
        // Try adding again
        try {
          reactViewGroup.addView(cachedAdView);
          android.util.Log.d("CachedBannerView", "Successfully added cached ad view after force removal");
        } catch (Exception retryEx) {
          android.util.Log.e("CachedBannerView", "Failed to add view even after force removal: " + retryEx.getMessage());
          
          // Last resort: create a new container view and add the ad view to it
          try {
            android.widget.FrameLayout wrapper = new android.widget.FrameLayout(reactViewGroup.getContext());
            wrapper.setLayoutParams(layoutParams);
            
            // Clear parent one more time before adding to wrapper
            if (cachedAdView.getParent() != null) {
              ((ViewGroup) cachedAdView.getParent()).removeView(cachedAdView);
            }
            
            wrapper.addView(cachedAdView);
            reactViewGroup.addView(wrapper);
            android.util.Log.d("CachedBannerView", "Successfully added cached ad view using wrapper approach");
          } catch (Exception wrapperEx) {
            android.util.Log.e("CachedBannerView", "Even wrapper approach failed: " + wrapperEx.getMessage());
            WritableMap payload = Arguments.createMap();
            payload.putString("code", "view-attachment-failed");
            payload.putString("message", "Failed to attach cached ad view: " + wrapperEx.getMessage());
            sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
            return;
          }
        }
      } else {
        android.util.Log.e("CachedBannerView", "Unexpected error adding view: " + e.getMessage());
        WritableMap payload = Arguments.createMap();
        payload.putString("code", "view-attachment-failed");
        payload.putString("message", "Failed to attach cached ad view: " + e.getMessage());
        sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
        return;
      }
    }
    
    // Force a layout pass to ensure the ad view is properly sized and positioned
    cachedAdView.post(new Runnable() {
      @Override
      public void run() {
        cachedAdView.requestLayout();
        
        // Trigger onAdLoaded event after layout is complete
        WritableMap payload = Arguments.createMap();
        payload.putDouble("width", PixelUtil.toDIPFromPixel(cachedAdView.getWidth()));
        payload.putDouble("height", PixelUtil.toDIPFromPixel(cachedAdView.getHeight()));
        sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
      }
    });
  }

  @Nullable
  private BaseAdView getCachedAdView(ViewGroup reactViewGroup) {
    if (reactViewGroup.getChildCount() > 0) {
      return (BaseAdView) reactViewGroup.getChildAt(0);
    }
    return null;
  }

  @Override
  public void onDropViewInstance(@NonNull ReactNativeAdView reactViewGroup) {
    BaseAdView adView = getCachedAdView(reactViewGroup);
    if (adView != null) {
      adView.setAdListener(null);
      if (adView instanceof AdManagerAdView) {
        ((AdManagerAdView) adView).setAppEventListener(null);
      }
      reactViewGroup.removeView(adView);
    }
    super.onDropViewInstance(reactViewGroup);
  }

  private void sendEvent(ReactNativeAdView reactViewGroup, String type, WritableMap payload) {
    WritableMap event = Arguments.createMap();
    event.putString("type", type);

    if (payload != null) {
      event.merge(payload);
    }

    ThemedReactContext themedReactContext = ((ThemedReactContext) reactViewGroup.getContext());
    EventDispatcher eventDispatcher =
        UIManagerHelper.getEventDispatcherForReactTag(themedReactContext, reactViewGroup.getId());
    if (eventDispatcher != null) {
      eventDispatcher.dispatchEvent(new OnNativeEvent(reactViewGroup.getId(), event));
    }
  }
}
