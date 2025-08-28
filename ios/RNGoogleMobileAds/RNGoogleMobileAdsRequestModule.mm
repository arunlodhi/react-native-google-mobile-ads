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

#import "RNGoogleMobileAdsRequestModule.h"
#import "RNGoogleMobileAdsCommon.h"

#if !TARGET_OS_MACCATALYST
#import <GoogleMobileAds/GoogleMobileAds.h>
#endif

@implementation RNGoogleMobileAdsRequestModule {
  NSMutableDictionary<NSString *, GADBannerView *> *_bannerAds;
  NSMutableDictionary<NSString *, GAMBannerView *> *_gamBannerAds;
}

RCT_EXPORT_MODULE(RNGoogleMobileAdsRequestModule);

- (instancetype)init {
  self = [super init];
  if (self) {
    _bannerAds = [[NSMutableDictionary alloc] init];
    _gamBannerAds = [[NSMutableDictionary alloc] init];
  }
  return self;
}

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(requestBannerAd:(NSString *)unitId
                  size:(NSString *)size
                  maxHeight:(NSNumber * _Nullable)maxHeight
                  width:(NSNumber * _Nullable)width
                  requestOptions:(NSString * _Nullable)requestOptions
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
#if TARGET_OS_MACCATALYST
  reject(@"unsupported", @"This operation is not supported on macOS", nil);
  return;
#else
  
  // Generate unique request ID
  NSString *requestId = [[NSUUID UUID] UUIDString];
  
  // Create banner view
  GADBannerView *bannerView = [[GADBannerView alloc] init];
  bannerView.adUnitID = unitId;
  
  // Set ad size
  GADAdSize adSize = [RNGoogleMobileAdsCommon getAdSize:size maxHeight:maxHeight width:width];
  bannerView.adSize = adSize;
  
  // Create ad request
  GADRequest *request = [GADRequest request];
  if (requestOptions && ![requestOptions isEqualToString:@"{}"]) {
    NSData *data = [requestOptions dataUsingEncoding:NSUTF8StringEncoding];
    NSError *error;
    NSDictionary *requestDict = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    if (!error && requestDict) {
      request = [RNGoogleMobileAdsCommon buildAdRequest:requestDict];
    }
  }
  
  // Set completion handler
  [bannerView loadRequest:request];
  
  // Store the banner view
  [_bannerAds setObject:bannerView forKey:requestId];
  
  // Set up load completion handler
  __weak typeof(self) weakSelf = self;
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (strongSelf) {
      // For now, we'll resolve immediately with dimensions
      // In a real implementation, you'd wait for the ad to load
      CGSize size = bannerView.adSize.size;
      NSDictionary *result = @{
        @"requestId": requestId,
        @"width": @(size.width),
        @"height": @(size.height)
      };
      resolve(result);
    }
  });
#endif
}

RCT_EXPORT_METHOD(requestGAMBannerAd:(NSString *)unitId
                  sizes:(NSArray<NSString *> *)sizes
                  maxHeight:(NSNumber * _Nullable)maxHeight
                  width:(NSNumber * _Nullable)width
                  requestOptions:(NSString * _Nullable)requestOptions
                  manualImpressionsEnabled:(NSNumber * _Nullable)manualImpressionsEnabled
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
#if TARGET_OS_MACCATALYST
  reject(@"unsupported", @"This operation is not supported on macOS", nil);
  return;
#else
  
  // Generate unique request ID
  NSString *requestId = [[NSUUID UUID] UUIDString];
  
  // Create GAM banner view
  GAMBannerView *gamBannerView = [[GAMBannerView alloc] init];
  gamBannerView.adUnitID = unitId;
  
  // Set ad sizes
  NSMutableArray<NSValue *> *adSizes = [[NSMutableArray alloc] init];
  for (NSString *sizeString in sizes) {
    GADAdSize adSize = [RNGoogleMobileAdsCommon getAdSize:sizeString maxHeight:maxHeight width:width];
    [adSizes addObject:NSValueFromGADAdSize(adSize)];
  }
  gamBannerView.validAdSizes = adSizes;
  
  // Set manual impressions if enabled
  if (manualImpressionsEnabled && [manualImpressionsEnabled boolValue]) {
    gamBannerView.enableManualImpressions = YES;
  }
  
  // Create ad request
  GAMRequest *request = [GAMRequest request];
  if (requestOptions && ![requestOptions isEqualToString:@"{}"]) {
    NSData *data = [requestOptions dataUsingEncoding:NSUTF8StringEncoding];
    NSError *error;
    NSDictionary *requestDict = [NSJSONSerialization JSONObjectWithData:data options:0 error:&error];
    if (!error && requestDict) {
      request = [RNGoogleMobileAdsCommon buildGAMRequest:requestDict];
    }
  }
  
  // Set completion handler
  [gamBannerView loadRequest:request];
  
  // Store the GAM banner view
  [_gamBannerAds setObject:gamBannerView forKey:requestId];
  
  // Set up load completion handler
  __weak typeof(self) weakSelf = self;
  dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(0.1 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
    __strong typeof(weakSelf) strongSelf = weakSelf;
    if (strongSelf) {
      // For now, we'll resolve immediately with dimensions
      // In a real implementation, you'd wait for the ad to load
      GADAdSize adSize = gamBannerView.adSize;
      CGSize size = adSize.size;
      NSDictionary *result = @{
        @"requestId": requestId,
        @"width": @(size.width),
        @"height": @(size.height)
      };
      resolve(result);
    }
  });
#endif
}

RCT_EXPORT_METHOD(releaseAd:(NSString *)requestId) {
#if !TARGET_OS_MACCATALYST
  // Remove from banner ads
  GADBannerView *bannerView = [_bannerAds objectForKey:requestId];
  if (bannerView) {
    [_bannerAds removeObjectForKey:requestId];
    return;
  }
  
  // Remove from GAM banner ads
  GAMBannerView *gamBannerView = [_gamBannerAds objectForKey:requestId];
  if (gamBannerView) {
    [_gamBannerAds removeObjectForKey:requestId];
    return;
  }
#endif
}

RCT_EXPORT_SYNCHRONOUS_TYPED_METHOD(BOOL, hasAd:(NSString *)requestId) {
#if TARGET_OS_MACCATALYST
  return NO;
#else
  return [_bannerAds objectForKey:requestId] != nil || [_gamBannerAds objectForKey:requestId] != nil;
#endif
}

// Method to get banner view by request ID (for use by view manager)
- (GADBannerView *)getBannerViewForRequestId:(NSString *)requestId {
#if TARGET_OS_MACCATALYST
  return nil;
#else
  return [_bannerAds objectForKey:requestId];
#endif
}

// Method to get GAM banner view by request ID (for use by view manager)
- (GAMBannerView *)getGAMBannerViewForRequestId:(NSString *)requestId {
#if TARGET_OS_MACCATALYST
  return nil;
#else
  return [_gamBannerAds objectForKey:requestId];
#endif
}

@end
