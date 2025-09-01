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

import { EmitterSubscription } from 'react-native';
import { BannerAdSize } from '../BannerAdSize';
import { RequestOptions } from '../types/RequestOptions';
import { validateAdRequestOptions } from '../validateAdRequestOptions';
import { AdEventType } from '../AdEventType';
import { GAMAdEventType } from '../GAMAdEventType';
import { AdEventListener, AdEventPayload } from '../types/AdEventListener';
import { AdEventsListener } from '../types/AdEventsListener';
import { SharedEventEmitter } from '../internal/SharedEventEmitter';
import { NativeError } from '../internal/NativeError';
import { isFunction, isOneOf } from '../common';
import NativeCachedBannerModule from '../specs/modules/NativeCachedBannerModule';

export interface AdListeners {
  onAdLoaded?: () => void;
  onAdFailedToLoad?: (error: Error) => void;
  onAdOpened?: () => void;
  onAdClosed?: () => void;
  onAdClicked?: () => void;
  onPaidEvent?: (value: any) => void;
  onAppEvent?: (name: string, data: string) => void;
  onSizeChange?: (dimensions: { width: number; height: number }) => void;
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

/**
 * Listener management class for cached banner ads
 */
class CachedBannerAdListenerManager {
  private _adEventsListeners: Map<number, AdEventsListener<EventType>>;
  private _adEventListenersMap: Map<EventType, Map<number, AdEventListener<EventType>>>;
  private _adEventsListenerId: number;
  private _adEventListenerId: number;
  private _nativeListener: EmitterSubscription | null;
  public _requestId: string;
  private _unitId: string;

  constructor(requestId: string, unitId: string) {
    this._requestId = requestId;
    this._unitId = unitId;
    this._adEventsListeners = new Map();
    this._adEventListenersMap = new Map();
    this._adEventsListenerId = 0;
    this._adEventListenerId = 0;
    this._nativeListener = null;

    // Initialize event listener maps
    Object.values({
      ...AdEventType,
      ...GAMAdEventType,
    }).forEach(type => {
      this._adEventListenersMap.set(type as EventType, new Map());
    });

    this._setupNativeListener();
  }

  public _setupNativeListener() {
    // Remove existing listener if any
    if (this._nativeListener) {
      this._nativeListener.remove();
    }

    this._nativeListener = SharedEventEmitter.addListener(
      `google_mobile_ads_cached_banner_event:${this._unitId}:${this._requestId}`,
      this._handleAdEvent.bind(this),
    );
  }

  private _handleAdEvent(event: {
    body: {
      type: EventType;
      error?: { code: string; message: string };
      data?: any;
    };
  }) {
    const { type, error, data } = event.body;

    let payload: AdEventPayload<EventType> = data;
    if (error) {
      payload = NativeError.fromEvent(error, 'googleMobileAds');
    }

    this._adEventsListeners.forEach(listener => {
      listener({
        type,
        payload,
      });
    });

    this._getAdEventListeners(type).forEach(listener => {
      listener(payload);
    });
  }

  private _getAdEventListeners<T extends EventType>(type: T) {
    return this._adEventListenersMap.get(type) as Map<number, AdEventListener<T>>;
  }

  addAdEventsListener<T extends EventType>(listener: AdEventsListener<T>) {
    if (!isFunction(listener)) {
      throw new Error("CachedBannerAd.addAdEventsListener(*) 'listener' expected a function.");
    }

    const id = this._adEventsListenerId++;
    this._adEventsListeners.set(id, listener as AdEventsListener<EventType>);
    return () => {
      this._adEventsListeners.delete(id);
    };
  }

  addAdEventListener<T extends EventType>(type: T, listener: AdEventListener<T>) {
    if (
      !(isOneOf(type, Object.values(AdEventType)) || isOneOf(type, Object.values(GAMAdEventType)))
    ) {
      throw new Error(
        "CachedBannerAd.addAdEventListener(*) 'type' expected a valid event type value.",
      );
    }
    if (!isFunction(listener)) {
      throw new Error("CachedBannerAd.addAdEventListener(_, *) 'listener' expected a function.");
    }

    const id = this._adEventListenerId++;
    this._getAdEventListeners(type).set(id, listener);
    return () => {
      this._getAdEventListeners(type).delete(id);
    };
  }

  removeAllListeners() {
    this._adEventsListeners.clear();
    this._adEventListenersMap.forEach((_, type, map) => {
      map.set(type, new Map());
    });
  }

  destroy() {
    this.removeAllListeners();
    if (this._nativeListener) {
      this._nativeListener.remove();
      this._nativeListener = null;
    }
  }
}

// Global registry to manage listener instances
const listenerRegistry = new Map<string, CachedBannerAdListenerManager>();

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
export function createCachedAdListeners(unitId: string): {
  addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) => () => void;
  addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) => () => void;
  removeAllListeners: () => void;
  requestBannerAd: (options: Omit<CachedBannerAdOptions, 'unitId'>) => Promise<CachedAdInfo>;
  requestGAMBannerAd: (options: Omit<CachedGAMBannerAdOptions, 'unitId'>) => Promise<CachedAdInfo>;
} {
  if (!unitId || typeof unitId !== 'string') {
    throw new Error('unitId must be a non-empty string');
  }

  // Generate a temporary request ID for the listener setup
  const tempRequestId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

  const manager = new CachedBannerAdListenerManager(tempRequestId, unitId);
  listenerRegistry.set(tempRequestId, manager);

  return {
    addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) =>
      manager.addAdEventsListener(listener),
    addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) =>
      manager.addAdEventListener(type, listener),
    removeAllListeners: () => manager.removeAllListeners(),
    requestBannerAd: async (options: Omit<CachedBannerAdOptions, 'unitId'>) => {
      const fullOptions = { ...options, unitId };
      const adInfo = await requestBannerAd(fullOptions);

      // Update the manager with the real request ID
      listenerRegistry.delete(tempRequestId);
      manager._requestId = adInfo.requestId;
      listenerRegistry.set(adInfo.requestId, manager);

      // Update the native listener with the real request ID
      manager._setupNativeListener();

      return adInfo;
    },
    requestGAMBannerAd: async (options: Omit<CachedGAMBannerAdOptions, 'unitId'>) => {
      const fullOptions = { ...options, unitId };
      const adInfo = await requestGAMBannerAd(fullOptions);

      // Update the manager with the real request ID
      listenerRegistry.delete(tempRequestId);
      manager._requestId = adInfo.requestId;
      listenerRegistry.set(adInfo.requestId, manager);

      // Update the native listener with the real request ID
      manager._setupNativeListener();

      return adInfo;
    },
  };
}

/**
 * Add event listeners to an existing cached ad
 * @param requestId The request ID of the cached ad
 * @returns Promise that resolves with listener management methods
 */
export async function addCachedAdListeners(requestId: string): Promise<{
  addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) => () => void;
  addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) => () => void;
  removeAllListeners: () => void;
}> {
  if (!requestId || typeof requestId !== 'string') {
    throw new Error('requestId must be a non-empty string');
  }

  // Get the cached ad info to retrieve unitId
  const adInfo = await getCachedAdInfo(requestId);

  let manager = listenerRegistry.get(requestId);
  if (!manager) {
    manager = new CachedBannerAdListenerManager(requestId, adInfo.unitId);
    listenerRegistry.set(requestId, manager);
  }

  return {
    addAdEventsListener: <T extends EventType>(listener: AdEventsListener<T>) =>
      manager!.addAdEventsListener(listener),
    addAdEventListener: <T extends EventType>(type: T, listener: AdEventListener<T>) =>
      manager!.addAdEventListener(type, listener),
    removeAllListeners: () => manager!.removeAllListeners(),
  };
}

/**
 * Remove all listeners for a cached ad
 * @param requestId The request ID of the cached ad
 */
export function removeCachedAdListeners(requestId: string): void {
  if (!requestId || typeof requestId !== 'string') {
    throw new Error('requestId must be a non-empty string');
  }

  const manager = listenerRegistry.get(requestId);
  if (manager) {
    manager.destroy();
    listenerRegistry.delete(requestId);
  }
}

/**
 * Setup listeners for a cached ad
 * @param manager The listener manager instance
 * @param adListeners The listeners configuration
 */
function setupAdListeners(manager: CachedBannerAdListenerManager, adListeners: AdListeners) {
  if (adListeners.onAdLoaded) {
    manager.addAdEventListener(AdEventType.LOADED, adListeners.onAdLoaded);
  }
  if (adListeners.onAdFailedToLoad) {
    manager.addAdEventListener(AdEventType.ERROR, adListeners.onAdFailedToLoad);
  }
  if (adListeners.onAdOpened) {
    manager.addAdEventListener(AdEventType.OPENED, adListeners.onAdOpened);
  }
  if (adListeners.onAdClosed) {
    manager.addAdEventListener(AdEventType.CLOSED, adListeners.onAdClosed);
  }
  if (adListeners.onAdClicked) {
    manager.addAdEventListener(AdEventType.CLICKED, adListeners.onAdClicked);
  }
  if (adListeners.onPaidEvent) {
    manager.addAdEventListener(AdEventType.PAID, adListeners.onPaidEvent);
  }
  if (adListeners.onAppEvent) {
    manager.addAdEventListener(GAMAdEventType.APP_EVENT, (appEvent: any) => {
      adListeners.onAppEvent!(appEvent.name, appEvent.data);
    });
  }
  if (adListeners.onSizeChange) {
    manager.addAdEventListener(AdEventType.SIZE_CHANGE, (sizeData: any) => {
      adListeners.onSizeChange!({ width: sizeData.width, height: sizeData.height });
    });
  }
}

/**
 * Request a cached banner ad that can be reused across re-renders
 * @param options Banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export function requestBannerAd(options: CachedBannerAdOptions): Promise<CachedAdInfo> {
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
    isGAM: false,
  };

  return NativeCachedBannerModule.requestCachedBannerAd(config).then(adInfo => {
    // Setup listeners if provided
    if (options.adListeners) {
      const manager = new CachedBannerAdListenerManager(adInfo.requestId, adInfo.unitId);
      setupAdListeners(manager, options.adListeners);
      listenerRegistry.set(adInfo.requestId, manager);
    }

    return adInfo;
  });
}

/**
 * Request a cached GAM banner ad that can be reused across re-renders
 * @param options GAM banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
export function requestGAMBannerAd(options: CachedGAMBannerAdOptions): Promise<CachedAdInfo> {
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
    isGAM: true,
  };

  return NativeCachedBannerModule.requestCachedBannerAd(config).then(adInfo => {
    // Setup listeners if provided
    if (options.adListeners) {
      const manager = new CachedBannerAdListenerManager(adInfo.requestId, adInfo.unitId);
      setupAdListeners(manager, options.adListeners);
      listenerRegistry.set(adInfo.requestId, manager);
    }

    return adInfo;
  });
}

/**
 * Get information about a cached ad
 * @param requestId The request ID of the cached ad
 * @returns Promise that resolves with cached ad info or rejects if not found
 */
export function getCachedAdInfo(requestId: string): Promise<CachedAdInfo> {
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
export function removeCachedAd(requestId: string): Promise<void> {
  if (!requestId || typeof requestId !== 'string') {
    return Promise.reject(new Error('requestId must be a non-empty string'));
  }

  return NativeCachedBannerModule.removeCachedAd(requestId);
}

/**
 * Get all cached ad request IDs
 * @returns Promise that resolves with array of request IDs
 */
export function getAllCachedAdIds(): Promise<string[]> {
  return NativeCachedBannerModule.getAllCachedAdIds();
}

/**
 * Clear all cached ads from memory
 * @returns Promise that resolves when all ads are cleared
 */
export function clearAllCachedAds(): Promise<void> {
  return NativeCachedBannerModule.clearAllCachedAds();
}
