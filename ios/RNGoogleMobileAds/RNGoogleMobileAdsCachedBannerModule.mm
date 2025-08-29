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

#import "RNGoogleMobileAdsCachedBannerModule.h"
#import "RNGoogleMobileAdsCommon.h"

#if !TARGET_OS_MACCATALYST
#import <GoogleMobileAds/GoogleMobileAds.h>
#endif

@interface RNGoogleMobileAdsCachedBannerModule ()
@property(nonatomic, strong) NSMutableDictionary<NSString *, GADBannerView *> *cachedBannerAds;
@property(nonatomic, strong) NSMutableDictionary<NSString *, NSDictionary *> *cachedAdInfo;
@end

@implementation RNGoogleMobileAdsCachedBannerModule

RCT_EXPORT_MODULE(RNGoogleMobileAdsCachedBannerModule);

- (instancetype)init {
  self = [super init];
  if (self) {
    _cachedBannerAds = [[NSMutableDictionary alloc] init];
    _cachedAdInfo = [[NSMutableDictionary alloc] init];
  }
  return self;
}

+ (BOOL)requiresMainQueueSetup {
  return NO;
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(requestCachedBannerAd:(NSDictionary *)config
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
#if TARGET_OS_MACCATALYST
  reject(@"unsupported", @"Cached banner ads are not supported on macOS.", nil);
  return;
#else
  NSString *requestId = config[@"requestId"];
  NSString *unitId = config[@"unitId"];
  BOOL isGAM = [config[@"isGAM"] boolValue];
  
  if (!requestId || !unitId) {
    reject(@"invalid_config", @"requestId and unitId are required", nil);
    return;
  }
  
  // Check if ad already exists
  if (self.cachedBannerAds[requestId]) {
    NSDictionary *existingInfo = self.cachedAdInfo[requestId];
    resolve(existingInfo);
    return;
  }
  
  GADBannerView *bannerView;
  
  if (isGAM) {
    bannerView = [[GAMBannerView alloc] init];
  } else {
    bannerView = [[GADBannerView alloc] init];
  }
  
  bannerView.adUnitID = unitId;
  
  // Set ad sizes
  if (isGAM) {
    NSArray *sizes = config[@"sizes"];
    if (sizes && sizes.count > 0) {
      NSMutableArray<NSValue *> *adSizes = [[NSMutableArray alloc] init];
      for (NSString *sizeString in sizes) {
        GADAdSize adSize = [RNGoogleMobileAdsCommon getAdSizeFromString:sizeString];
        [adSizes addObject:NSValueFromGADAdSize(adSize)];
      }
      ((GAMBannerView *)bannerView).validAdSizes = adSizes;
    }
  } else {
    NSString *sizeString = config[@"size"];
    if (sizeString) {
      GADAdSize adSize = [RNGoogleMobileAdsCommon getAdSizeFromString:sizeString];
      bannerView.adSize = adSize;
    }
  }
  
  // Create ad request
  GADRequest *request = [RNGoogleMobileAdsCommon buildAdRequest:config[@"requestOptions"]];
  
  // Set delegate to handle load completion
  __weak typeof(self) weakSelf = self;
  bannerView.delegate = [[RNGoogleMobileAdsCachedBannerDelegate alloc] initWithRequestId:requestId
                                                                                resolver:resolve
                                                                                rejecter:reject
                                                                                  module:weakSelf];
  
  // Store the banner view
  self.cachedBannerAds[requestId] = bannerView;
  
  // Load the ad
  [bannerView loadRequest:request];
#endif
}

RCT_EXPORT_METHOD(getCachedAdInfo:(NSString *)requestId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  NSDictionary *adInfo = self.cachedAdInfo[requestId];
  if (adInfo) {
    resolve(adInfo);
  } else {
    reject(@"not_found", @"Cached ad not found", nil);
  }
}

RCT_EXPORT_METHOD(removeCachedAd:(NSString *)requestId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  GADBannerView *bannerView = self.cachedBannerAds[requestId];
  if (bannerView) {
    bannerView.delegate = nil;
    [self.cachedBannerAds removeObjectForKey:requestId];
    [self.cachedAdInfo removeObjectForKey:requestId];
  }
  resolve(nil);
}

RCT_EXPORT_METHOD(getAllCachedAdIds:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  NSArray *requestIds = [self.cachedBannerAds allKeys];
  resolve(requestIds);
}

RCT_EXPORT_METHOD(clearAllCachedAds:(RCTPromiseResolveBlock)resolve
                  rejecter:(RCTPromiseRejectBlock)reject) {
  for (GADBannerView *bannerView in [self.cachedBannerAds allValues]) {
    bannerView.delegate = nil;
  }
  [self.cachedBannerAds removeAllObjects];
  [self.cachedAdInfo removeAllObjects];
  resolve(nil);
}

- (GADBannerView *)getCachedBannerView:(NSString *)requestId {
  return self.cachedBannerAds[requestId];
}

- (void)storeCachedAdInfo:(NSString *)requestId
                   unitId:(NSString *)unitId
                 isLoaded:(BOOL)isLoaded
                    width:(NSNumber *)width
                   height:(NSNumber *)height {
  NSDictionary *adInfo = @{
    @"requestId": requestId,
    @"unitId": unitId,
    @"isLoaded": @(isLoaded),
    @"width": width ?: @0,
    @"height": height ?: @0
  };
  self.cachedAdInfo[requestId] = adInfo;
}

@end

// Delegate class for handling ad load events
@interface RNGoogleMobileAdsCachedBannerDelegate : NSObject <GADBannerViewDelegate>
@property(nonatomic, strong) NSString *requestId;
@property(nonatomic, copy) RCTPromiseResolveBlock resolver;
@property(nonatomic, copy) RCTPromiseRejectBlock rejecter;
@property(nonatomic, weak) RNGoogleMobileAdsCachedBannerModule *module;
@end

@implementation RNGoogleMobileAdsCachedBannerDelegate

- (instancetype)initWithRequestId:(NSString *)requestId
                         resolver:(RCTPromiseResolveBlock)resolver
                         rejecter:(RCTPromiseRejectBlock)rejecter
                           module:(RNGoogleMobileAdsCachedBannerModule *)module {
  self = [super init];
  if (self) {
    _requestId = requestId;
    _resolver = resolver;
    _rejecter = rejecter;
    _module = module;
  }
  return self;
}

- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView {
  CGSize adSize = bannerView.bounds.size;
  [self.module storeCachedAdInfo:self.requestId
                          unitId:bannerView.adUnitID
                        isLoaded:YES
                           width:@(adSize.width)
                          height:@(adSize.height)];
  
  NSDictionary *adInfo = @{
    @"requestId": self.requestId,
    @"unitId": bannerView.adUnitID,
    @"isLoaded": @YES,
    @"width": @(adSize.width),
    @"height": @(adSize.height)
  };
  
  if (self.resolver) {
    self.resolver(adInfo);
    self.resolver = nil;
    self.rejecter = nil;
  }
}

- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error {
  [self.module storeCachedAdInfo:self.requestId
                          unitId:bannerView.adUnitID
                        isLoaded:NO
                           width:@0
                          height:@0];
  
  if (self.rejecter) {
    self.rejecter(@"ad_load_failed", error.localizedDescription, error);
    self.resolver = nil;
    self.rejecter = nil;
  }
}

@end
