# iOS C++ Standard Library Headers Fix

## Problem

The iOS build is failing with missing C++ standard library headers:

- `'utility' file not found`
- `'optional' file not found`
- `'tuple' file not found`

## Root Cause

This issue occurs when React Native projects with the New Architecture (Fabric/TurboModules) don't have proper C++ standard library configuration. The errors typically happen because:

1. The C++ standard library version is not properly set
2. Header search paths are missing for C++ standard library
3. The project is not configured to use the correct C++ standard library (libc++)

## Solution

### 1. Update Podspec Configuration

The `RNGoogleMobileAds.podspec` already has some C++ configuration, but it needs to be enhanced:

```ruby
# In RNGoogleMobileAds.podspec, update the New Architecture section:
if ENV['RCT_NEW_ARCH_ENABLED'] == '1' then
  s.compiler_flags = folly_compiler_flags + " -DRCT_NEW_ARCH_ENABLED=1"
  s.pod_target_xcconfig = {
      "HEADER_SEARCH_PATHS" => "\"$(PODS_ROOT)/boost\"",
      "OTHER_CPLUSPLUSFLAGS" => "-DFOLLY_NO_CONFIG -DFOLLY_MOBILE=1 -DFOLLY_USE_LIBCPP=1",
      "CLANG_CXX_LANGUAGE_STANDARD" => "c++17",
      "CLANG_CXX_LIBRARY" => "libc++",
      "GCC_C_LANGUAGE_STANDARD" => "gnu11"
  }
  s.dependency "React-Codegen"
  s.dependency "RCT-Folly"
  s.dependency "RCTRequired"
  s.dependency "RCTTypeSafety"
  s.dependency "ReactCommon/turbomodule/core"
end
```

### 2. Fix in Consumer Project (LokalAppRN)

Since the errors are coming from `/Users/arunlodhi/playground/ghtpl/lokal-app/LokalAppRN/`, you need to apply these fixes in that project:

#### A. Update Podfile

Add these configurations to your `LokalAppRN/ios/Podfile`:

```ruby
# Add this at the top after platform declaration
platform :ios, '12.0'

# Add this configuration block
post_install do |installer|
  installer.pods_project.targets.each do |target|
    target.build_configurations.each do |config|
      # Ensure C++17 standard
      config.build_settings['CLANG_CXX_LANGUAGE_STANDARD'] = 'c++17'
      config.build_settings['CLANG_CXX_LIBRARY'] = 'libc++'

      # Add C++ standard library headers
      config.build_settings['OTHER_CPLUSPLUSFLAGS'] ||= []
      config.build_settings['OTHER_CPLUSPLUSFLAGS'] << '-DFOLLY_USE_LIBCPP=1'

      # Ensure proper header search paths
      config.build_settings['HEADER_SEARCH_PATHS'] ||= []
      config.build_settings['HEADER_SEARCH_PATHS'] << '$(PODS_ROOT)/boost'
      config.build_settings['HEADER_SEARCH_PATHS'] << '$(PODS_ROOT)/RCT-Folly'
    end
  end
end
```

#### B. Update iOS Deployment Target

Ensure your project supports iOS 12.0+ which is required for proper C++17 support:

In `LokalAppRN/ios/LokalAppRN.xcodeproj`, set:

- iOS Deployment Target: 12.0
- C++ Language Dialect: C++17
- C++ Standard Library: libc++

### 3. Clean and Rebuild

After making these changes:

```bash
# Navigate to your LokalAppRN project
cd /Users/arunlodhi/playground/ghtpl/lokal-app/LokalAppRN

# Clean iOS build
cd ios
rm -rf build/
rm -rf Pods/
rm -f Podfile.lock

# Reinstall pods
pod install --repo-update

# Clean React Native cache
cd ..
npx react-native start --reset-cache

# Rebuild iOS
npx react-native run-ios
```

### 4. Alternative: Xcode Project Settings

If the Podfile approach doesn't work, manually set these in Xcode:

1. Open `LokalAppRN.xcworkspace` in Xcode
2. Select your project target
3. Go to Build Settings
4. Set:
   - C++ Language Dialect: C++17
   - C++ Standard Library: libc++
   - Header Search Paths: Add `$(PODS_ROOT)/boost` and `$(PODS_ROOT)/RCT-Folly`

### 5. Verify React Native Version Compatibility

Ensure you're using compatible versions:

- React Native 0.68+ for New Architecture
- Xcode 13+ for proper C++17 support
- iOS 12.0+ deployment target

## Expected Result

After applying these fixes, the C++ standard library headers (`utility`, `optional`, `tuple`) should be found during compilation, and the iOS build should succeed.
