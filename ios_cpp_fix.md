# iOS C++ Standard Library Headers Fix - Root Cause Found

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

## Solution

**Convert Objective-C files to Objective-C++ files** by changing their extensions from `.m` to `.mm`:

1. `RNGoogleMobileAdsBannerComponent.m` → `RNGoogleMobileAdsBannerComponent.mm`
2. `RNGoogleMobileAdsCachedBannerComponent.m` → `RNGoogleMobileAdsCachedBannerComponent.mm`

## Why This Works

- **Objective-C++ (`.mm`)** files can compile both Objective-C and C++ code
- **Objective-C (`.m`)** files can only compile Objective-C code
- React Native's New Architecture includes C++ components that require C++ standard library headers
- When these headers are imported (even indirectly through React Native headers), the file must be compiled as Objective-C++

## Files Changed

The following files were renamed to fix the compilation issue:

```
ios/RNGoogleMobileAds/RNGoogleMobileAdsBannerComponent.m → .mm
ios/RNGoogleMobileAds/RNGoogleMobileAdsCachedBannerComponent.m → .mm
```

## Expected Result

After renaming these files to `.mm` extensions, the C++ standard library headers will be properly accessible during compilation, and the iOS build should succeed.

## Key Lesson

When working with React Native's New Architecture, any files that import React Native headers (directly or indirectly) should use the `.mm` extension to ensure they can handle C++ code that may be pulled in by the React Native framework.
