#if !TARGET_OS_MACCATALYST

// This guard prevent this file to be compiled in the old architecture.
#ifdef RCT_NEW_ARCH_ENABLED
#import <GoogleMobileAds/GADAppEventDelegate.h>
#import <GoogleMobileAds/GADBannerView.h>
#import <GoogleMobileAds/GADBannerViewDelegate.h>
#import <React/RCTViewComponentView.h>
#import <UIKit/UIKit.h>

#ifndef RNGoogleMobileAdsCachedBannerView_h
#define RNGoogleMobileAdsCachedBannerView_h

NS_ASSUME_NONNULL_BEGIN

@interface RNGoogleMobileAdsCachedBannerView
    : RCTViewComponentView <GADBannerViewDelegate, GADAppEventDelegate>

@property GADBannerView *banner;
@property(nonatomic, copy) NSString *requestId;

@end

NS_ASSUME_NONNULL_END

#endif /* RNGoogleMobileAdsCachedBannerView_h */
#endif /* RCT_NEW_ARCH_ENABLED */

#endif
