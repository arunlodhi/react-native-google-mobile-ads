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

#import "RNGoogleMobileAdsRequestIdBannerComponent.h"
#import "RNGoogleMobileAdsRequestModule.h"
#import "RNGoogleMobileAdsCommon.h"

#if !TARGET_OS_MACCATALYST
#import <GoogleMobileAds/GoogleMobileAds.h>
#endif

@implementation RNGoogleMobileAdsRequestIdBannerComponent {
  GADBannerView *_bannerView;
  GAMBannerView *_gamBannerView;
  BOOL _requestIdChanged;
}

- (instancetype)init {
  if (self = [super init]) {
    _requestIdChanged = NO;
  }
  return self;
}

- (void)dealloc {
#if !TARGET_OS_MACCATALYST
  if (_bannerView) {
    _bannerView.delegate = nil;
    _bannerView.paidEventHandler = nil;
    [_bannerView removeFromSuperview];
    _bannerView = nil;
  }
  
  if (_gamBannerView) {
    _gamBannerView.delegate = nil;
    _gamBannerView.paidEventHandler = nil;
    _gamBannerView.appEventDelegate = nil;
    [_gamBannerView removeFromSuperview];
    _gamBannerView = nil;
  }
#endif
}

- (void)setRequestId:(NSString *)requestId {
  _requestId = requestId;
  _requestIdChanged = YES;
  [self requestAd];
}

- (void)requestAd {
#if TARGET_OS_MACCATALYST
  return;
#else
  if (!_requestId || !_requestIdChanged) {
    return;
  }
  
  _requestIdChanged = NO;
  
  // Clean up existing views
  if (_bannerView) {
    _bannerView.delegate = nil;
    _bannerView.paidEventHandler = nil;
    [_bannerView removeFromSuperview];
    _bannerView = nil;
  }
  
  if (_gamBannerView) {
    _gamBannerView.delegate = nil;
    _gamBannerView.paidEventHandler = nil;
    _gamBannerView.appEventDelegate = nil;
    [_gamBannerView removeFromSuperview];
    _gamBannerView = nil;
  }
  
  // Get the request module
  RNGoogleMobileAdsRequestModule *requestModule = [self.bridge moduleForClass:[RNGoogleMobileAdsRequestModule class]];
  if (!requestModule) {
    [self sendEvent:@"onAdFailedToLoad" body:@{@"code": @"request_module_not_found", @"message": @"Request module not found"}];
    return;
  }
  
  // Try to get banner view first
  _bannerView = [requestModule getBannerViewForRequestId:_requestId];
  if (_bannerView) {
    [self setupBannerView:_bannerView];
    return;
  }
  
  // Try to get GAM banner view
  _gamBannerView = [requestModule getGAMBannerViewForRequestId:_requestId];
  if (_gamBannerView) {
    [self setupGAMBannerView:_gamBannerView];
    return;
  }
  
  // No ad found for this request ID
  [self sendEvent:@"onAdFailedToLoad" body:@{@"code": @"ad_not_found", @"message": @"Ad not found for request ID"}];
#endif
}

#if !TARGET_OS_MACCATALYST
- (void)setupBannerView:(GADBannerView *)bannerView {
  bannerView.delegate = self;
  bannerView.paidEventHandler = ^(GADAdValue *_Nonnull adValue) {
    [self sendEvent:@"onPaid" body:@{
      @"currency": adValue.currencyCode,
      @"precision": @(adValue.precision),
      @"value": @(adValue.value.doubleValue / 1000000.0)
    }];
  };
  
  [self addSubview:bannerView];
  
  // Send loaded event
  CGSize size = bannerView.adSize.size;
  [self sendEvent:@"onAdLoaded" body:@{
    @"width": @(size.width),
    @"height": @(size.height)
  }];
}

- (void)setupGAMBannerView:(GAMBannerView *)gamBannerView {
  gamBannerView.delegate = self;
  gamBannerView.appEventDelegate = self;
  gamBannerView.paidEventHandler = ^(GADAdValue *_Nonnull adValue) {
    [self sendEvent:@"onPaid" body:@{
      @"currency": adValue.currencyCode,
      @"precision": @(adValue.precision),
      @"value": @(adValue.value.doubleValue / 1000000.0)
    }];
  };
  
  [self addSubview:gamBannerView];
  
  // Send loaded event
  CGSize size = gamBannerView.adSize.size;
  [self sendEvent:@"onAdLoaded" body:@{
    @"width": @(size.width),
    @"height": @(size.height)
  }];
}
#endif

- (void)recordManualImpression {
#if !TARGET_OS_MACCATALYST
  if (_gamBannerView) {
    [_gamBannerView recordImpression];
  }
#endif
}

- (void)sendEvent:(NSString *)type body:(NSDictionary *)body {
  if (!self.onNativeEvent) {
    return;
  }
  
  NSMutableDictionary *event = [[NSMutableDictionary alloc] init];
  [event setObject:type forKey:@"type"];
  
  if (body) {
    [event addEntriesFromDictionary:body];
  }
  
  self.onNativeEvent(event);
}

#if !TARGET_OS_MACCATALYST
#pragma mark - GADBannerViewDelegate

- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView {
  // Already handled in setup
}

- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error {
  [self sendEvent:@"onAdFailedToLoad" body:@{
    @"code": @(error.code),
    @"message": error.localizedDescription
  }];
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
  // Not used
}

- (void)bannerViewDidDismissScreen:(GADBannerView *)bannerView {
  [self sendEvent:@"onAdClosed" body:nil];
}

#pragma mark - GAMBannerAdLoaderDelegate

- (void)adLoader:(GADAdLoader *)adLoader didReceiveGAMBannerView:(GAMBannerView *)bannerView {
  // Not used in this context
}

- (void)adLoader:(GADAdLoader *)adLoader didFailToReceiveAdWithError:(NSError *)error {
  // Not used in this context
}

#pragma mark - GADAppEventDelegate

- (void)adView:(GADBannerView *)banner didReceiveAppEvent:(NSString *)name withInfo:(NSString *)info {
  [self sendEvent:@"onAppEvent" body:@{
    @"name": name,
    @"data": info ?: [NSNull null]
  }];
}
#endif

@end
