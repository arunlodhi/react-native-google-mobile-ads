#if !TARGET_OS_MACCATALYST

// This guard prevent the code from being compiled in the old architecture
#ifdef RCT_NEW_ARCH_ENABLED
#import "RNGoogleMobileAdsCachedBannerView.h"
#import "RNGoogleMobileAdsCommon.h"
#import "RNGoogleMobileAdsCachedBannerModule.h"

#import <react/renderer/components/RNGoogleMobileAdsSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNGoogleMobileAdsSpec/EventEmitters.h>
#import <react/renderer/components/RNGoogleMobileAdsSpec/Props.h>
#import <react/renderer/components/RNGoogleMobileAdsSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface RNGoogleMobileAdsCachedBannerView () <RCTRNGoogleMobileAdsCachedBannerViewViewProtocol>

@end

@implementation RNGoogleMobileAdsCachedBannerView

+ (ComponentDescriptorProvider)componentDescriptorProvider {
  return concreteComponentDescriptorProvider<RNGoogleMobileAdsCachedBannerViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RNGoogleMobileAdsCachedBannerViewProps>();
    _props = defaultProps;
  }

  return self;
}

- (void)prepareForRecycle {
  [super prepareForRecycle];
  static const auto defaultProps = std::make_shared<const RNGoogleMobileAdsCachedBannerViewProps>();
  _props = defaultProps;

  if (_banner) {
    [_banner removeFromSuperview];
    _banner = nil;
  }
  _requestId = nil;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps {
  const auto &oldViewProps =
      *std::static_pointer_cast<RNGoogleMobileAdsCachedBannerViewProps const>(_props);
  const auto &newViewProps =
      *std::static_pointer_cast<RNGoogleMobileAdsCachedBannerViewProps const>(props);

  if (oldViewProps.requestId != newViewProps.requestId) {
    _requestId = [[NSString alloc] initWithUTF8String:newViewProps.requestId.c_str()];
    [self setupBannerView];
  }

  [super updateProps:props oldProps:oldProps];
}

- (void)dealloc {
  if (_banner) {
    [_banner removeFromSuperview];
    _banner = nil;
  }
}

#pragma mark - Methods

- (void)setupBannerView {
  if (!_requestId) {
    return;
  }
  
  // Remove existing banner view
  if (_banner) {
    [_banner removeFromSuperview];
    _banner = nil;
  }
  
  // Get cached banner view from module
  RNGoogleMobileAdsCachedBannerModule *module = [[RCTBridge currentBridge] moduleForClass:[RNGoogleMobileAdsCachedBannerModule class]];
  if (!module) {
    // Try to get from shared instance
    module = [RNGoogleMobileAdsCachedBannerComponent sharedModuleInstance];
  }
  
  GADBannerView *cachedBannerView = [module getCachedBannerView:_requestId];
  
  if (cachedBannerView) {
    _banner = cachedBannerView;
    _banner.delegate = self;
    _banner.rootViewController = [RNGoogleMobileAdsCommon currentViewController];
    
    // Check if this is a fluid ad and handle layout accordingly
    GADAdSize adSize = _banner.adSize;
    BOOL isFluid = GADAdSizeEqualToSize(adSize, GADAdSizeFluid);
    
    if (isFluid) {
      // For fluid ads, use frame-based layout with autoresizing mask
      _banner.frame = self.bounds;
      _banner.autoresizingMask = UIViewAutoresizingFlexibleWidth;
      _banner.translatesAutoresizingMaskIntoConstraints = YES;
    } else {
      // For fixed-size ads, use constraints
      _banner.translatesAutoresizingMaskIntoConstraints = NO;
      [NSLayoutConstraint activateConstraints:@[
        [_banner.centerXAnchor constraintEqualToAnchor:self.centerXAnchor],
        [_banner.centerYAnchor constraintEqualToAnchor:self.centerYAnchor],
        [_banner.widthAnchor constraintLessThanOrEqualToAnchor:self.widthAnchor],
        [_banner.heightAnchor constraintLessThanOrEqualToAnchor:self.heightAnchor]
      ]];
    }
    
    // Add to view hierarchy
    [self addSubview:_banner];
  }
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
  if ([commandName isEqual:@"recordManualImpression"]) {
    [self recordManualImpression];
  }
}

- (void)recordManualImpression {
  if ([_banner isKindOfClass:[GAMBannerView class]]) {
    [(GAMBannerView *)_banner recordImpression];
  }
}

#pragma mark - Events

- (void)bannerViewDidReceiveAd:(GADBannerView *)bannerView {
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAdLoaded",
            .width = bannerView.bounds.size.width,
            .height = bannerView.bounds.size.height});
  }
}

- (void)bannerView:(GADBannerView *)bannerView didFailToReceiveAdWithError:(NSError *)error {
  NSDictionary *errorAndMessage = [RNGoogleMobileAdsCommon getCodeAndMessageFromAdError:error];
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAdFailedToLoad",
            .code = std::string([[errorAndMessage valueForKey:@"code"] UTF8String]),
            .message = std::string([[errorAndMessage valueForKey:@"message"] UTF8String])});
  }
}

- (void)bannerViewWillPresentScreen:(GADBannerView *)bannerView {
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAdOpened"});
  }
}

- (void)bannerViewDidRecordImpression:(GADBannerView *)bannerView {
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAdImpression"});
  }
}

- (void)bannerViewDidRecordClick:(GADBannerView *)bannerView {
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAdClicked"});
  }
}

- (void)bannerViewWillDismissScreen:(GADBannerView *)bannerView {
  // not in use
}

- (void)bannerViewDidDismissScreen:(GADBannerView *)bannerView {
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAdClosed"});
  }
}

- (void)adView:(nonnull GADBannerView *)banner
    didReceiveAppEvent:(nonnull NSString *)name
              withInfo:(nullable NSString *)info {
  if (_eventEmitter != nullptr) {
    std::dynamic_pointer_cast<const facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter>(
        _eventEmitter)
        ->onNativeEvent(facebook::react::RNGoogleMobileAdsCachedBannerViewEventEmitter::OnNativeEvent{
            .type = "onAppEvent",
            .name = std::string([name UTF8String]),
            .data = std::string([info UTF8String])});
  }
}

@end

#pragma mark - RNGoogleMobileAdsCachedBannerViewCls

Class<RCTComponentViewProtocol> RNGoogleMobileAdsCachedBannerViewCls(void) {
  return RNGoogleMobileAdsCachedBannerView.class;
}

#endif

#endif
