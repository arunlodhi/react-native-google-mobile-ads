"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.addCachedAdListeners = addCachedAdListeners;
exports.clearAllCachedAds = clearAllCachedAds;
exports.createCachedAdListeners = createCachedAdListeners;
exports.getAllCachedAdIds = getAllCachedAdIds;
exports.getCachedAdInfo = getCachedAdInfo;
exports.removeCachedAd = removeCachedAd;
exports.removeCachedAdListeners = removeCachedAdListeners;
exports.requestBannerAd = requestBannerAd;
exports.requestGAMBannerAd = requestGAMBannerAd;
var _validateAdRequestOptions = require("../validateAdRequestOptions");
var _AdEventType = require("../AdEventType");
var _GAMAdEventType = require("../GAMAdEventType");
var _SharedEventEmitter = require("../internal/SharedEventEmitter");
var _NativeError = require("../internal/NativeError");
var _common = require("../common");
var _NativeCachedBannerModule = _interopRequireDefault(require("../specs/modules/NativeCachedBannerModule"));
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
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

/**
 * Listener management class for cached banner ads
 */
class CachedBannerAdListenerManager {
  constructor(requestId, unitId) {
    this._requestId = requestId;
    this._unitId = unitId;
    this._adEventsListeners = new Map();
    this._adEventListenersMap = new Map();
    this._adEventsListenerId = 0;
    this._adEventListenerId = 0;
    this._nativeListener = null;

    // Initialize event listener maps
    Object.values({
      ..._AdEventType.AdEventType,
      ..._GAMAdEventType.GAMAdEventType
    }).forEach(type => {
      this._adEventListenersMap.set(type, new Map());
    });
    this._setupNativeListener();
  }
  _setupNativeListener() {
    // Remove existing listener if any
    if (this._nativeListener) {
      this._nativeListener.remove();
    }
    this._nativeListener = _SharedEventEmitter.SharedEventEmitter.addListener(`google_mobile_ads_cached_banner_event:${this._unitId}:${this._requestId}`, this._handleAdEvent.bind(this));
  }
  _handleAdEvent(event) {
    const {
      type,
      error,
      data
    } = event.body;
    let payload = data;
    if (error) {
      payload = _NativeError.NativeError.fromEvent(error, 'googleMobileAds');
    }
    this._adEventsListeners.forEach(listener => {
      listener({
        type,
        payload
      });
    });
    this._getAdEventListeners(type).forEach(listener => {
      listener(payload);
    });
  }
  _getAdEventListeners(type) {
    return this._adEventListenersMap.get(type);
  }
  addAdEventsListener(listener) {
    if (!(0, _common.isFunction)(listener)) {
      throw new Error("CachedBannerAd.addAdEventsListener(*) 'listener' expected a function.");
    }
    const id = this._adEventsListenerId++;
    this._adEventsListeners.set(id, listener);
    return () => {
      this._adEventsListeners.delete(id);
    };
  }
  addAdEventListener(type, listener) {
    if (!((0, _common.isOneOf)(type, Object.values(_AdEventType.AdEventType)) || (0, _common.isOneOf)(type, Object.values(_GAMAdEventType.GAMAdEventType)))) {
      throw new Error("CachedBannerAd.addAdEventListener(*) 'type' expected a valid event type value.");
    }
    if (!(0, _common.isFunction)(listener)) {
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
const listenerRegistry = new Map();
/**
 * Create a listener manager for a cached ad before requesting it
 * @param unitId The ad unit ID
 * @returns Object with listener management methods and request function
 */
function createCachedAdListeners(unitId) {
  if (!unitId || typeof unitId !== 'string') {
    throw new Error('unitId must be a non-empty string');
  }

  // Generate a temporary request ID for the listener setup
  const tempRequestId = `temp_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  const manager = new CachedBannerAdListenerManager(tempRequestId, unitId);
  listenerRegistry.set(tempRequestId, manager);
  return {
    addAdEventsListener: listener => manager.addAdEventsListener(listener),
    addAdEventListener: (type, listener) => manager.addAdEventListener(type, listener),
    removeAllListeners: () => manager.removeAllListeners(),
    requestBannerAd: async options => {
      const fullOptions = {
        ...options,
        unitId
      };
      const adInfo = await requestBannerAd(fullOptions);

      // Update the manager with the real request ID
      listenerRegistry.delete(tempRequestId);
      manager._requestId = adInfo.requestId;
      listenerRegistry.set(adInfo.requestId, manager);

      // Update the native listener with the real request ID
      manager._setupNativeListener();
      return adInfo;
    },
    requestGAMBannerAd: async options => {
      const fullOptions = {
        ...options,
        unitId
      };
      const adInfo = await requestGAMBannerAd(fullOptions);

      // Update the manager with the real request ID
      listenerRegistry.delete(tempRequestId);
      manager._requestId = adInfo.requestId;
      listenerRegistry.set(adInfo.requestId, manager);

      // Update the native listener with the real request ID
      manager._setupNativeListener();
      return adInfo;
    }
  };
}

/**
 * Add event listeners to an existing cached ad
 * @param requestId The request ID of the cached ad
 * @returns Promise that resolves with listener management methods
 */
async function addCachedAdListeners(requestId) {
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
    addAdEventsListener: listener => manager.addAdEventsListener(listener),
    addAdEventListener: (type, listener) => manager.addAdEventListener(type, listener),
    removeAllListeners: () => manager.removeAllListeners()
  };
}

/**
 * Remove all listeners for a cached ad
 * @param requestId The request ID of the cached ad
 */
function removeCachedAdListeners(requestId) {
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
function setupAdListeners(manager, adListeners) {
  if (adListeners.onAdLoaded) {
    manager.addAdEventListener(_AdEventType.AdEventType.LOADED, adListeners.onAdLoaded);
  }
  if (adListeners.onAdFailedToLoad) {
    manager.addAdEventListener(_AdEventType.AdEventType.ERROR, adListeners.onAdFailedToLoad);
  }
  if (adListeners.onAdOpened) {
    manager.addAdEventListener(_AdEventType.AdEventType.OPENED, adListeners.onAdOpened);
  }
  if (adListeners.onAdClosed) {
    manager.addAdEventListener(_AdEventType.AdEventType.CLOSED, adListeners.onAdClosed);
  }
  if (adListeners.onAdClicked) {
    manager.addAdEventListener(_AdEventType.AdEventType.CLICKED, adListeners.onAdClicked);
  }
  if (adListeners.onPaidEvent) {
    manager.addAdEventListener(_AdEventType.AdEventType.PAID, adListeners.onPaidEvent);
  }
  if (adListeners.onAppEvent) {
    manager.addAdEventListener(_GAMAdEventType.GAMAdEventType.APP_EVENT, appEvent => {
      adListeners.onAppEvent(appEvent.name, appEvent.data);
    });
  }
  if (adListeners.onSizeChange) {
    manager.addAdEventListener(_AdEventType.AdEventType.SIZE_CHANGE, sizeData => {
      adListeners.onSizeChange({
        width: sizeData.width,
        height: sizeData.height
      });
    });
  }
}

/**
 * Request a cached banner ad that can be reused across re-renders
 * @param options Banner ad configuration options
 * @returns Promise that resolves when ad is loaded or rejects on failure
 */
function requestBannerAd(options) {
  if (!options.unitId) {
    return Promise.reject(new Error('unitId is required'));
  }
  if (!options.size) {
    return Promise.reject(new Error('size is required'));
  }
  let validatedRequestOptions = {};
  if (options.requestOptions) {
    try {
      validatedRequestOptions = (0, _validateAdRequestOptions.validateAdRequestOptions)(options.requestOptions);
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
  return _NativeCachedBannerModule.default.requestCachedBannerAd(config).then(adInfo => {
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
function requestGAMBannerAd(options) {
  if (!options.unitId) {
    return Promise.reject(new Error('unitId is required'));
  }
  if (!options.sizes || !Array.isArray(options.sizes) || options.sizes.length === 0) {
    return Promise.reject(new Error('sizes array is required and must not be empty'));
  }
  let validatedRequestOptions = {};
  if (options.requestOptions) {
    try {
      validatedRequestOptions = (0, _validateAdRequestOptions.validateAdRequestOptions)(options.requestOptions);
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
  return _NativeCachedBannerModule.default.requestCachedBannerAd(config).then(adInfo => {
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
function getCachedAdInfo(requestId) {
  if (!requestId || typeof requestId !== 'string') {
    return Promise.reject(new Error('requestId must be a non-empty string'));
  }
  return _NativeCachedBannerModule.default.getCachedAdInfo(requestId);
}

/**
 * Remove a cached ad from memory
 * @param requestId The request ID of the cached ad to remove
 * @returns Promise that resolves when ad is removed
 */
function removeCachedAd(requestId) {
  if (!requestId || typeof requestId !== 'string') {
    return Promise.reject(new Error('requestId must be a non-empty string'));
  }
  return _NativeCachedBannerModule.default.removeCachedAd(requestId);
}

/**
 * Get all cached ad request IDs
 * @returns Promise that resolves with array of request IDs
 */
function getAllCachedAdIds() {
  return _NativeCachedBannerModule.default.getAllCachedAdIds();
}

/**
 * Clear all cached ads from memory
 * @returns Promise that resolves when all ads are cleared
 */
function clearAllCachedAds() {
  return _NativeCachedBannerModule.default.clearAllCachedAds();
}
//# sourceMappingURL=CachedBannerAds.js.map