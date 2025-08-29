import { BannerAdSize } from '../BannerAdSize';
import { RequestOptions } from '../types/RequestOptions';
export interface CachedBannerAdOptions {
    unitId: string;
    size: BannerAdSize | string;
    requestOptions?: RequestOptions;
    maxHeight?: number;
    width?: number;
}
export interface CachedGAMBannerAdOptions {
    unitId: string;
    sizes: (BannerAdSize | string)[];
    requestOptions?: RequestOptions;
    maxHeight?: number;
    width?: number;
    manualImpressionsEnabled?: boolean;
}
export interface CachedAdInfo {
    requestId: string;
    unitId: string;
    isLoaded: boolean;
    width?: number;
    height?: number;
}
/**
 * Request a cached banner ad that can be reused across re-renders
 * @param requestId Unique identifier for this cached ad
 * @param options Banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export declare function requestBannerAd(requestId: string, options: CachedBannerAdOptions): Promise<CachedAdInfo>;
/**
 * Request a cached GAM banner ad that can be reused across re-renders
 * @param requestId Unique identifier for this cached ad
 * @param options GAM banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export declare function requestGAMBannerAd(requestId: string, options: CachedGAMBannerAdOptions): Promise<CachedAdInfo>;
/**
 * Get information about a cached ad
 * @param requestId The request ID of the cached ad
 * @returns Promise that resolves with cached ad info or rejects if not found
 */
export declare function getCachedAdInfo(requestId: string): Promise<CachedAdInfo>;
/**
 * Remove a cached ad from memory
 * @param requestId The request ID of the cached ad to remove
 * @returns Promise that resolves when ad is removed
 */
export declare function removeCachedAd(requestId: string): Promise<void>;
/**
 * Get all cached ad request IDs
 * @returns Promise that resolves with array of request IDs
 */
export declare function getAllCachedAdIds(): Promise<string[]>;
/**
 * Clear all cached ads from memory
 * @returns Promise that resolves when all ads are cleared
 */
export declare function clearAllCachedAds(): Promise<void>;
//# sourceMappingURL=CachedBannerAds.d.ts.map