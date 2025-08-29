import type { TurboModule } from 'react-native';
export interface Spec extends TurboModule {
    /**
     * Request a banner ad and return a promise with the requestId
     */
    requestBannerAd(unitId: string, size: string, maxHeight?: number, width?: number, requestOptions?: string): Promise<{
        requestId: string;
        width: number;
        height: number;
    }>;
    /**
     * Request a GAM banner ad and return a promise with the requestId
     */
    requestGAMBannerAd(unitId: string, sizes: string[], maxHeight?: number, width?: number, requestOptions?: string, manualImpressionsEnabled?: boolean): Promise<{
        requestId: string;
        width: number;
        height: number;
    }>;
    /**
     * Release a requested ad by requestId
     */
    releaseAd(requestId: string): void;
    /**
     * Check if an ad with requestId exists
     */
    hasAd(requestId: string): boolean;
}
declare const _default: Spec;
export default _default;
//# sourceMappingURL=NativeGoogleMobileAdsRequestModule.d.ts.map