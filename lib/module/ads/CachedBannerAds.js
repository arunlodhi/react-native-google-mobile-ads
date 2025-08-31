/*
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

import { validateAdRequestOptions } from '../validateAdRequestOptions';
import NativeCachedBannerModule from '../specs/modules/NativeCachedBannerModule';
/**
 * Request a cached banner ad that can be reused across re-renders
 * @param options Banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export function requestBannerAd(options) {
  if (!options.unitId) {
    return Promise.reject(new Error('unitId is required'));
  }
  if (!options.size) {
    return Promise.reject(new Error('size is required'));
  }
  let validatedRequestOptions = {};
  if (options.requestOptions) {
    try {
      validatedRequestOptions = validateAdRequestOptions(options.requestOptions);
    } catch (e) {
      return Promise.reject(e);
    }
  }
  const config = {
    unitId: options.unitId,
    size: options.size,
    requestOptions: validatedRequestOptions,
    maxHeight: options.maxHeight || 0,
    width: options.width || 0,
    isGAM: false
  };
  return NativeCachedBannerModule.requestCachedBannerAd(config);
}

/**
 * Request a cached GAM banner ad that can be reused across re-renders
 * @param options GAM banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export function requestGAMBannerAd(options) {
  if (!options.unitId) {
    return Promise.reject(new Error('unitId is required'));
  }
  if (!options.sizes || !Array.isArray(options.sizes) || options.sizes.length === 0) {
    return Promise.reject(new Error('sizes array is required and must not be empty'));
  }
  let validatedRequestOptions = {};
  if (options.requestOptions) {
    try {
      validatedRequestOptions = validateAdRequestOptions(options.requestOptions);
    } catch (e) {
      return Promise.reject(e);
    }
  }
  const config = {
    unitId: options.unitId,
    sizes: options.sizes,
    requestOptions: validatedRequestOptions,
    maxHeight: options.maxHeight || 0,
    width: options.width || 0,
    manualImpressionsEnabled: options.manualImpressionsEnabled || false,
    isGAM: true
  };
  return NativeCachedBannerModule.requestCachedBannerAd(config);
}

/**
 * Get information about a cached ad
 * @param requestId The request ID of the cached ad
 * @returns Promise that resolves with cached ad info or rejects if not found
 */
export function getCachedAdInfo(requestId) {
  if (!requestId || typeof requestId !== 'string') {
    return Promise.reject(new Error('requestId must be a non-empty string'));
  }
  return NativeCachedBannerModule.getCachedAdInfo(requestId);
}

/**
 * Remove a cached ad from memory
 * @param requestId The request ID of the cached ad to remove
 * @returns Promise that resolves when ad is removed
 */
export function removeCachedAd(requestId) {
  if (!requestId || typeof requestId !== 'string') {
    return Promise.reject(new Error('requestId must be a non-empty string'));
  }
  return NativeCachedBannerModule.removeCachedAd(requestId);
}

/**
 * Get all cached ad request IDs
 * @returns Promise that resolves with array of request IDs
 */
export function getAllCachedAdIds() {
  return NativeCachedBannerModule.getAllCachedAdIds();
}

/**
 * Clear all cached ads from memory
 * @returns Promise that resolves when all ads are cleared
 */
export function clearAllCachedAds() {
  return NativeCachedBannerModule.clearAllCachedAds();
}
//# sourceMappingURL=CachedBannerAds.js.map