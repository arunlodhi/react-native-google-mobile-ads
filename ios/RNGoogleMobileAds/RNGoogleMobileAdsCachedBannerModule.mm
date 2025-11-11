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
#import "RNGoogleMobileAdsCachedBannerComponent.h"
#import "RNGoogleMobileAdsCommon.h"

#if !TARGET_OS_MACCATALYST
#import <GoogleMobileAds/GoogleMobileAds.h>
#import <GoogleMobileAds/GAMBannerView.h>
#endif

#ifdef RCT_NEW_ARCH_ENABLED
#import <RNGoogleMobileAdsSpec/RNGoogleMobileAdsSpec.h>
#endif

// Delegate class interface declaration
@interface RNGoogleMobileAdsCachedBannerDelegate : NSObject <GADBannerViewDelegate>
@property(nonatomic, strong) NSString *requestId;
@property(nonatomic, copy) RCTPromiseResolveBlock resolver;
@property(nonatomic, copy) RCTPromiseRejectBlock rejecter;
@property(nonatomic, weak) RNGoogleMobileAdsCachedBannerModule *module;

- (instancetype)initWithRequestId:(NSString *)requestId
                         resolver:(RCTPromiseResolveBlock)resolver
                         rejecter:(RCTPromiseRejectBlock)rejecter
                           module:(RNGoogleMobileAdsCachedBannerModule *)module;
@end

@interface RNGoogleMobileAdsCachedBannerModule ()
@property(nonatomic, strong) NSMutableDictionary<NSString *, GADBannerView *> *cachedBannerAds;
@property(nonatomic, strong) NSMutableDictionary<NSString *, NSDictionary *> *cachedAdInfo;
@property(nonatomic, strong) NSMutableDictionary<NSString *, RNGoogleMobileAdsCachedBannerDelegate *> *delegates;
@end

@implementation RNGoogleMobileAdsCachedBannerModule

RCT_EXPORT_MODULE(RNGoogleMobileAdsCachedBannerModule);

#ifdef RCT_NEW_ARCH_ENABLED
- (std::shared_ptr<facebook::react::TurboModule>)getTurboModule:
    (const facebook::react::ObjCTurboModule::InitParams &)params
{
    return std::make_shared<facebook::react::NativeCachedBannerModuleSpecJSI>(params);
}
#endif

- (instancetype)init {
  self = [super init];
  if (self) {
    _cachedBannerAds = [[NSMutableDictionary alloc] init];
    _cachedAdInfo = [[NSMutableDictionary alloc] init];
    _delegates = [[NSMutableDictionary alloc] init];
    
    // Set this instance as the shared module instance for components to access
    [RNGoogleMobileAdsCachedBannerComponent setSharedModuleInstance:self];
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
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
#if TARGET_OS_MACCATALYST
  reject(@"unsupported", @"Cached banner ads are not supported on macOS.", nil);
  return;
#else
  NSString *unitId = config[@"unitId"];
  BOOL isGAM = [config[@"isGAM"] boolValue];
  
  if (!unitId) {
    reject(@"invalid_config", @"unitId is required", nil);
    return;
  }
  
  // Always generate a new requestId for each request
  NSString *requestId = [self generateRequestId];
  
  GADBannerView *bannerView;
  
  if (isGAM) {
    // Check if GAMBannerView class is available
    Class gamBannerViewClass = NSClassFromString(@"GAMBannerView");
    if (gamBannerViewClass) {
      bannerView = [[gamBannerViewClass alloc] init];
    } else {
      reject(@"gam_unavailable", @"GAMBannerView class not available. Make sure Google Mobile Ads SDK is properly installed.", nil);
      return;
    }
  } else {
    bannerView = [[GADBannerView alloc] init];
  }
  
  bannerView.adUnitID = unitId;
  
  // Set root view controller
  bannerView.rootViewController = [UIApplication sharedApplication].delegate.window.rootViewController;
  
  // Initialize bannerView with initial dimensions if provided
  CGFloat maxHeight = config[@"maxHeight"] ? [config[@"maxHeight"] floatValue] : 0.0f;
  CGFloat width = config[@"width"] ? [config[@"width"] floatValue] : 0.0f;
  
  NSLog(@"CachedBannerModule: === INITIAL DIMENSIONS DEBUG (iOS) ===");
  NSLog(@"CachedBannerModule: MaxHeight from config: %.2f", maxHeight);
  NSLog(@"CachedBannerModule: Width from config: %.2f", width);
  
  if (maxHeight > 0 || width > 0) {
    CGFloat frameWidth = width > 0 ? width : 320.0f; // Default width if not specified
    CGFloat frameHeight = maxHeight > 0 ? maxHeight : 50.0f; // Default height if not specified
    
    NSLog(@"CachedBannerModule: Setting initial frame:");
    NSLog(@"CachedBannerModule: - frameWidth: %.2f", frameWidth);
    NSLog(@"CachedBannerModule: - frameHeight: %.2f", frameHeight);
    
    bannerView.frame = CGRectMake(0, 0, frameWidth, frameHeight);
  }
  
  // Set ad sizes
  if (isGAM) {
    NSArray *sizes = config[@"sizes"];
    if (sizes && sizes.count > 0) {
      NSMutableArray<NSValue *> *adSizes = [[NSMutableArray alloc] init];
      for (NSString *sizeString in sizes) {
        GADAdSize adSize = [RNGoogleMobileAdsCommon stringToAdSize:sizeString withMaxHeight:-1 andWidth:0];
        [adSizes addObject:NSValueFromGADAdSize(adSize)];
      }
      // Use performSelector to set validAdSizes on GAMBannerView
      if ([bannerView respondsToSelector:@selector(setValidAdSizes:)]) {
        [bannerView performSelector:@selector(setValidAdSizes:) withObject:adSizes];
      }
    }
    
    // Set manual impressions enabled for GAM ads if specified
    BOOL manualImpressionsEnabled = config[@"manualImpressionsEnabled"] ? [config[@"manualImpressionsEnabled"] boolValue] : NO;
    if (manualImpressionsEnabled && [bannerView respondsToSelector:@selector(setEnableManualImpressions:)]) {
      [bannerView performSelector:@selector(setEnableManualImpressions:) withObject:@(YES)];
    }
  } else {
    NSString *sizeString = config[@"size"];
    if (sizeString) {
      GADAdSize adSize = [RNGoogleMobileAdsCommon stringToAdSize:sizeString withMaxHeight:-1 andWidth:0];
      bannerView.adSize = adSize;
    }
  }
  
  // Create ad request - use GAMRequest for GAM ads, GADRequest for regular ads
  id request;
  if (isGAM) {
    // For GAM ads, we need to use GAMRequest
    Class gamRequestClass = NSClassFromString(@"GAMRequest");
    if (gamRequestClass) {
      request = [RNGoogleMobileAdsCommon buildAdRequest:config[@"requestOptions"]];
      NSLog(@"CachedBannerModule: Created GAMRequest for GAM ad");
    } else {
      reject(@"gam_unavailable", @"GAMRequest class not available. Make sure Google Mobile Ads SDK is properly installed.", nil);
      return;
    }
  } else {
    request = [RNGoogleMobileAdsCommon buildAdRequest:config[@"requestOptions"]];
    NSLog(@"CachedBannerModule: Created GADRequest for regular ad");
  }
  
  // Set delegate to handle load completion
  __weak RNGoogleMobileAdsCachedBannerModule *weakSelf = self;
  RNGoogleMobileAdsCachedBannerDelegate *delegate = [[RNGoogleMobileAdsCachedBannerDelegate alloc] initWithRequestId:requestId
                                                                                                              resolver:resolve
                                                                                                              rejecter:reject
                                                                                                                module:weakSelf];
  bannerView.delegate = delegate;
  
  // Store the delegate to keep it alive until the ad loads or fails
  self.delegates[requestId] = delegate;
  
  NSLog(@"CachedBannerModule: Set delegate for requestId: %@", requestId);
  NSLog(@"CachedBannerModule: About to load ad with unitId: %@", unitId);
  
  // Store the banner view
  self.cachedBannerAds[requestId] = bannerView;
  
  // Load the ad
  [bannerView loadRequest:request];
  NSLog(@"CachedBannerModule: Called loadRequest for requestId: %@", requestId);
#endif
}

RCT_EXPORT_METHOD(getCachedAdInfo:(NSString *)requestId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  NSDictionary *adInfo = self.cachedAdInfo[requestId];
  if (adInfo) {
    resolve(adInfo);
  } else {
    reject(@"not_found", @"Cached ad not found", nil);
  }
}

RCT_EXPORT_METHOD(removeCachedAd:(NSString *)requestId
                  resolve:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  GADBannerView *bannerView = self.cachedBannerAds[requestId];
  if (bannerView) {
    bannerView.delegate = nil;
    [self.cachedBannerAds removeObjectForKey:requestId];
    [self.cachedAdInfo removeObjectForKey:requestId];
    [self.delegates removeObjectForKey:requestId];
  }
  resolve(nil);
}

RCT_EXPORT_METHOD(getAllCachedAdIds:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  NSArray *requestIds = [self.cachedBannerAds allKeys];
  resolve(requestIds);
}

RCT_EXPORT_METHOD(clearAllCachedAds:(RCTPromiseResolveBlock)resolve
                  reject:(RCTPromiseRejectBlock)reject) {
  for (GADBannerView *bannerView in [self.cachedBannerAds allValues]) {
    bannerView.delegate = nil;
  }
  [self.cachedBannerAds removeAllObjects];
  [self.cachedAdInfo removeAllObjects];
  [self.delegates removeAllObjects];
  resolve(nil);
}

- (GADBannerView *)getCachedBannerView:(NSString *)requestId {
  return self.cachedBannerAds[requestId];
}

- (GADBannerView *)createNewViewForCachedAd:(NSString *)requestId {
  NSDictionary *adInfoData = self.cachedAdInfo[requestId];
  if (adInfoData == nil || ![adInfoData[@"isLoaded"] boolValue]) {
    NSLog(@"CachedBannerModule: No cached ad info or ad not loaded for requestId: %@", requestId);
    return nil;
  }
  
  // For now, return the original ad view
  // Creating new views with the same ad content is complex and may not be supported by the SDK
  GADBannerView *originalAdView = self.cachedBannerAds[requestId];
  if (originalAdView == nil) {
    NSLog(@"CachedBannerModule: Original ad view not found for requestId: %@", requestId);
    return nil;
  }
  
  NSLog(@"CachedBannerModule: Returning original cached ad view for requestId: %@", requestId);
  return originalAdView;
}

- (void)storeCachedAdInfo:(NSString *)requestId
                   unitId:(NSString *)unitId
                   isGAM:(BOOL)isGAM
              sizesString:(NSString *)sizesString
                 isLoaded:(BOOL)isLoaded
                    width:(NSNumber *)width
                   height:(NSNumber *)height {
  NSDictionary *adInfo = @{
    @"requestId": requestId,
    @"unitId": unitId,
    @"isGAM": @(isGAM),
    @"sizesString": sizesString ?: @"",
    @"isLoaded": @(isLoaded),
    @"width": width ?: @0,
    @"height": height ?: @0
  };
  self.cachedAdInfo[requestId] = adInfo;
}


- (NSString *)generateRequestId {
  return [[NSUUID UUID] UUIDString];
}

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
  NSLog(@"CachedBannerModule: === AD LOADED - SIZE CALCULATION DEBUG (iOS) ===");
  NSLog(@"CachedBannerModule: RequestId: %@", self.requestId);
  NSLog(@"CachedBannerModule: UnitId: %@", bannerView.adUnitID);
  
  // Check if this is a GAM banner view using class name
  Class gamBannerViewClass = NSClassFromString(@"GAMBannerView");
  BOOL isGAM = gamBannerViewClass && [bannerView isKindOfClass:gamBannerViewClass];
  NSLog(@"CachedBannerModule: IsGAM: %@", isGAM ? @"YES" : @"NO");
  
  // Log banner view details
  NSLog(@"CachedBannerModule: BannerView.bounds: %@", NSStringFromCGRect(bannerView.bounds));
  NSLog(@"CachedBannerModule: BannerView.frame: %@", NSStringFromCGRect(bannerView.frame));
  NSLog(@"CachedBannerModule: BannerView.adSize: %@", NSStringFromGADAdSize(bannerView.adSize));
  
  // For cached ads, prioritize actual view dimensions over theoretical AdSize dimensions
  // This ensures we get the actual rendered ad size, not just the AdSize specification
  CGSize boundsSize = bannerView.bounds.size;
  CGSize frameSize = bannerView.frame.size;
  GADAdSize gadAdSize = bannerView.adSize;
  
  NSLog(@"CachedBannerModule: Comparing dimensions:");
  NSLog(@"CachedBannerModule: - boundsSize: %.2fx%.2f", boundsSize.width, boundsSize.height);
  NSLog(@"CachedBannerModule: - frameSize: %.2fx%.2f", frameSize.width, frameSize.height);
  NSLog(@"CachedBannerModule: - GADAdSize: %@", NSStringFromGADAdSize(gadAdSize));
  
  // Use bounds size if it's non-zero, otherwise fallback to frame size
  CGSize adSize;
  if (boundsSize.width > 0 && boundsSize.height > 0) {
    adSize = boundsSize;
    NSLog(@"CachedBannerModule: Using bounds.size for dimensions:");
    NSLog(@"CachedBannerModule: - width (from bounds): %.2f", adSize.width);
    NSLog(@"CachedBannerModule: - height (from bounds): %.2f", adSize.height);
  } else if (frameSize.width > 0 && frameSize.height > 0) {
    adSize = frameSize;
    NSLog(@"CachedBannerModule: Using frame.size for dimensions (bounds was zero):");
    NSLog(@"CachedBannerModule: - width (from frame): %.2f", adSize.width);
    NSLog(@"CachedBannerModule: - height (from frame): %.2f", adSize.height);
  } else {
    // Fallback to GADAdSize if both bounds and frame are zero
    adSize = CGSizeMake(gadAdSize.size.width, gadAdSize.size.height);
    NSLog(@"CachedBannerModule: Using GADAdSize for dimensions (both bounds and frame were zero):");
    NSLog(@"CachedBannerModule: - width (from GADAdSize): %.2f", adSize.width);
    NSLog(@"CachedBannerModule: - height (from GADAdSize): %.2f", adSize.height);
  }
  
  // Resize the bannerView to match the actual ad content dimensions
  // This ensures the cached ad takes only the required space
  CGRect currentFrame = bannerView.frame;
  if (adSize.width > 0 && adSize.height > 0 && 
      (currentFrame.size.width != adSize.width || currentFrame.size.height != adSize.height)) {
    
    NSLog(@"CachedBannerModule: Resizing bannerView to match ad content:");
    NSLog(@"CachedBannerModule: - from: %.2fx%.2f", currentFrame.size.width, currentFrame.size.height);
    NSLog(@"CachedBannerModule: - to: %.2fx%.2f", adSize.width, adSize.height);
    
    bannerView.frame = CGRectMake(currentFrame.origin.x, currentFrame.origin.y, adSize.width, adSize.height);
    
    NSLog(@"CachedBannerModule: BannerView resized - new frame: %@", NSStringFromCGRect(bannerView.frame));
  }
  
  [self.module storeCachedAdInfo:self.requestId
                          unitId:bannerView.adUnitID
                           isGAM:isGAM
                     sizesString:@""
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
  
  NSLog(@"CachedBannerModule: Final adInfo: %@", adInfo);
  
  if (self.resolver) {
    self.resolver(adInfo);
    self.resolver = nil;
    self.rejecter = nil;
  }
  
  // Clean up the delegate from the module's dictionary after resolving
  if (self.module) {
    [self.module.delegates removeObjectForKey:self.requestId];
  }
}

- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error {
  // Determine if this is a GAM banner view using class name
  Class gamBannerViewClass = NSClassFromString(@"GAMBannerView");
  BOOL isGAM = gamBannerViewClass && [bannerView isKindOfClass:gamBannerViewClass];
  
  [self.module storeCachedAdInfo:self.requestId
                          unitId:bannerView.adUnitID
                           isGAM:isGAM
                     sizesString:@""
                        isLoaded:NO
                           width:@0
                          height:@0];
  
  if (self.rejecter) {
    self.rejecter(@"ad_load_failed", error.localizedDescription, error);
    self.resolver = nil;
    self.rejecter = nil;
  }
  
  // Clean up the delegate from the module's dictionary after rejecting
  if (self.module) {
    [self.module.delegates removeObjectForKey:self.requestId];
  }
}

@end
