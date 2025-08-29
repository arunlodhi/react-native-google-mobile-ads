import type { RequestBannerAdParams, RequestGAMBannerAdParams, AdRequestResponse } from '../types/CachedAdProps';
/**
 * Request a Banner Ad and return a promise that resolves with requestId when ad loads
 * or rejects when ad fails to load
 */
export declare function requestBannerAd(params: RequestBannerAdParams): Promise<AdRequestResponse>;
/**
 * Request a GAM Banner Ad and return a promise that resolves with requestId when ad loads
 * or rejects when ad fails to load
 */
export declare function requestGAMBannerAd(params: RequestGAMBannerAdParams): Promise<AdRequestResponse>;
/**
 * Release a requested ad by requestId to free up memory
 */
export declare function releaseAd(requestId: string): void;
/**
 * Check if an ad with requestId exists
 */
export declare function hasAd(requestId: string): boolean;
//# sourceMappingURL=AdRequestManager.d.ts.map