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

@interface RNGoogleMobileAdsCachedBannerComponent () <GADBannerViewDelegate>
@property(nonatomic, strong) GADBannerView *bannerView;
@property(nonatomic, strong) RNGoogleMobileAdsCachedBannerModule *cachedBannerModule;
@end

@implementation RNGoogleMobileAdsCachedBannerComponent

- (instancetype)initWithFrame:(CGRect)frame {
  self = [super initWithFrame:frame];
  if (self) {
    // Get reference to the cached banner module
    _cachedBannerModule = [RNGoogleMobileAdsCachedBannerModule new];
  }
  return self;
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
    self.bannerView.rootViewController = [RNGoogleMobileAdsCommon getCurrentViewController];
    
    // Add to view hierarchy
    [self addSubview:self.bannerView];
    
    // Set constraints to fill the container
    self.bannerView.translatesAutoresizingMaskIntoConstraints = NO;
    [NSLayoutConstraint activateConstraints:@[
      [self.bannerView.centerXAnchor constraintEqualToAnchor:self.centerXAnchor],
      [self.bannerView.centerYAnchor constraintEqualToAnchor:self.centerYAnchor],
      [self.bannerView.widthAnchor constraintLessThanOrEqualToAnchor:self.widthAnchor],
      [self.bannerView.heightAnchor constraintLessThanOrEqualToAnchor:self.heightAnchor]
    ]];
    
    // Send loaded event if ad is already loaded
    if (self.bannerView.hasBeenReceived) {
      [self sendEvent:@"onAdLoaded" body:@{
        @"width": @(self.bannerView.bounds.size.width),
        @"height": @(self.bannerView.bounds.size.height)
      }];
    }
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
