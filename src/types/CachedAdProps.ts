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

import { BannerAdSize, GAMBannerAdSize } from '../BannerAdSize';
import { AppEvent } from './AppEvent';
import type { PaidEventListener } from './PaidEventListener';
import { RequestOptions } from './RequestOptions';

/**
 * Props for rendering a cached Banner Ad using requestId
 */
export interface CachedBannerAdProps {
  /**
   * The unique request ID for the cached banner ad
   */
  requestId: string;

  /**
   * When an ad has finished loading.
   */
  onAdLoaded?: (dimensions: { width: number; height: number }) => void;

  /**
   * When an ad has failed to load. Callback contains an Error.
   */
  onAdFailedToLoad?: (error: Error) => void;

  /**
   * The ad is now visible to the user.
   */
  onAdOpened?: () => void;

  /**
   * Called when an impression is recorded for an ad.
   */
  onAdImpression?: () => void;

  /**
   * Called when a click is recorded for an ad
   */
  onAdClicked?: () => void;

  /**
   * Called when the user is about to return to the app after tapping on an ad.
   */
  onAdClosed?: () => void;

  /**
   * Called when ad generates revenue.
   * See: https://developers.google.com/admob/android/impression-level-ad-revenue
   */
  onPaid?: PaidEventListener;

  /**
   * Called when ad size dimensions changed
   */
  onSizeChange?: (dimensions: { width: number; height: number }) => void;
}

/**
 * Props for rendering a cached GAM Banner Ad using requestId
 */
export interface CachedGAMBannerAdProps extends CachedBannerAdProps {
  /**
   * When an ad received Ad Manager specific app events.
   */
  onAppEvent?: (appEvent: AppEvent) => void;
}

/**
 * Parameters for requesting a Banner Ad
 */
export interface RequestBannerAdParams {
  /**
   * The Google Mobile Ads unit ID for the banner.
   */
  unitId: string;

  /**
   * The size of the banner. Can be a predefined size via `BannerAdSize` or custom dimensions, e.g. `300x200`.
   */
  size: BannerAdSize | string;

  /**
   * Limit inline adaptive banner height.
   */
  maxHeight?: number;

  /**
   * Sets the width for adaptive banners (inline and anchored).
   */
  width?: number;

  /**
   * The request options for this banner.
   */
  requestOptions?: RequestOptions;
}

/**
 * Parameters for requesting a GAM Banner Ad
 */
export interface RequestGAMBannerAdParams {
  /**
   * The Google Mobile Ads unit ID for the banner.
   */
  unitId: string;

  /**
   * The available sizes of the banner. Can be a array of predefined sizes via `BannerAdSize` or custom dimensions, e.g. `300x200`.
   */
  sizes: (typeof GAMBannerAdSize)[keyof typeof GAMBannerAdSize][] | string[];

  /**
   * Limit inline adaptive banner height.
   */
  maxHeight?: number;

  /**
   * Sets the width for adaptive banners (inline and anchored).
   */
  width?: number;

  /**
   * The request options for this banner.
   */
  requestOptions?: RequestOptions;

  /**
   * Whether to enable the manual impression counting.
   */
  manualImpressionsEnabled?: boolean;
}

/**
 * Response from requesting an ad
 */
export interface AdRequestResponse {
  /**
   * The unique request ID for the loaded ad
   */
  requestId: string;

  /**
   * Dimensions of the loaded ad
   */
  dimensions: {
    width: number;
    height: number;
  };
}
