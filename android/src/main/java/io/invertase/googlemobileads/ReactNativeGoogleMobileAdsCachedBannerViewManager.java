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
import android.widget.FrameLayout;
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
    // Remove existing ad view and container
    clearExistingAdView(reactViewGroup);

    // Get cached banner module
    ReactContext reactContext = (ReactContext) reactViewGroup.getContext();
    ReactNativeGoogleMobileAdsCachedBannerModule cachedBannerModule = 
        reactContext.getNativeModule(ReactNativeGoogleMobileAdsCachedBannerModule.class);

    if (cachedBannerModule != null) {
      // Get the cached ad view and attach it with proper parent handling
      android.util.Log.d("CachedBannerView", "Getting cached ad view for requestId: " + requestId);
      
      BaseAdView cachedAdView = cachedBannerModule.getCachedBannerView(requestId);
      
      if (cachedAdView != null) {
        attachCachedAdViewWithFrameLayout(reactViewGroup, cachedAdView);
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

  private void clearExistingAdView(ReactNativeAdView reactViewGroup) {
    android.util.Log.d("CachedBannerView", "Clearing existing ad view, child count: " + reactViewGroup.getChildCount());
    
    // Remove all children (including frame layout containers)
    for (int i = reactViewGroup.getChildCount() - 1; i >= 0; i--) {
      android.view.View child = reactViewGroup.getChildAt(i);
      android.util.Log.d("CachedBannerView", "Removing child at index " + i + ": " + child.getClass().getSimpleName());
      
      // If it's a FrameLayout container, clean up the ad view inside it first
      if (child instanceof FrameLayout) {
        FrameLayout container = (FrameLayout) child;
        if (container.getChildCount() > 0) {
          android.view.View adViewChild = container.getChildAt(0);
          if (adViewChild instanceof BaseAdView) {
            BaseAdView adView = (BaseAdView) adViewChild;
            android.util.Log.d("CachedBannerView", "Cleaning up ad view inside container");
            cleanupAdViewListeners(adView);
          }
        }
        container.removeAllViews();
      } else if (child instanceof BaseAdView) {
        // Direct ad view (legacy approach)
        BaseAdView adView = (BaseAdView) child;
        android.util.Log.d("CachedBannerView", "Cleaning up direct ad view");
        cleanupAdViewListeners(adView);
      }
      
      reactViewGroup.removeViewAt(i);
    }
    
    android.util.Log.d("CachedBannerView", "Finished clearing, child count now: " + reactViewGroup.getChildCount());
  }

  private void cleanupAdViewListeners(BaseAdView adView) {
    try {
      adView.setAdListener(null);
      adView.setOnPaidEventListener(null);
      if (adView instanceof AdManagerAdView) {
        ((AdManagerAdView) adView).setAppEventListener(null);
      }
    } catch (Exception e) {
      android.util.Log.w("CachedBannerView", "Error cleaning up ad view listeners: " + e.getMessage());
    }
  }

  /**
   * Attach cached ad view using frame layout container approach (inspired by Lokal Android app)
   * This approach provides better isolation and prevents parent attachment issues
   */
  private void attachCachedAdViewWithFrameLayout(ReactNativeAdView reactViewGroup, BaseAdView cachedAdView) {
    android.util.Log.d("CachedBannerView", "=== FRAME LAYOUT ATTACHMENT DEBUG ===");
    android.util.Log.d("CachedBannerView", "Starting frame layout attachment process");
    
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
    
    // Multiple attempts to ensure parent is null (same as original logic)
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
    } else {
      android.util.Log.d("CachedBannerView", "Successfully removed parent after " + attempts + " attempts");
    }

    // Create a FrameLayout container (inspired by Lokal's BottomBannerAdView approach)
    FrameLayout adContainer = new FrameLayout(reactViewGroup.getContext());
    android.util.Log.d("CachedBannerView", "Created FrameLayout container");
    
    // Get the actual ad dimensions for proper sizing
    int adWidth = cachedAdView.getWidth();
    int adHeight = cachedAdView.getHeight();
    android.util.Log.d("CachedBannerView", "Cached ad view dimensions: " + adWidth + "x" + adHeight);
    
    // If ad view doesn't have dimensions yet, use wrap_content and let it size itself
    ViewGroup.LayoutParams containerParams;
    if (adWidth > 0 && adHeight > 0) {
      // Use exact dimensions if available
      containerParams = new ViewGroup.LayoutParams(adWidth, adHeight);
      android.util.Log.d("CachedBannerView", "Using exact container dimensions: " + adWidth + "x" + adHeight);
    } else {
      // Use wrap_content to let the ad size itself
      containerParams = new ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT
      );
      android.util.Log.d("CachedBannerView", "Using wrap_content container dimensions");
    }
    
    adContainer.setLayoutParams(containerParams);
    
    // Set up ad view layout params for the container
    FrameLayout.LayoutParams adViewParams = new FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    );
    cachedAdView.setLayoutParams(adViewParams);
    
    // Add the cached ad view to the container
    try {
      adContainer.addView(cachedAdView);
      android.util.Log.d("CachedBannerView", "Successfully added cached ad view to container");
    } catch (IllegalStateException e) {
      if (e.getMessage() != null && e.getMessage().contains("already has a parent")) {
        android.util.Log.w("CachedBannerView", "Child already has parent error during container addition, forcing removal");
        
        // Force remove using reflection as last resort
        try {
          java.lang.reflect.Field parentField = android.view.View.class.getDeclaredField("mParent");
          parentField.setAccessible(true);
          parentField.set(cachedAdView, null);
          android.util.Log.d("CachedBannerView", "Forcefully cleared parent using reflection");
          
          // Try adding to container again
          adContainer.addView(cachedAdView);
          android.util.Log.d("CachedBannerView", "Successfully added cached ad view to container after reflection fix");
        } catch (Exception reflectionEx) {
          android.util.Log.e("CachedBannerView", "Failed to add to container even with reflection: " + reflectionEx.getMessage());
          WritableMap payload = Arguments.createMap();
          payload.putString("code", "container-attachment-failed");
          payload.putString("message", "Failed to attach cached ad view to container: " + reflectionEx.getMessage());
          sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
          return;
        }
      } else {
        android.util.Log.e("CachedBannerView", "Unexpected error adding view to container: " + e.getMessage());
        WritableMap payload = Arguments.createMap();
        payload.putString("code", "container-attachment-failed");
        payload.putString("message", "Failed to attach cached ad view to container: " + e.getMessage());
        sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
        return;
      }
    }
    
    // Set up event listeners (same as original logic)
    setupAdViewEventListeners(reactViewGroup, cachedAdView);
    
    // Add the container to the React view group
    try {
      reactViewGroup.addView(adContainer);
      android.util.Log.d("CachedBannerView", "Successfully added container to React view group");
    } catch (Exception e) {
      android.util.Log.e("CachedBannerView", "Failed to add container to React view group: " + e.getMessage());
      WritableMap payload = Arguments.createMap();
      payload.putString("code", "react-view-attachment-failed");
      payload.putString("message", "Failed to attach container to React view: " + e.getMessage());
      sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, payload);
      return;
    }
    
    // Force a layout pass and trigger onAdLoaded event
    adContainer.post(new Runnable() {
      @Override
      public void run() {
        android.util.Log.d("CachedBannerView", "=== POST-LAYOUT DIMENSIONS DEBUG ===");
        
        // Request layout for both container and ad view
        adContainer.requestLayout();
        cachedAdView.requestLayout();
        
        // Get final dimensions
        int finalAdWidth = cachedAdView.getWidth();
        int finalAdHeight = cachedAdView.getHeight();
        int finalContainerWidth = adContainer.getWidth();
        int finalContainerHeight = adContainer.getHeight();
        
        android.util.Log.d("CachedBannerView", "Final ad view dimensions: " + finalAdWidth + "x" + finalAdHeight);
        android.util.Log.d("CachedBannerView", "Final container dimensions: " + finalContainerWidth + "x" + finalContainerHeight);
        
        // Use the larger of the two dimensions for reporting (container should match or exceed ad view)
        int reportWidth = Math.max(finalAdWidth, finalContainerWidth);
        int reportHeight = Math.max(finalAdHeight, finalContainerHeight);
        
        android.util.Log.d("CachedBannerView", "Reporting dimensions: " + reportWidth + "x" + reportHeight);
        
        // Convert to DP and trigger onAdLoaded event
        double widthDp = PixelUtil.toDIPFromPixel(reportWidth);
        double heightDp = PixelUtil.toDIPFromPixel(reportHeight);
        
        android.util.Log.d("CachedBannerView", "Reporting DP dimensions: " + widthDp + "x" + heightDp);
        
        WritableMap payload = Arguments.createMap();
        payload.putDouble("width", widthDp);
        payload.putDouble("height", heightDp);
        sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
        
        android.util.Log.d("CachedBannerView", "=== FRAME LAYOUT ATTACHMENT COMPLETE ===");
      }
    });
  }

  private void setupAdViewEventListeners(ReactNativeAdView reactViewGroup, BaseAdView adView) {
    // Set up paid event listener
    adView.setOnPaidEventListener(
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

    // Set up ad listener
    adView.setAdListener(
        new AdListener() {
          @Override
          public void onAdLoaded() {
            // Note: We don't trigger onAdLoaded here because the ad is already loaded
            // The onAdLoaded event will be triggered after the view is properly attached
            android.util.Log.d("CachedBannerView", "AdListener.onAdLoaded called (cached ad)");
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

    // Set up app event listener for GAM ads
    if (adView instanceof AdManagerAdView) {
      ((AdManagerAdView) adView)
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
  }

  @Nullable
  private BaseAdView getCachedAdView(ViewGroup reactViewGroup) {
    if (reactViewGroup.getChildCount() > 0) {
      android.view.View child = reactViewGroup.getChildAt(0);
      
      // Check if it's a FrameLayout container (new approach)
      if (child instanceof FrameLayout) {
        FrameLayout container = (FrameLayout) child;
        if (container.getChildCount() > 0) {
          android.view.View adViewChild = container.getChildAt(0);
          if (adViewChild instanceof BaseAdView) {
            return (BaseAdView) adViewChild;
          }
        }
      }
      // Check if it's a direct BaseAdView (legacy approach)
      else if (child instanceof BaseAdView) {
        return (BaseAdView) child;
      }
    }
    return null;
  }

  @Override
  public void onDropViewInstance(@NonNull ReactNativeAdView reactViewGroup) {
    android.util.Log.d("CachedBannerView", "onDropViewInstance called");
    clearExistingAdView(reactViewGroup);
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
      eventDispatcher.dispatchEvent(new OnNativeEvent(-1, reactViewGroup.getId(), event));
    }
  }
}
