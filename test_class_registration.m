// Test file to verify class registration works
#import <Foundation/Foundation.h>

int main() {
    @autoreleasepool {
        // Test the class registration that was causing the crash
        Class cachedBannerClass = NSClassFromString(@"RNGoogleMobileAdsCachedBannerView");
        
        if (cachedBannerClass != nil) {
            NSLog(@"✅ SUCCESS: RNGoogleMobileAdsCachedBannerView class found!");
            
            // Test creating the dictionary that was causing the crash
            NSDictionary *testDict = @{
                @"RNGoogleMobileAdsCachedBannerView": cachedBannerClass
            };
            
            NSLog(@"✅ SUCCESS: Dictionary created without crash: %@", testDict);
            return 0;
        } else {
            NSLog(@"❌ ERROR: RNGoogleMobileAdsCachedBannerView class not found!");
            return 1;
        }
    }
}
