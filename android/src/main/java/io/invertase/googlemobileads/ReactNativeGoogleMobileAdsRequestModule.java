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

import android.app.Activity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.module.annotations.ReactModule;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import io.invertase.googlemobileads.common.SharedUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nonnull;
import org.json.JSONException;
import org.json.JSONObject;

@ReactModule(name = ReactNativeGoogleMobileAdsRequestModule.NAME)
public class ReactNativeGoogleMobileAdsRequestModule extends ReactContextBaseJavaModule {
  private static final String REACT_CLASS = "RNGoogleMobileAdsRequestModule";
  public static final String NAME = "RNGoogleMobileAdsRequestModule";
  
  private final Map<String, AdView> bannerAds = new HashMap<>();
  private final Map<String, AdManagerAdView> gamBannerAds = new HashMap<>();

  public ReactNativeGoogleMobileAdsRequestModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Nonnull
  @Override
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void requestBannerAd(
      String unitId,
      String size,
      @Nullable Double maxHeight,
      @Nullable Double width,
      @Nullable String requestOptions,
      Promise promise) {
    
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("no_activity", "No current activity available");
      return;
    }

    try {
      // Generate unique request ID
      String requestId = UUID.randomUUID().toString();

      // Create banner view
      AdView bannerView = new AdView(currentActivity);
      bannerView.setAdUnitId(unitId);

      // Set ad size
      AdSize adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(size, null);
      bannerView.setAdSize(adSize);

      // Create ad request
      AdRequest.Builder requestBuilder = new AdRequest.Builder();
      if (requestOptions != null && !requestOptions.equals("{}")) {
        try {
          JSONObject jsonObject = new JSONObject(requestOptions);
          WritableMap writableMap = SharedUtils.jsonObjectToWritableMap(jsonObject);
          AdRequest request = ReactNativeGoogleMobileAdsCommon.buildAdRequest(writableMap);
          bannerView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
              WritableMap result = Arguments.createMap();
              result.putString("requestId", requestId);
              result.putDouble("width", adSize.getWidth());
              result.putDouble("height", adSize.getHeight());
              promise.resolve(result);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
              bannerAds.remove(requestId);
              promise.reject("ad_load_failed", loadAdError.getMessage());
            }
          });
          bannerView.loadAd(request);
        } catch (JSONException e) {
          promise.reject("invalid_request_options", "Invalid request options JSON");
          return;
        }
      } else {
        bannerView.setAdListener(new AdListener() {
          @Override
          public void onAdLoaded() {
            WritableMap result = Arguments.createMap();
            result.putString("requestId", requestId);
            result.putDouble("width", adSize.getWidth());
            result.putDouble("height", adSize.getHeight());
            promise.resolve(result);
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            bannerAds.remove(requestId);
            promise.reject("ad_load_failed", loadAdError.getMessage());
          }
        });
        bannerView.loadAd(new AdRequest.Builder().build());
      }

      // Store the banner view
      bannerAds.put(requestId, bannerView);

    } catch (Exception e) {
      promise.reject("request_failed", e.getMessage());
    }
  }

  @ReactMethod
  public void requestGAMBannerAd(
      String unitId,
      ReadableArray sizes,
      @Nullable Double maxHeight,
      @Nullable Double width,
      @Nullable String requestOptions,
      @Nullable Boolean manualImpressionsEnabled,
      Promise promise) {
    
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject("no_activity", "No current activity available");
      return;
    }

    try {
      // Generate unique request ID
      String requestId = UUID.randomUUID().toString();

      // Create GAM banner view
      AdManagerAdView gamBannerView = new AdManagerAdView(currentActivity);
      gamBannerView.setAdUnitId(unitId);

      // Set ad sizes
      List<AdSize> adSizes = new ArrayList<>();
      for (int i = 0; i < sizes.size(); i++) {
        String sizeString = sizes.getString(i);
        AdSize adSize = ReactNativeGoogleMobileAdsCommon.getAdSize(sizeString, null);
        adSizes.add(adSize);
      }
      gamBannerView.setAdSizes(adSizes.toArray(new AdSize[0]));

      // Set manual impressions if enabled
      if (manualImpressionsEnabled != null && manualImpressionsEnabled) {
        gamBannerView.setManualImpressionsEnabled(true);
      }

      // Create ad request
      if (requestOptions != null && !requestOptions.equals("{}")) {
        try {
          JSONObject jsonObject = new JSONObject(requestOptions);
          WritableMap writableMap = SharedUtils.jsonObjectToWritableMap(jsonObject);
          AdRequest request = ReactNativeGoogleMobileAdsCommon.buildAdRequest(writableMap);
          gamBannerView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
              AdSize adSize = gamBannerView.getAdSize();
              WritableMap result = Arguments.createMap();
              result.putString("requestId", requestId);
              result.putDouble("width", adSize.getWidth());
              result.putDouble("height", adSize.getHeight());
              promise.resolve(result);
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
              gamBannerAds.remove(requestId);
              promise.reject("ad_load_failed", loadAdError.getMessage());
            }
          });
          gamBannerView.loadAd(request);
        } catch (JSONException e) {
          promise.reject("invalid_request_options", "Invalid request options JSON");
          return;
        }
      } else {
        gamBannerView.setAdListener(new AdListener() {
          @Override
          public void onAdLoaded() {
            AdSize adSize = gamBannerView.getAdSize();
            WritableMap result = Arguments.createMap();
            result.putString("requestId", requestId);
            result.putDouble("width", adSize.getWidth());
            result.putDouble("height", adSize.getHeight());
            promise.resolve(result);
          }

          @Override
          public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
            gamBannerAds.remove(requestId);
            promise.reject("ad_load_failed", loadAdError.getMessage());
          }
        });
        gamBannerView.loadAd(new AdRequest.Builder().build());
      }

      // Store the GAM banner view
      gamBannerAds.put(requestId, gamBannerView);

    } catch (Exception e) {
      promise.reject("request_failed", e.getMessage());
    }
  }

  @ReactMethod
  public void releaseAd(String requestId) {
    // Remove from banner ads
    AdView bannerView = bannerAds.get(requestId);
    if (bannerView != null) {
      bannerView.destroy();
      bannerAds.remove(requestId);
      return;
    }

    // Remove from GAM banner ads
    AdManagerAdView gamBannerView = gamBannerAds.get(requestId);
    if (gamBannerView != null) {
      gamBannerView.destroy();
      gamBannerAds.remove(requestId);
    }
  }

  @ReactMethod(isBlockingSynchronousMethod = true)
  public boolean hasAd(String requestId) {
    return bannerAds.containsKey(requestId) || gamBannerAds.containsKey(requestId);
  }

  // Method to get banner view by request ID (for use by view manager)
  public AdView getBannerViewForRequestId(String requestId) {
    return bannerAds.get(requestId);
  }

  // Method to get GAM banner view by request ID (for use by view manager)
  public AdManagerAdView getGAMBannerViewForRequestId(String requestId) {
    return gamBannerAds.get(requestId);
  }
}
