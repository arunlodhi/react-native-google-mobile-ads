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
import androidx.annotation.Nullable;
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
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.BaseAdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AppEventListener;
import io.invertase.googlemobileads.common.ReactNativeCachedAdView;
import java.util.Map;
import javax.annotation.Nonnull;

public class ReactNativeGoogleMobileAdsCachedBannerAdViewManager
    extends SimpleViewManager<ReactNativeCachedAdView> {
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
  public ReactNativeCachedAdView createViewInstance(@Nonnull ThemedReactContext themedReactContext) {
    return new ReactNativeCachedAdView(themedReactContext);
  }

  @Override
  public Map<String, Object> getExportedCustomDirectEventTypeConstants() {
    MapBuilder.Builder<String, Object> builder = MapBuilder.builder();
    builder.put(OnNativeEvent.EVENT_NAME, MapBuilder.of("registrationName", "onNativeEvent"));
    return builder.build();
  }

  @Override
  public void receiveCommand(
      @NonNull ReactNativeCachedAdView reactViewGroup, String commandId, @Nullable ReadableArray args) {
    super.receiveCommand(reactViewGroup, commandId, args);

    if (commandId.equals(COMMAND_ID_RECORD_MANUAL_IMPRESSION)) {
      BaseAdView adView = getAdView(reactViewGroup);
      if (adView instanceof AdManagerAdView) {
        ((AdManagerAdView) adView).recordManualImpression();
      }
    }
  }

  @ReactProp(name = "requestId")
  public void setRequestId(ReactNativeCachedAdView reactViewGroup, String value) {
    reactViewGroup.setRequestId(value);
    requestAd(reactViewGroup);
  }

  @Override
  public void onDropViewInstance(@NonNull ReactNativeCachedAdView reactViewGroup) {
    BaseAdView adView = getAdView(reactViewGroup);
    if (adView != null) {
      adView.setAdListener(null);
      if (adView instanceof AdManagerAdView) {
        ((AdManagerAdView) adView).setAppEventListener(null);
      }
      adView.destroy();
      reactViewGroup.removeView(adView);
    }
    super.onDropViewInstance(reactViewGroup);
  }

  @Nullable
  private BaseAdView getAdView(ViewGroup reactViewGroup) {
    if (reactViewGroup.getChildCount() > 0) {
      return (BaseAdView) reactViewGroup.getChildAt(0);
    }
    return null;
  }

  private void requestAd(ReactNativeCachedAdView reactViewGroup) {
    String requestId = reactViewGroup.getRequestId();

    if (requestId == null) {
      return;
    }

    // Clean up existing ad view
    BaseAdView existingAdView = getAdView(reactViewGroup);
    if (existingAdView != null) {
      existingAdView.setAdListener(null);
      if (existingAdView instanceof AdManagerAdView) {
        ((AdManagerAdView) existingAdView).setAppEventListener(null);
      }
      existingAdView.destroy();
      reactViewGroup.removeView(existingAdView);
    }

    // Get the request module
    ReactContext reactContext = (ReactContext) reactViewGroup.getContext();
    ReactNativeGoogleMobileAdsRequestModule requestModule =
        reactContext.getNativeModule(ReactNativeGoogleMobileAdsRequestModule.class);

    if (requestModule == null) {
      WritableMap errorPayload = Arguments.createMap();
      errorPayload.putString("code", "request_module_not_found");
      errorPayload.putString("message", "Request module not found");
      sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, errorPayload);
      return;
    }

    // Try to get banner view first
    AdView bannerView = requestModule.getBannerViewForRequestId(requestId);
    if (bannerView != null) {
      setupBannerView(reactViewGroup, bannerView);
      return;
    }

    // Try to get GAM banner view
    AdManagerAdView gamBannerView = requestModule.getGAMBannerViewForRequestId(requestId);
    if (gamBannerView != null) {
      setupGAMBannerView(reactViewGroup, gamBannerView);
      return;
    }

    // No ad found for this request ID
    WritableMap notFoundPayload = Arguments.createMap();
    notFoundPayload.putString("code", "ad_not_found");
    notFoundPayload.putString("message", "Ad not found for request ID");
    sendEvent(reactViewGroup, EVENT_AD_FAILED_TO_LOAD, notFoundPayload);
  }

  private void setupBannerView(ReactNativeCachedAdView reactViewGroup, AdView bannerView) {
    bannerView.setOnPaidEventListener(new OnPaidEventListener() {
      @Override
      public void onPaidEvent(AdValue adValue) {
        WritableMap payload = Arguments.createMap();
        payload.putDouble("value", 1e-6 * adValue.getValueMicros());
        payload.putDouble("precision", adValue.getPrecisionType());
        payload.putString("currency", adValue.getCurrencyCode());
        sendEvent(reactViewGroup, EVENT_PAID, payload);
      }
    });

    bannerView.setAdListener(new AdListener() {
      @Override
      public void onAdLoaded() {
        AdSize adSize = bannerView.getAdSize();
        int width = adSize.getWidthInPixels(reactViewGroup.getContext());
        int height = adSize.getHeightInPixels(reactViewGroup.getContext());

        WritableMap payload = Arguments.createMap();
        payload.putDouble("width", PixelUtil.toDIPFromPixel(width));
        payload.putDouble("height", PixelUtil.toDIPFromPixel(height));

        sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
      }

      @Override
      public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
        WritableMap payload = ReactNativeGoogleMobileAdsCommon.errorCodeToMap(loadAdError.getCode());
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

    reactViewGroup.addView(bannerView);
  }

  private void setupGAMBannerView(ReactNativeCachedAdView reactViewGroup, AdManagerAdView gamBannerView) {
    gamBannerView.setOnPaidEventListener(new OnPaidEventListener() {
      @Override
      public void onPaidEvent(AdValue adValue) {
        WritableMap payload = Arguments.createMap();
        payload.putDouble("value", 1e-6 * adValue.getValueMicros());
        payload.putDouble("precision", adValue.getPrecisionType());
        payload.putString("currency", adValue.getCurrencyCode());
        sendEvent(reactViewGroup, EVENT_PAID, payload);
      }
    });

    gamBannerView.setAdListener(new AdListener() {
      @Override
      public void onAdLoaded() {
        AdSize adSize = gamBannerView.getAdSize();
        int width = adSize.getWidthInPixels(reactViewGroup.getContext());
        int height = adSize.getHeightInPixels(reactViewGroup.getContext());

        WritableMap payload = Arguments.createMap();
        payload.putDouble("width", PixelUtil.toDIPFromPixel(width));
        payload.putDouble("height", PixelUtil.toDIPFromPixel(height));

        sendEvent(reactViewGroup, EVENT_AD_LOADED, payload);
      }

      @Override
      public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
        WritableMap payload = ReactNativeGoogleMobileAdsCommon.errorCodeToMap(loadAdError.getCode());
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

    gamBannerView.setAppEventListener(new AppEventListener() {
      @Override
      public void onAppEvent(@NonNull String name, @Nullable String data) {
        WritableMap payload = Arguments.createMap();
        payload.putString("name", name);
        payload.putString("data", data);
        sendEvent(reactViewGroup, EVENT_APP_EVENT, payload);
      }
    });

    reactViewGroup.addView(gamBannerView);
  }

  private void sendEvent(ReactNativeCachedAdView reactViewGroup, String type, WritableMap payload) {
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
