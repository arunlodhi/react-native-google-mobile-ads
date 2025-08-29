"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.hasAd = hasAd;
exports.releaseAd = releaseAd;
exports.requestBannerAd = requestBannerAd;
exports.requestGAMBannerAd = requestGAMBannerAd;
var _validateAdRequestOptions = require("../validateAdRequestOptions");
var _BannerAdSize = require("../BannerAdSize");
var _NativeGoogleMobileAdsRequestModule = _interopRequireDefault(require("../specs/modules/NativeGoogleMobileAdsRequestModule"));
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

const sizeRegex = /([0-9]+)x([0-9]+)/;

/**
 * Request a Banner Ad and return a promise that resolves with requestId when ad loads
 * or rejects when ad fails to load
 */
async function requestBannerAd(params) {
  const {
    unitId,
    size,
    maxHeight,
    width,
    requestOptions
  } = params;

  // Validate unitId
  if (!unitId || typeof unitId !== 'string') {
    throw new Error("requestBannerAd: 'unitId' expected a valid string unit ID.");
  }

  // Validate size
  if (!(size in _BannerAdSize.BannerAdSize) && !(size in _BannerAdSize.GAMBannerAdSize) && !sizeRegex.test(size)) {
    throw new Error("requestBannerAd: 'size' expected a valid BannerAdSize or custom size string.");
  }

  // Validate request options
  let validatedRequestOptions;
  if (requestOptions) {
    try {
      validatedRequestOptions = (0, _validateAdRequestOptions.validateAdRequestOptions)(requestOptions);
    } catch (e) {
      if (e instanceof Error) {
        throw new Error(`requestBannerAd: ${e.message}`);
      }
    }
  }
  try {
    const result = await _NativeGoogleMobileAdsRequestModule.default.requestBannerAd(unitId, size, maxHeight, width, validatedRequestOptions);
    return {
      requestId: result.requestId,
      dimensions: {
        width: result.width,
        height: result.height
      }
    };
  } catch (error) {
    throw error;
  }
}

/**
 * Request a GAM Banner Ad and return a promise that resolves with requestId when ad loads
 * or rejects when ad fails to load
 */
async function requestGAMBannerAd(params) {
  const {
    unitId,
    sizes,
    maxHeight,
    width,
    requestOptions,
    manualImpressionsEnabled
  } = params;

  // Validate unitId
  if (!unitId || typeof unitId !== 'string') {
    throw new Error("requestGAMBannerAd: 'unitId' expected a valid string unit ID.");
  }

  // Validate sizes
  if (!Array.isArray(sizes) || sizes.length === 0 || !sizes.every(size => size in _BannerAdSize.BannerAdSize || size in _BannerAdSize.GAMBannerAdSize || sizeRegex.test(size))) {
    throw new Error("requestGAMBannerAd: 'sizes' expected a valid array of BannerAdSize or custom size strings.");
  }

  // Validate request options
  let validatedRequestOptions;
  if (requestOptions) {
    try {
      validatedRequestOptions = (0, _validateAdRequestOptions.validateAdRequestOptions)(requestOptions);
    } catch (e) {
      if (e instanceof Error) {
        throw new Error(`requestGAMBannerAd: ${e.message}`);
      }
    }
  }
  try {
    const result = await _NativeGoogleMobileAdsRequestModule.default.requestGAMBannerAd(unitId, sizes, maxHeight, width, validatedRequestOptions, manualImpressionsEnabled);
    return {
      requestId: result.requestId,
      dimensions: {
        width: result.width,
        height: result.height
      }
    };
  } catch (error) {
    throw error;
  }
}

/**
 * Release a requested ad by requestId to free up memory
 */
function releaseAd(requestId) {
  if (!requestId || typeof requestId !== 'string') {
    throw new Error("releaseAd: 'requestId' expected a valid string.");
  }
  _NativeGoogleMobileAdsRequestModule.default.releaseAd(requestId);
}

/**
 * Check if an ad with requestId exists
 */
function hasAd(requestId) {
  if (!requestId || typeof requestId !== 'string') {
    return false;
  }
  return _NativeGoogleMobileAdsRequestModule.default.hasAd(requestId);
}
//# sourceMappingURL=AdRequestManager.js.map