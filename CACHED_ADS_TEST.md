# Cached Ads System Test

This document provides a simple test to verify that the cached ads system is working correctly.

## Test Steps

1. **Import the new components and functions:**

```javascript
import {
  requestBannerAd,
  requestGAMBannerAd,
  CachedBannerAd,
  CachedGAMBannerAd,
} from 'react-native-google-mobile-ads';
```

2. **Test Banner Ad Request:**

```javascript
const testBannerAd = async () => {
  try {
    const result = await requestBannerAd({
      unitId: 'ca-app-pub-3940256099942544/6300978111', // Test ad unit
      size: 'BANNER',
    });
    console.log('Banner ad requested successfully:', result);
    return result.requestId;
  } catch (error) {
    console.error('Banner ad request failed:', error);
  }
};
```

3. **Test GAM Banner Ad Request:**

```javascript
const testGAMBannerAd = async () => {
  try {
    const result = await requestGAMBannerAd({
      unitId: '/6499/example/banner', // Test GAM ad unit
      sizes: ['BANNER', 'MEDIUM_RECTANGLE'],
    });
    console.log('GAM banner ad requested successfully:', result);
    return result.requestId;
  } catch (error) {
    console.error('GAM banner ad request failed:', error);
  }
};
```

4. **Test Rendering Cached Ads:**

```javascript
import React, { useState, useEffect } from 'react';
import { View, Button } from 'react-native';

const CachedAdsTest = () => {
  const [bannerRequestId, setBannerRequestId] = useState(null);
  const [gamBannerRequestId, setGamBannerRequestId] = useState(null);

  const loadBannerAd = async () => {
    const requestId = await testBannerAd();
    setBannerRequestId(requestId);
  };

  const loadGAMBannerAd = async () => {
    const requestId = await testGAMBannerAd();
    setGamBannerRequestId(requestId);
  };

  return (
    <View style={{ flex: 1, padding: 20 }}>
      <Button title="Load Banner Ad" onPress={loadBannerAd} />
      <Button title="Load GAM Banner Ad" onPress={loadGAMBannerAd} />

      {bannerRequestId && <CachedBannerAd requestId={bannerRequestId} style={{ marginTop: 20 }} />}

      {gamBannerRequestId && (
        <CachedGAMBannerAd requestId={gamBannerRequestId} style={{ marginTop: 20 }} />
      )}
    </View>
  );
};

export default CachedAdsTest;
```

## Expected Behavior

1. **Module Registration**: The `RNGoogleMobileAdsRequestModule` should be found by TurboModuleRegistry
2. **Ad Request**: Both `requestBannerAd` and `requestGAMBannerAd` should return promises that resolve with `{ requestId, width, height }`
3. **Ad Rendering**: The `CachedBannerAd` and `CachedGAMBannerAd` components should render the cached ads using the requestId
4. **Re-rendering**: The same ad should persist even when React re-renders the component

## Troubleshooting

If you encounter the TurboModuleRegistry error, ensure that:

1. The native modules are properly registered in both iOS and Android
2. The app has been rebuilt after adding the new native modules
3. The module names match between JavaScript specs and native implementations

## Manual Impression Testing (GAM Ads Only)

For GAM ads with manual impressions enabled:

```javascript
const testManualImpression = async () => {
  const result = await requestGAMBannerAd({
    unitId: '/6499/example/banner',
    sizes: ['BANNER'],
    manualImpressionsEnabled: true,
  });

  // Later, record impression manually
  // This would be called from the CachedGAMBannerAd component
  // recordManualImpression(reactTag);
};
```
