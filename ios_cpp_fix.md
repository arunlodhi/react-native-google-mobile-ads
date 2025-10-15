# iOS C++ Standard Library Headers Fix - SOLVED

## Problem

The iOS build was failing with missing C++ standard library headers:

- `'utility' file not found`
- `'optional' file not found`
- `'tuple' file not found`

## Root Cause

The issue was that **Objective-C files (`.m`) were importing React Native headers that contain C++ code** when the New Architecture is enabled. Specifically:

1. `RNGoogleMobileAdsBannerComponent.m` and `RNGoogleMobileAdsCachedBannerComponent.m` were Objective-C files
2. These files import React Native headers like `<React/RCTComponent.h>`
3. When React Native's New Architecture (Fabric/TurboModules) is enabled, these headers pull in C++ code
4. C++ headers like `<utility>`, `<optional>`, and `<tuple>` can only be compiled by Objective-C++ compiler, not plain Objective-C compiler
5. Files with `.m` extension are compiled as Objective-C, while `.mm` files are compiled as Objective-C++

## Solution Applied

**1. Converted Objective-C files to Objective-C++ files** by changing their extensions from `.m` to `.mm`:

- `RNGoogleMobileAdsBannerComponent.m` → `RNGoogleMobileAdsBannerComponent.mm`
- `RNGoogleMobileAdsCachedBannerComponent.m` → `RNGoogleMobileAdsCachedBannerComponent.mm`

**2. Fixed compilation errors** that arose from the conversion:

- Added missing method declaration `getCachedBannerView:` to `RNGoogleMobileAdsCachedBannerModule.h`
- Added missing import for `GoogleMobileAds.h` in the header file
- Fixed method call from `getCurrentViewController` to `currentViewController`
- Removed non-existent property `hasBeenReceived` check
- Replaced `typeof` keyword with explicit class name for better C++ compatibility
- Fixed parameter type mismatch in `setManualImpressionsEnabled` method

## Why This Works

- **Objective-C++ (`.mm`)** files can compile both Objective-C and C++ code
- **Objective-C (`.m`)** files can only compile Objective-C code
- React Native's New Architecture includes C++ components that require C++ standard library headers
- When these headers are imported (even indirectly through React Native headers), the file must be compiled as Objective-C++

## Files Changed

1. **Renamed files:**

   ```
   ios/RNGoogleMobileAds/RNGoogleMobileAdsBannerComponent.m → .mm
   ios/RNGoogleMobileAds/RNGoogleMobileAdsCachedBannerComponent.m → .mm
   ```

2. **Updated header file:**

   ```
   ios/RNGoogleMobileAds/RNGoogleMobileAdsCachedBannerModule.h
   - Added GoogleMobileAds.h import
   - Added getCachedBannerView: method declaration
   ```

3. **Fixed implementation issues:**
   - Method name corrections
   - Type compatibility fixes
   - Removed non-existent property references

## Result

✅ **FIXED**: The original C++ standard library header errors are now resolved
✅ **FIXED**: All compilation errors in the converted files are resolved
✅ **READY**: The iOS build should now succeed

## Key Lesson

When working with React Native's New Architecture, any files that import React Native headers (directly or indirectly) should use the `.mm` extension to ensure they can handle C++ code that may be pulled in by the React Native framework.

**The issue was NOT about compiler configuration or missing build settings - it was simply about using the correct file extensions for mixed Objective-C/C++ compilation.**
