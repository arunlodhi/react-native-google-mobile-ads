//
/**
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

#import "RNGoogleMobileAdsCachedBannerComponent.h"
#import "RNGoogleMobileAdsCachedBannerModule.h"
#import "RNGoogleMobileAdsCommon.h"

#if !TARGET_OS_MACCATALYST
#import <GoogleMobileAds/GoogleMobileAds.h>
#endif

#import <React/RCTBridge.h>

// Static reference to the module instance
static RNGoogleMobileAdsCachedBannerModule *_sharedModuleInstance = nil;

@interface RNGoogleMobileAdsCachedBannerComponent () <GADBannerViewDelegate>
@property(nonatomic, strong) GADBannerView *bannerView;
@end

@implementation RNGoogleMobileAdsCachedBannerComponent

+ (void)setSharedModuleInstance:(RNGoogleMobileAdsCachedBannerModule *)module {
  _sharedModuleInstance = module;
}

+ (RNGoogleMobileAdsCachedBannerModule *)sharedModuleInstance {
  return _sharedModuleInstance;
}

- (instancetype)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    // Module will be accessed via static reference or bridge
  }
  return self;
}

- (RNGoogleMobileAdsCachedBannerModule *)cachedBannerModule {
  // Try to get from static reference first (works in both architectures)
  if (_sharedModuleInstance) {
    return _sharedModuleInstance;
  }
  
  // Fallback to bridge for Old Architecture
  if (self.bridge) {
    RNGoogleMobileAdsCachedBannerModule *module = [self.bridge moduleForClass:[RNGoogleMobileAdsCachedBannerModule class]];
    // Cache it for future use
    if (module) {
      _sharedModuleInstance = module;
    }
    return module;
  }
  
  return nil;
}

- (void)setRequestId:(NSString *)requestId {
  if (_requestId != requestId) {
    _requestId = requestId;
    [self setupBannerView];
  }
}

- (void)setupBannerView {
#if !TARGET_OS_MACCATALYST
  if (!self.requestId) {
    return;
  }
  
  // Remove existing banner view
  if (self.bannerView) {
    [self.bannerView removeFromSuperview];
    self.bannerView = nil;
  }
  
  // Get cached banner view from module
  GADBannerView *cachedBannerView = [self.cachedBannerModule getCachedBannerView:self.requestId];
  
  if (cachedBannerView) {
    self.bannerView = cachedBannerView;
    self.bannerView.delegate = self;
    self.bannerView.rootViewController = [RNGoogleMobileAdsCommon currentViewController];
    
    // Check if this is a fluid ad and handle layout accordingly
    GADAdSize adSize = self.bannerView.adSize;
    BOOL isFluid = GADAdSizeEqualToSize(adSize, GADAdSizeFluid);
    
    if (isFluid) {
      // For fluid ads, use frame-based layout with autoresizing mask (same as regular banner)
      self.bannerView.frame = self.bounds;
      self.bannerView.autoresizingMask = UIViewAutoresizingFlexibleWidth;
      self.bannerView.translatesAutoresizingMaskIntoConstraints = YES;
    } else {
      // For fixed-size ads, use constraints
      self.bannerView.translatesAutoresizingMaskIntoConstraints = NO;
      [NSLayoutConstraint activateConstraints:@[
        [self.bannerView.centerXAnchor constraintEqualToAnchor:self.centerXAnchor],
        [self.bannerView.centerYAnchor constraintEqualToAnchor:self.centerYAnchor],
        [self.bannerView.widthAnchor constraintLessThanOrEqualToAnchor:self.widthAnchor],
        [self.bannerView.heightAnchor constraintLessThanOrEqualToAnchor:self.heightAnchor]
      ]];
    }
    
    // Add to view hierarchy
    [self addSubview:self.bannerView];
    
    // Send loaded event - the delegate will handle this when ad loads
  }
#endif
}

- (void)recordManualImpression {
#if !TARGET_OS_MACCATALYST
  if ([self.bannerView isKindOfClass:[GAMBannerView class]]) {
    [(GAMBannerView *)self.bannerView recordImpression];
  }
#endif
}

- (void)sendEvent:(NSString *)type body:(NSDictionary *)body {
  if (!self.onNativeEvent) {
    return;
  }
  
  NSMutableDictionary *event = [[NSMutableDictionary alloc] init];
  event[@"type"] = type;
  
  if (body) {
    [event addEntriesFromDictionary:body];
  }
  
  self.onNativeEvent(event);
}

#pragma mark - GADBannerViewDelegate

- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView {
  [self sendEvent:@"onAdLoaded" body:@{
    @"width": @(bannerView.bounds.size.width),
    @"height": @(bannerView.bounds.size.height)
  }];
}

- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error {
  NSDictionary *errorInfo = [RNGoogleMobileAdsCommon getCodeAndMessageFromAdError:error];
  [self sendEvent:@"onAdFailedToLoad" body:errorInfo];
}

- (void)bannerViewDidRecordImpression:(GADBannerView *)bannerView {
  [self sendEvent:@"onAdImpression" body:nil];
}

- (void)bannerViewDidRecordClick:(GADBannerView *)bannerView {
  [self sendEvent:@"onAdClicked" body:nil];
}

- (void)bannerViewWillPresentScreen:(GADBannerView *)bannerView {
  [self sendEvent:@"onAdOpened" body:nil];
}

- (void)bannerViewWillDismissScreen:(GADBannerView *)bannerView {
  // This method is called when the banner is about to dismiss a modal screen
}

- (void)bannerViewDidDismissScreen:(GADBannerView *)bannerView {
  [self sendEvent:@"onAdClosed" body:nil];
}

@end
