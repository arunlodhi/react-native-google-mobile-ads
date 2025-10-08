# Frame Layout Based Rendering Improvements

This document outlines the improvements made to the React Native Google Mobile Ads cached banner implementation, inspired by the Lokal Android app's banner ad rendering approach.

## Problem Statement

The original cached banner ads implementation had issues with:

1. **Parent attachment conflicts** - Ad views being reused across different React components caused "child already has a parent" errors
2. **Missing size calculation logic** - No proper ad view dimension calculation or dynamic size change handling
3. **Direct ad view attachment** - Ad views were directly attached to React view groups without proper isolation

## Solution: Frame Layout Container Approach

### Key Improvements

#### 1. Frame Layout Container Isolation

**Inspired by**: `lokal.libraries.common.ui.views.BottomBannerAdView.kt`

- **Before**: Ad views were directly attached to React view groups
- **After**: Ad views are wrapped in `FrameLayout` containers for better isolation

```java
// Create a FrameLayout container (inspired by Lokal's BottomBannerAdView approach)
FrameLayout adContainer = new FrameLayout(reactViewGroup.getContext());

// Add the cached ad view to the container
adContainer.addView(cachedAdView);

// Add the container to the React view group
reactViewGroup.addView(adContainer);
```

**Benefits**:

- Prevents parent attachment conflicts
- Provides better view hierarchy isolation
- Matches the approach used in production Lokal Android app

#### 2. Enhanced Size Calculation Logic

**Inspired by**: Lokal's `GAMUnifiedAdHelper.kt` and `AdsUtils2.kt`

- **Proper AdSize handling**: Uses the same logic as the regular banner implementation
- **View dimension prioritization**: Prioritizes actual view dimensions over theoretical AdSize dimensions
- **Fluid ad support**: Handles fluid ads with layout change listeners

```kotlin
// Use view dimensions if they are non-zero and different from AdSize dimensions
// This handles cases where the actual ad content has different dimensions than the AdSize spec
if (viewWidth > 0 && viewHeight > 0 && (viewWidth != adSizeWidth || viewHeight != adSizeHeight)) {
    width = viewWidth
    height = viewHeight
} else {
    // Fallback to AdSize dimensions if view dimensions are zero or match AdSize
    width = adSizeWidth
    height = adSizeHeight
}
```

#### 3. Dynamic Size Change Monitoring

**Inspired by**: Lokal's layout change listener approach

- **Layout change listeners**: Added to handle dynamic size updates after initial load
- **Cached info updates**: Automatically updates cached ad info when dimensions change

```kotlin
// Add layout change listener for dynamic size updates (inspired by Lokal's approach)
adView.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
    val newWidth = right - left
    val newHeight = bottom - top
    val oldWidth = oldRight - oldLeft
    val oldHeight = oldBottom - oldTop

    if (newWidth != oldWidth || newHeight != oldHeight) {
        // Update cached ad info with new dimensions
        val updatedAdInfoData = mapOf(
            "requestId" to requestId,
            "unitId" to unitId,
            "isGAM" to isGAM,
            "sizesString" to sizesString,
            "isLoaded" to true,
            "width" to newWidthDp,
            "height" to newHeightDp
        )
        cachedAdInfo[requestId] = updatedAdInfoData
    }
}
```

#### 4. Improved View Cleanup

**Inspired by**: Lokal's comprehensive cleanup approach

- **Container-aware cleanup**: Properly handles both direct ad views and container-wrapped ad views
- **Listener cleanup**: Ensures all event listeners are properly removed
- **Recursive cleanup**: Cleans up nested view hierarchies

```java
private void clearExistingAdView(ReactNativeAdView reactViewGroup) {
    // Remove all children (including frame layout containers)
    for (int i = reactViewGroup.getChildCount() - 1; i >= 0; i--) {
        android.view.View child = reactViewGroup.getChildAt(i);

        // If it's a FrameLayout container, clean up the ad view inside it first
        if (child instanceof FrameLayout) {
            FrameLayout container = (FrameLayout) child;
            if (container.getChildCount() > 0) {
                android.view.View adViewChild = container.getChildAt(0);
                if (adViewChild instanceof BaseAdView) {
                    BaseAdView adView = (BaseAdView) adViewChild;
                    cleanupAdViewListeners(adView);
                }
            }
            container.removeAllViews();
        }

        reactViewGroup.removeViewAt(i);
    }
}
```

## Files Modified

### 1. ReactNativeGoogleMobileAdsCachedBannerViewManager.java

- Added `attachCachedAdViewWithFrameLayout()` method
- Enhanced `clearExistingAdView()` for container-aware cleanup
- Updated `getCachedAdView()` to handle both direct and container-wrapped ad views
- Added comprehensive error handling and logging

### 2. ReactNativeGoogleMobileAdsCachedBannerModule.kt

- Enhanced size calculation logic in `onAdLoaded()` callback
- Added layout change listener for dynamic size updates
- Improved ad view resizing logic
- Added comprehensive dimension logging for debugging

## Technical Benefits

1. **Reliability**: Eliminates parent attachment conflicts that caused crashes
2. **Consistency**: Matches the proven approach used in the Lokal Android production app
3. **Flexibility**: Handles various ad sizes and dynamic size changes
4. **Maintainability**: Clear separation of concerns with container-based architecture
5. **Performance**: Proper cleanup prevents memory leaks

## Backward Compatibility

The improvements maintain full backward compatibility:

- Existing cached banner ad usage continues to work unchanged
- API remains the same
- Only internal implementation has been enhanced

## Testing Recommendations

1. **Multi-component reuse**: Test the same cached ad being displayed in multiple React components
2. **Size variations**: Test with different ad sizes (banner, large banner, medium rectangle, etc.)
3. **Fluid ads**: Test with fluid ad sizes that change dynamically
4. **Memory testing**: Verify no memory leaks during component mount/unmount cycles
5. **Error scenarios**: Test error handling when ads fail to load or attach

## Future Enhancements

1. **Size change events**: Consider emitting size change events to React Native for dynamic layout updates
2. **Container customization**: Allow customization of the container layout parameters
3. **Performance metrics**: Add performance tracking for ad attachment and rendering times
