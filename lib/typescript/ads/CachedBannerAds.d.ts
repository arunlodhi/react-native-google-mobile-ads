import { BannerAdSize } from '../BannerAdSize';
import { RequestOptions } from '../types/RequestOptions';
import { AdEventType } from '../AdEventType';
import { GAMAdEventType } from '../GAMAdEventType';
import { AdEventListener } from '../types/AdEventListener';
import { AdEventsListener } from '../types/AdEventsListener';
export interface AdListeners {
    onAdLoaded?: () => void;
    onAdFailedToLoad?: (error: Error) => void;
    onAdOpened?: () => void;
    onAdClosed?: () => void;
    onAdClicked?: () => void;
    onPaidEvent?: (value: any) => void;
    onAppEvent?: (name: string, data: string) => void;
    onSizeChange?: (dimensions: {
        width: number;
        height: number;
    }) => void;
}
export interface CachedBannerAdOptions {
    unitId: string;
    size: BannerAdSize | string;
    requestOptions?: RequestOptions;
    maxHeight?: number;
    width?: number;
    adListeners?: AdListeners;
}
export interface CachedGAMBannerAdOptions {
    unitId: string;
    sizes: (BannerAdSize | string)[];
    requestOptions?: RequestOptions;
    maxHeight?: number;
    width?: number;
    manualImpressionsEnabled?: boolean;
    adListeners?: AdListeners;
}
export interface CachedAdInfo {
    requestId: string;
    unitId: string;
    isLoaded: boolean;
    width?: number;
    height?: number;
}
type EventType = AdEventType | GAMAdEventType;
export interface CachedAdInfoWithListeners extends CachedAdInfo {
    addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) => () => void;
    addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) => () => void;
    removeAllListeners: () => void;
}
/**
 * Create a listener manager for a cached ad before requesting it
 * @param unitId The ad unit ID
 * @returns Object with listener management methods and request function
 */
export declare function createCachedAdListeners(unitId: string): {
    addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) => () => void;
    addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) => () => void;
    removeAllListeners: () => void;
    requestBannerAd: (options: Omit<CachedBannerAdOptions, 'unitId'>) => Promise<CachedAdInfo>;
    requestGAMBannerAd: (options: Omit<CachedGAMBannerAdOptions, 'unitId'>) => Promise<CachedAdInfo>;
};
/**
 * Add event listeners to an existing cached ad
 * @param requestId The request ID of the cached ad
 * @returns Promise that resolves with listener management methods
 */
export declare function addCachedAdListeners(requestId: string): Promise<{
    addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) => () => void;
    addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) => () => void;
    removeAllListeners: () => void;
}>;
/**
 * Remove all listeners for a cached ad
 * @param requestId The request ID of the cached ad
 */
export declare function removeCachedAdListeners(requestId: string): void;
/**
 * Request a cached banner ad that can be reused across re-renders
 * @param options Banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export declare function requestBannerAd(options: CachedBannerAdOptions): Promise<CachedAdInfo>;
/**
 * Request a cached GAM banner ad that can be reused across re-renders
 * @param options GAM banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export declare function requestGAMBannerAd(options: CachedGAMBannerAdOptions): Promise<CachedAdInfo>;
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
export {};
//# sourceMappingURL=CachedBannerAds.d.ts.map