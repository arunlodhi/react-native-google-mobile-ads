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
    const adInfo = await requestGAMBannerAd('my-unique-request-id', {
      unitId: TestIds.GAM_BANNER,
      sizes: [BannerAdSize.BANNER],
      requestOptions: {
        requestNonPersonalizedAdsOnly: true,
      },
    });

    console.log('Cached ad loaded:', adInfo);
    // adInfo contains: { requestId, unitId, isLoaded, width, height }
  } catch (error) {
    console.error('Failed to load cached ad:', error);
  }
};
```

### 2. Display the Cached Ad

```javascript
import React from 'react';
import { View } from 'react-native';
import { CachedGAMBannerAd } from 'react-native-google-mobile-ads';

const MyComponent = () => {
  return (
    <View>
      <CachedGAMBannerAd
        requestId="my-unique-request-id"
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
    { requestId: 'home-banner', unitId: 'ca-app-pub-xxx/home' },
    { requestId: 'profile-banner', unitId: 'ca-app-pub-xxx/profile' },
    { requestId: 'settings-banner', unitId: 'ca-app-pub-xxx/settings' },
  ];

  for (const { requestId, unitId } of adRequests) {
    try {
      await requestGAMBannerAd(requestId, {
        unitId,
        sizes: [BannerAdSize.BANNER],
      });
    } catch (error) {
      console.error(`Failed to load ad ${requestId}:`, error);
    }
  }
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

const requestRegularBannerAd = async () => {
  try {
    const adInfo = await requestBannerAd('regular-banner-id', {
      unitId: 'ca-app-pub-xxx/banner',
      size: BannerAdSize.BANNER,
      requestOptions: {
        requestNonPersonalizedAdsOnly: true,
      },
    });

    console.log('Regular banner ad loaded:', adInfo);
  } catch (error) {
    console.error('Failed to load regular banner ad:', error);
  }
};

// Display the cached regular banner ad
const RegularBannerComponent = () => (
  <CachedBannerAd
    requestId="regular-banner-id"
    onAdLoaded={dimensions => {
      console.log('Regular banner ad displayed:', dimensions);
    }}
    onAdFailedToLoad={error => {
      console.error('Regular banner ad failed:', error);
    }}
  />
);
```

## Best Practices

### 1. Pre-load Ads Early

```javascript
// Load ads when app starts or when navigating to a section
useEffect(() => {
  const preloadAds = async () => {
    // Pre-load ads for screens user is likely to visit
    await requestGAMBannerAd('home-banner', {
      unitId: TestIds.GAM_BANNER,
      sizes: [BannerAdSize.BANNER],
    });
  };

  preloadAds();
}, []);
```

### 2. Handle Ad Lifecycle

```javascript
const AdManager = () => {
  useEffect(() => {
    // Load ads on mount
    const loadAds = async () => {
      try {
        await requestGAMBannerAd('screen-banner', {
          unitId: TestIds.GAM_BANNER,
          sizes: [BannerAdSize.BANNER],
        });
      } catch (error) {
        console.error('Failed to load ad:', error);
      }
    };

    loadAds();

    // Cleanup on unmount
    return () => {
      removeCachedAd('screen-banner').catch(console.error);
    };
  }, []);

  return (
    <CachedGAMBannerAd
      requestId="screen-banner"
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

- `requestBannerAd(requestId, options)` - Request a cached regular banner ad
- `requestGAMBannerAd(requestId, options)` - Request a cached GAM banner ad
- `getCachedAdInfo(requestId)` - Get information about a cached ad
- `removeCachedAd(requestId)` - Remove a specific cached ad
- `getAllCachedAdIds()` - Get all cached ad request IDs
- `clearAllCachedAds()` - Remove all cached ads

### Components

- `<CachedBannerAd requestId="..." />` - Display a cached regular banner ad
- `<CachedGAMBannerAd requestId="..." />` - Display a cached GAM banner ad

All components support the same event handlers as regular banner ads (`onAdLoaded`, `onAdFailedToLoad`, `onAdClicked`, `onPaid`, etc.). GAM banner ads additionally support `onAppEvent` for Ad Manager specific app events.
