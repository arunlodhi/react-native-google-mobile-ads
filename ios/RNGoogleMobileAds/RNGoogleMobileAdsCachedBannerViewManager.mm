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

#import <React/RCTUIManager.h>
#import <React/RCTViewManager.h>

#import "RNGoogleMobileAdsCachedBannerComponent.h"

#ifdef RCT_NEW_ARCH_ENABLED
#import <react/renderer/components/RNGoogleMobileAdsSpec/ComponentDescriptors.h>
#import <react/renderer/components/RNGoogleMobileAdsSpec/EventEmitters.h>
#import <react/renderer/components/RNGoogleMobileAdsSpec/Props.h>
#import <react/renderer/components/RNGoogleMobileAdsSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@interface RNGoogleMobileAdsCachedBannerView : RCTViewComponentView <RCTRNGoogleMobileAdsCachedBannerViewViewProtocol>
@property(nonatomic, strong) RNGoogleMobileAdsCachedBannerComponent *cachedBannerComponent;
@end

@implementation RNGoogleMobileAdsCachedBannerView

- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const RNGoogleMobileAdsCachedBannerViewProps>();
    _props = defaultProps;
    
    _cachedBannerComponent = [RNGoogleMobileAdsCachedBannerComponent new];
    [self addSubview:_cachedBannerComponent];
  }
  return self;
}

- (void)layoutSubviews {
  [super layoutSubviews];
  _cachedBannerComponent.frame = self.bounds;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
  return concreteComponentDescriptorProvider<RNGoogleMobileAdsCachedBannerViewComponentDescriptor>();
}

+ (BOOL)shouldBeRecycled {
  return NO;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps {
  const auto &oldViewProps = *std::static_pointer_cast<RNGoogleMobileAdsCachedBannerViewProps const>(_props);
  const auto &newViewProps = *std::static_pointer_cast<RNGoogleMobileAdsCachedBannerViewProps const>(props);

  if (oldViewProps.requestId != newViewProps.requestId) {
    NSString *requestId = [[NSString alloc] initWithUTF8String:newViewProps.requestId.c_str()];
    _cachedBannerComponent.requestId = requestId;
  }

  [super updateProps:props oldProps:oldProps];
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
  if ([commandName isEqualToString:@"recordManualImpression"]) {
    [_cachedBannerComponent recordManualImpression];
  }
}

@end

Class<RCTComponentViewProtocol> RNGoogleMobileAdsCachedBannerViewCls(void) {
  return RNGoogleMobileAdsCachedBannerView.class;
}

#endif

#ifndef RCT_NEW_ARCH_ENABLED

@interface RNGoogleMobileAdsCachedBannerViewManager : RCTViewManager
@end

@implementation RNGoogleMobileAdsCachedBannerViewManager

RCT_EXPORT_MODULE(RNGoogleMobileAdsCachedBannerView);

RCT_EXPORT_VIEW_PROPERTY(requestId, NSString);

RCT_EXPORT_VIEW_PROPERTY(onNativeEvent, RCTBubblingEventBlock);

RCT_EXPORT_METHOD(recordManualImpression : (nonnull NSNumber *)reactTag) {
#if !TARGET_OS_MACCATALYST
  [self.bridge.uiManager
      addUIBlock:^(RCTUIManager *uiManager, NSDictionary<NSNumber *, UIView *> *viewRegistry) {
        RNGoogleMobileAdsCachedBannerComponent *banner = viewRegistry[reactTag];
        if (!banner || ![banner isKindOfClass:[RNGoogleMobileAdsCachedBannerComponent class]]) {
          RCTLogError(@"Cannot find NativeView with tag #%@", reactTag);
          return;
        }
        [banner recordManualImpression];
      }];
#endif
}

@synthesize bridge = _bridge;

- (UIView *)view {
#if TARGET_OS_MACCATALYST
  return nil;
#else
  RNGoogleMobileAdsCachedBannerComponent *banner = [RNGoogleMobileAdsCachedBannerComponent new];
  return banner;
#endif
}

- (dispatch_queue_t)methodQueue {
  return dispatch_get_main_queue();
}

@end

#endif
