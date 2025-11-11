# Cached Banner Ad Fabric (New Architecture) Fix

## Problem

When trying to render the `CachedBannerAd` component on iOS with React Native's New Architecture (Fabric) enabled, the following error occurred:

```
Warning: Error: Exception in HostFunction: <unknown>

This error is located at:
    at RNGoogleMobileAdsCachedBannerView (<anonymous>)
    at CachedBaseAd.tsx
```

## Root Cause

The `CachedBannerAd` component was missing the proper Fabric (New Architecture) implementation on iOS. While the regular `BannerAd` component had complete Fabric support with proper ComponentView classes, the `CachedBannerAd` only had the old architecture implementation.

The error occurred because:

1. The codegen spec existed (`GoogleMobileAdsCachedBannerViewNativeComponent.ts`)
2. But the native iOS Fabric implementation was missing
3. The ViewManager had an incomplete Fabric wrapper that didn't properly extend `RCTViewComponentView`

## Solution

Created the missing Fabric implementation files for iOS:

### 1. Created `RNGoogleMobileAdsCachedBannerView.h`

- Header file for the Fabric ComponentView
- Extends `RCTViewComponentView` (required for Fabric)
- Implements `GADBannerViewDelegate` and `GADAppEventDelegate` protocols
- Defines the `requestId` property

### 2. Created `RNGoogleMobileAdsCachedBannerView.mm`

- Implementation file for the Fabric ComponentView
- Implements the ComponentDescriptorProvider for Fabric
- Handles props updates via `updateProps:oldProps:`
- Manages banner view lifecycle (setup, display, cleanup)
- Implements all GAD delegate methods with proper event emission using Fabric's event emitter
- Includes the `RNGoogleMobileAdsCachedBannerViewCls` function for component registration

### 3. Updated `RNGoogleMobileAdsCachedBannerViewManager.mm`

- Removed the incomplete Fabric wrapper
- Kept only the old architecture implementation (guarded with `#ifndef RCT_NEW_ARCH_ENABLED`)
- The Fabric implementation is now in the separate `.h` and `.mm` files

## Key Implementation Details

### Fabric Architecture Pattern

The implementation follows the same pattern as the regular `BannerView`:

- Uses `RCTViewComponentView` as the base class
- Implements `ComponentDescriptorProvider` for Fabric's component system
- Uses C++ event emitters for sending events to JavaScript
- Properly handles props through the `updateProps` method

### Banner View Management

- Retrieves cached banner views from `RNGoogleMobileAdsCachedBannerModule`
- Supports both fixed-size and fluid ads with appropriate layout strategies
- Properly sets up delegates and root view controller
- Handles view lifecycle (setup, display, cleanup, recycling)

### Event Handling

All banner events are properly forwarded to JavaScript using Fabric's event emitter:

- `onAdLoaded` - with width and height
- `onAdFailedToLoad` - with error code and message
- `onAdOpened`, `onAdClosed`, `onAdImpression`, `onAdClicked`
- `onAppEvent` - for GAM banner ads
- `onPaid` - for revenue tracking

## Files Modified/Created

### Created:

- `ios/RNGoogleMobileAds/RNGoogleMobileAdsCachedBannerView.h`
- `ios/RNGoogleMobileAds/RNGoogleMobileAdsCachedBannerView.mm`

### Modified:

- `ios/RNGoogleMobileAds/RNGoogleMobileAdsCachedBannerViewManager.mm`

## Testing

After applying this fix:

1. Clean the iOS build: `cd ios && rm -rf build && cd ..`
2. Reinstall pods: `cd ios && pod install && cd ..`
3. Rebuild the app
4. The `CachedBannerAd` component should now render without errors on iOS with Fabric enabled

## Notes

- The podspec automatically includes all `.h` and `.mm` files in the `ios` directory, so no podspec changes were needed
- The implementation is compatible with both old and new React Native architectures
- The fix maintains backward compatibility with the old architecture
