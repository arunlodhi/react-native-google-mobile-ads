# Cached Banner Ads Usage Guide

This guide demonstrates how to use the new Cached Banner Ads functionality in react-native-google-mobile-ads.

## Overview

Cached Banner Ads allow you to pre-load banner ads and reuse them across React re-renders, ensuring consistent ad display even when components unmount and remount.

## Basic Usage

### 1. Request a Cached Banner Ad

```javascript
import { requestGAMBannerAd, BannerAdSize, TestIds } from 'react-native-google-mobile-ads';

// Request a cached GAM banner ad
const requestCachedAd = async () => {
  try {
    const adInfo = await requestGAMBannerAd({
      unitId: TestIds.GAM_BANNER,
      sizes: [BannerAdSize.BANNER],
      requestOptions: {
        requestNonPersonalizedAdsOnly: true,
      },
    });

    console.log('Cached ad loaded:', adInfo);
    // adInfo contains: { requestId, unitId, isLoaded, width, height }
    // The requestId is automatically generated and returned
  } catch (error) {
    console.error('Failed to load cached ad:', error);
  }
};
```

### 2. Display the Cached Ad

```javascript
import React, { useState, useEffect } from 'react';
import { View } from 'react-native';
import { CachedGAMBannerAd, requestGAMBannerAd } from 'react-native-google-mobile-ads';

const MyComponent = () => {
  const [requestId, setRequestId] = useState(null);

  useEffect(() => {
    const loadAd = async () => {
      try {
        const adInfo = await requestGAMBannerAd({
          unitId: TestIds.GAM_BANNER,
          sizes: [BannerAdSize.BANNER],
        });
        setRequestId(adInfo.requestId);
      } catch (error) {
        console.error('Failed to load ad:', error);
      }
    };

    loadAd();
  }, []);

  if (!requestId) {
    return <View>Loading ad...</View>;
  }

  return (
    <View>
      <CachedGAMBannerAd
        requestId={requestId}
        onAdLoaded={dimensions => {
          console.log('Ad displayed with dimensions:', dimensions);
        }}
        onAdFailedToLoad={error => {
          console.error('Ad failed to display:', error);
        }}
        onAdClicked={() => {
          console.log('Ad clicked');
        }}
        onPaid={revenue => {
          console.log('Ad generated revenue:', revenue);
        }}
      />
    </View>
  );
};
```

## Advanced Usage

### Managing Multiple Cached Ads

```javascript
import {
  requestGAMBannerAd,
  getCachedAdInfo,
  removeCachedAd,
  getAllCachedAdIds,
  clearAllCachedAds,
} from 'react-native-google-mobile-ads';

// Request multiple ads
const requestMultipleAds = async () => {
  const adRequests = [
    { name: 'home-banner', unitId: 'ca-app-pub-xxx/home' },
    { name: 'profile-banner', unitId: 'ca-app-pub-xxx/profile' },
    { name: 'settings-banner', unitId: 'ca-app-pub-xxx/settings' },
  ];

  const adInfos = {};

  for (const { name, unitId } of adRequests) {
    try {
      const adInfo = await requestGAMBannerAd({
        unitId,
        sizes: [BannerAdSize.BANNER],
      });
      adInfos[name] = adInfo; // Store the returned adInfo with generated requestId
      console.log(`Loaded ad ${name} with requestId: ${adInfo.requestId}`);
    } catch (error) {
      console.error(`Failed to load ad ${name}:`, error);
    }
  }

  return adInfos;
};

// Check cached ad status
const checkAdStatus = async requestId => {
  try {
    const adInfo = await getCachedAdInfo(requestId);
    console.log(`Ad ${requestId} status:`, adInfo);
  } catch (error) {
    console.log(`Ad ${requestId} not found`);
  }
};

// Clean up specific ad
const cleanupAd = async requestId => {
  try {
    await removeCachedAd(requestId);
    console.log(`Removed cached ad: ${requestId}`);
  } catch (error) {
    console.error('Failed to remove ad:', error);
  }
};

// Get all cached ad IDs
const listAllCachedAds = async () => {
  try {
    const adIds = await getAllCachedAdIds();
    console.log('All cached ads:', adIds);
  } catch (error) {
    console.error('Failed to get cached ads:', error);
  }
};

// Clear all cached ads
const clearAllAds = async () => {
  try {
    await clearAllCachedAds();
    console.log('All cached ads cleared');
  } catch (error) {
    console.error('Failed to clear ads:', error);
  }
};
```

### Regular Banner Ads (Non-GAM)

```javascript
import { requestBannerAd, CachedBannerAd, BannerAdSize } from 'react-native-google-mobile-ads';

const RegularBannerComponent = () => {
  const [requestId, setRequestId] = useState(null);

  useEffect(() => {
    const loadAd = async () => {
      try {
        const adInfo = await requestBannerAd({
          unitId: 'ca-app-pub-xxx/banner',
          size: BannerAdSize.BANNER,
          requestOptions: {
            requestNonPersonalizedAdsOnly: true,
          },
        });

        console.log('Regular banner ad loaded:', adInfo);
        setRequestId(adInfo.requestId);
      } catch (error) {
        console.error('Failed to load regular banner ad:', error);
      }
    };

    loadAd();
  }, []);

  if (!requestId) {
    return <View>Loading ad...</View>;
  }

  return (
    <CachedBannerAd
      requestId={requestId}
      onAdLoaded={dimensions => {
        console.log('Regular banner ad displayed:', dimensions);
      }}
      onAdFailedToLoad={error => {
        console.error('Regular banner ad failed:', error);
      }}
    />
  );
};
```

## Best Practices

### 1. Pre-load Ads Early

```javascript
// Load ads when app starts or when navigating to a section
useEffect(() => {
  const preloadAds = async () => {
    // Pre-load ads for screens user is likely to visit
    const adInfo = await requestGAMBannerAd({
      unitId: TestIds.GAM_BANNER,
      sizes: [BannerAdSize.BANNER],
    });

    // Store the requestId for later use
    console.log('Pre-loaded ad with requestId:', adInfo.requestId);
  };

  preloadAds();
}, []);
```

### 2. Handle Ad Lifecycle

```javascript
const AdManager = () => {
  const [requestId, setRequestId] = useState(null);

  useEffect(() => {
    // Load ads on mount
    const loadAds = async () => {
      try {
        const adInfo = await requestGAMBannerAd({
          unitId: TestIds.GAM_BANNER,
          sizes: [BannerAdSize.BANNER],
        });
        setRequestId(adInfo.requestId);
      } catch (error) {
        console.error('Failed to load ad:', error);
      }
    };

    loadAds();

    // Cleanup on unmount
    return () => {
      if (requestId) {
        removeCachedAd(requestId).catch(console.error);
      }
    };
  }, [requestId]);

  if (!requestId) {
    return <View>Loading ad...</View>;
  }

  return (
    <CachedGAMBannerAd
      requestId={requestId}
      onAdLoaded={() => console.log('Ad loaded')}
      onAdFailedToLoad={error => console.error('Ad failed:', error)}
    />
  );
};
```

### 3. Error Handling

```javascript
const RobustAdComponent = ({ requestId }) => {
  const [adLoaded, setAdLoaded] = useState(false);
  const [adError, setAdError] = useState(null);

  return (
    <View>
      {!adLoaded && !adError && <Text>Loading ad...</Text>}
      {adError && <Text>Ad failed to load</Text>}

      <CachedGAMBannerAd
        requestId={requestId}
        onAdLoaded={() => {
          setAdLoaded(true);
          setAdError(null);
        }}
        onAdFailedToLoad={error => {
          setAdLoaded(false);
          setAdError(error);
        }}
      />
    </View>
  );
};
```

## Key Benefits

1. **Consistent Ad Display**: Ads remain visible even during React re-renders
2. **Improved Performance**: Pre-loaded ads display instantly
3. **Better User Experience**: No flickering or loading delays
4. **Memory Efficient**: Reuse the same ad instance across components
5. **Reliable Revenue**: Ads stay loaded and continue generating revenue

## API Reference

### Functions

- `requestBannerAd(options)` - Request a cached regular banner ad. Returns a Promise that resolves to `{ requestId, unitId, isLoaded, width, height }`
- `requestGAMBannerAd(options)` - Request a cached GAM banner ad. Returns a Promise that resolves to `{ requestId, unitId, isLoaded, width, height }`
- `getCachedAdInfo(requestId)` - Get information about a cached ad
- `removeCachedAd(requestId)` - Remove a specific cached ad
- `getAllCachedAdIds()` - Get all cached ad request IDs
- `clearAllCachedAds()` - Remove all cached ads

#### Function Parameters

**requestBannerAd(options)**

- `options.unitId` (string) - The ad unit ID
- `options.size` (BannerAdSize) - The banner ad size
- `options.requestOptions` (object, optional) - Ad request options

**requestGAMBannerAd(options)**

- `options.unitId` (string) - The ad unit ID
- `options.sizes` (BannerAdSize[]) - Array of valid ad sizes
- `options.requestOptions` (object, optional) - Ad request options

### Components

- `<CachedBannerAd requestId="..." />` - Display a cached regular banner ad
- `<CachedGAMBannerAd requestId="..." />` - Display a cached GAM banner ad

All components support the same event handlers as regular banner ads (`onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onPaid`, etc.). GAM banner ads additionally support `onAppEvent` for Ad Manager specific app events.

### Key Changes from Previous API

- **Automatic requestId generation**: The `requestId` is now automatically generated by the native side and returned in the response
- **Fresh ads for each request**: Each function call creates a new ad with a unique requestId
- **Simplified caching**: Caching is only used to link requestIds to ad views for component reuse
