# Cached Ads Usage Guide

This guide explains how to use the new cached ad system in react-native-google-mobile-ads. This system allows you to pre-load ads and reliably re-render them even when React performs re-rendering.

## Overview

The cached ad system consists of:

1. **Request Functions**: `requestBannerAd()` and `requestGAMBannerAd()` to pre-load ads
2. **Components**: `<CachedBannerAd />` and `<CachedGAMBannerAd />` to render pre-loaded ads
3. **Management Functions**: `releaseAd()` and `hasAd()` to manage ad lifecycle

## Basic Usage

### Step 1: Request an Ad

```javascript
import {
  requestBannerAd,
  requestGAMBannerAd,
  BannerAdSize,
  TestIds,
} from 'react-native-google-mobile-ads';

// Request a regular banner ad
const requestRegularBanner = async () => {
  try {
    const response = await requestBannerAd({
      unitId: TestIds.BANNER,
      size: BannerAdSize.BANNER,
      requestOptions: {
        requestNonPersonalizedAdsOnly: true,
      },
    });

    console.log('Ad loaded with requestId:', response.requestId);
    console.log('Ad dimensions:', response.dimensions);
    return response.requestId;
  } catch (error) {
    console.error('Failed to load ad:', error);
  }
};

// Request a GAM banner ad
const requestGAMBanner = async () => {
  try {
    const response = await requestGAMBannerAd({
      unitId: TestIds.GAM_BANNER,
      sizes: [BannerAdSize.BANNER, BannerAdSize.LARGE_BANNER],
      requestOptions: {
        requestNonPersonalizedAdsOnly: true,
      },
      manualImpressionsEnabled: true,
    });

    console.log('GAM Ad loaded with requestId:', response.requestId);
    return response.requestId;
  } catch (error) {
    console.error('Failed to load GAM ad:', error);
  }
};
```

### Step 2: Render the Ad

```javascript
import React, { useState, useEffect } from 'react';
import { View } from 'react-native';
import { CachedBannerAd, CachedGAMBannerAd } from 'react-native-google-mobile-ads';

const AdComponent = () => {
  const [bannerRequestId, setBannerRequestId] = useState(null);
  const [gamBannerRequestId, setGamBannerRequestId] = useState(null);

  useEffect(() => {
    // Load ads when component mounts
    const loadAds = async () => {
      const bannerId = await requestRegularBanner();
      const gamId = await requestGAMBanner();

      setBannerRequestId(bannerId);
      setGamBannerRequestId(gamId);
    };

    loadAds();
  }, []);

  return (
    <View>
      {bannerRequestId && (
        <CachedBannerAd
          requestId={bannerRequestId}
          onAdLoaded={dimensions => {
            console.log('Banner ad rendered:', dimensions);
          }}
          onAdFailedToLoad={error => {
            console.error('Banner ad failed to render:', error);
          }}
          onAdClicked={() => {
            console.log('Banner ad clicked');
          }}
        />
      )}

      {gamBannerRequestId && (
        <CachedGAMBannerAd
          requestId={gamBannerRequestId}
          onAdLoaded={dimensions => {
            console.log('GAM Banner ad rendered:', dimensions);
          }}
          onAppEvent={event => {
            console.log('GAM App event:', event);
          }}
        />
      )}
    </View>
  );
};
```

## Advanced Usage

### Managing Ad Lifecycle

```javascript
import { releaseAd, hasAd } from 'react-native-google-mobile-ads';

const AdManager = () => {
  const [requestIds, setRequestIds] = useState([]);

  const preloadMultipleAds = async () => {
    const ids = [];

    // Load multiple ads
    for (let i = 0; i < 3; i++) {
      try {
        const response = await requestBannerAd({
          unitId: TestIds.BANNER,
          size: BannerAdSize.BANNER,
        });
        ids.push(response.requestId);
      } catch (error) {
        console.error(`Failed to load ad ${i}:`, error);
      }
    }

    setRequestIds(ids);
  };

  const checkAdExists = requestId => {
    return hasAd(requestId);
  };

  const cleanupAds = () => {
    requestIds.forEach(requestId => {
      if (hasAd(requestId)) {
        releaseAd(requestId);
        console.log(`Released ad: ${requestId}`);
      }
    });
    setRequestIds([]);
  };

  useEffect(() => {
    // Cleanup on unmount
    return () => {
      cleanupAds();
    };
  }, []);

  return (
    <View>
      {requestIds.map((requestId, index) => (
        <CachedBannerAd
          key={requestId}
          requestId={requestId}
          onAdLoaded={() => console.log(`Ad ${index} rendered`)}
        />
      ))}
    </View>
  );
};
```

### Re-rendering Reliability

The main benefit of cached ads is that they survive React re-renders:

```javascript
const ReliableAdComponent = () => {
  const [requestId, setRequestId] = useState(null);
  const [counter, setCounter] = useState(0);

  useEffect(() => {
    // Load ad once
    const loadAd = async () => {
      const response = await requestBannerAd({
        unitId: TestIds.BANNER,
        size: BannerAdSize.BANNER,
      });
      setRequestId(response.requestId);
    };

    loadAd();
  }, []); // Only load once

  return (
    <View>
      <Text>Counter: {counter}</Text>
      <Button title="Re-render" onPress={() => setCounter(c => c + 1)} />

      {/* This ad will reliably show the same ad instance even after re-renders */}
      {requestId && (
        <CachedBannerAd
          requestId={requestId}
          onAdLoaded={() => console.log('Ad still showing after re-render')}
        />
      )}
    </View>
  );
};
```

### Manual Impression Recording (GAM only)

```javascript
import React, { useRef } from 'react';

const GAMAdWithManualImpression = () => {
  const adRef = useRef(null);
  const [requestId, setRequestId] = useState(null);

  useEffect(() => {
    const loadAd = async () => {
      const response = await requestGAMBannerAd({
        unitId: TestIds.GAM_BANNER,
        sizes: [BannerAdSize.BANNER],
        manualImpressionsEnabled: true,
      });
      setRequestId(response.requestId);
    };

    loadAd();
  }, []);

  const recordImpression = () => {
    if (adRef.current) {
      adRef.current.recordManualImpression();
    }
  };

  return (
    <View>
      {requestId && (
        <CachedGAMBannerAd
          ref={adRef}
          requestId={requestId}
          onAdLoaded={() => console.log('GAM ad loaded')}
        />
      )}
      <Button title="Record Impression" onPress={recordImpression} />
    </View>
  );
};
```

## API Reference

### Request Functions

#### `requestBannerAd(params)`

Requests a regular banner ad.

**Parameters:**

- `unitId` (string): Ad unit ID
- `size` (BannerAdSize | string): Ad size
- `maxHeight` (number, optional): Maximum height for adaptive banners
- `width` (number, optional): Width for adaptive banners
- `requestOptions` (RequestOptions, optional): Ad request options

**Returns:** `Promise<AdRequestResponse>`

#### `requestGAMBannerAd(params)`

Requests a GAM banner ad.

**Parameters:**

- `unitId` (string): Ad unit ID
- `sizes` (Array<BannerAdSize | string>): Array of ad sizes
- `maxHeight` (number, optional): Maximum height for adaptive banners
- `width` (number, optional): Width for adaptive banners
- `requestOptions` (RequestOptions, optional): Ad request options
- `manualImpressionsEnabled` (boolean, optional): Enable manual impression recording

**Returns:** `Promise<AdRequestResponse>`

### Management Functions

#### `releaseAd(requestId)`

Releases an ad from memory.

**Parameters:**

- `requestId` (string): The request ID to release

#### `hasAd(requestId)`

Checks if an ad exists for the given request ID.

**Parameters:**

- `requestId` (string): The request ID to check

**Returns:** `boolean`

### Components

#### `<CachedBannerAd />`

Renders a cached banner ad.

**Props:**

- `requestId` (string): The request ID of the cached ad
- All standard banner ad event handlers (`onAdLoaded`, `onAdFailedToLoad`, etc.)

#### `<CachedGAMBannerAd />`

Renders a cached GAM banner ad.

**Props:**

- `requestId` (string): The request ID of the cached ad
- All standard GAM banner ad event handlers including `onAppEvent`
- `recordManualImpression()` method available via ref

## Best Practices

1. **Load ads early**: Request ads as soon as you know you'll need them
2. **Handle errors**: Always wrap ad requests in try-catch blocks
3. **Clean up**: Release ads when they're no longer needed to free memory
4. **Check existence**: Use `hasAd()` before rendering to avoid errors
5. **One request per render**: Don't request the same ad multiple times
6. **Memory management**: Release ads in component cleanup (useEffect return function)

## Migration from Regular Ads

To migrate from regular ads to cached ads:

1. Replace `<BannerAd />` with `<CachedBannerAd />`
2. Replace `<GAMBannerAd />` with `<CachedGAMBannerAd />`
3. Add ad request logic using `requestBannerAd()` or `requestGAMBannerAd()`
4. Pass the returned `requestId` to the component
5. Add proper cleanup with `releaseAd()`

This system provides better control over ad lifecycle and ensures reliable rendering across React re-renders.
