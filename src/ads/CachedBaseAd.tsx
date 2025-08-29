/* eslint-disable react/prop-types */
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

import React, { useState, useEffect } from 'react';
import { DimensionValue, NativeSyntheticEvent } from 'react-native';
import { isFunction } from '../common';
import { RevenuePrecisions } from '../common/constants';
import { NativeError } from '../internal/NativeError';
import GoogleMobileAdsCachedBannerView from '../specs/components/GoogleMobileAdsCachedBannerViewNativeComponent';
import type { NativeEvent } from '../specs/components/GoogleMobileAdsCachedBannerViewNativeComponent';
import { CachedGAMBannerAdProps } from './CachedGAMBannerAd';
import { CachedBannerAdProps } from './CachedBannerAd';
import { getCachedAdInfo } from './CachedBannerAds';

type CachedBaseAdProps = CachedGAMBannerAdProps | CachedBannerAdProps;

export const CachedBaseAd = React.forwardRef<
  React.ElementRef<typeof GoogleMobileAdsCachedBannerView>,
  CachedBaseAdProps
>(({ requestId, ...props }, ref) => {
  const [dimensions, setDimensions] = useState<(number | DimensionValue)[]>([0, 0]);
  const [isFluid, setIsFluid] = useState(false);

  useEffect(() => {
    if (!requestId) {
      throw new Error("CachedBaseAd: 'requestId' expected a valid string.");
    }

    // Get cached ad info to determine dimensions
    getCachedAdInfo(requestId)
      .then(adInfo => {
        if (adInfo.width && adInfo.height) {
          setDimensions([adInfo.width, adInfo.height]);
        }
      })
      .catch(() => {
        // Ad might not be loaded yet, dimensions will be set when onAdLoaded is called
      });
  }, [requestId]);

  function onNativeEvent(event: NativeSyntheticEvent<NativeEvent>) {
    const nativeEvent = event.nativeEvent as
      | {
          type: 'onAdLoaded' | 'onSizeChange';
          width: number;
          height: number;
        }
      | { type: 'onAdOpened' | 'onAdClosed' | 'onAdImpression' | 'onAdClicked' }
      | {
          type: 'onAdFailedToLoad';
          code: string;
          message: string;
        }
      | {
          type: 'onAppEvent';
          name: string;
          data?: string;
        }
      | {
          type: 'onPaid';
          currency: string;
          precision: RevenuePrecisions;
          value: number;
        };
    const { type } = nativeEvent;

    let eventHandler, eventPayload;
    switch (type) {
      case 'onAdLoaded':
      case 'onSizeChange':
        eventPayload = {
          width: nativeEvent.width,
          height: nativeEvent.height,
        };
        if ((eventHandler = props[type]) && isFunction(eventHandler)) {
          eventHandler(eventPayload);
        }
        break;
      case 'onAdFailedToLoad':
        eventPayload = NativeError.fromEvent(nativeEvent, 'googleMobileAds');
        if ((eventHandler = props[type]) && isFunction(eventHandler)) {
          eventHandler(eventPayload);
        }
        break;
      case 'onAppEvent':
        eventPayload = {
          name: nativeEvent.name,
          data: nativeEvent.data,
        };
        // onAppEvent only exists on GAM banner ads
        if (
          'onAppEvent' in props &&
          (eventHandler = props.onAppEvent) &&
          isFunction(eventHandler)
        ) {
          eventHandler(eventPayload);
        }
        break;
      case 'onPaid':
        if ((eventHandler = props[type]) && isFunction(eventHandler)) {
          eventHandler({
            currency: nativeEvent.currency,
            precision: nativeEvent.precision,
            value: nativeEvent.value,
          });
        }
        break;
      case 'onAdOpened':
      case 'onAdClosed':
      case 'onAdImpression':
      case 'onAdClicked':
        if ((eventHandler = props[type]) && isFunction(eventHandler)) {
          eventHandler();
        }
        break;
    }

    if (type === 'onAdLoaded' || type === 'onSizeChange') {
      const width = Math.ceil(nativeEvent.width);
      const height = Math.ceil(nativeEvent.height);

      if (width && height && JSON.stringify([width, height]) !== JSON.stringify(dimensions)) {
        setDimensions([width, height]);
      }
    }
  }

  const style = isFluid
    ? {
        width: '100%' as DimensionValue,
        height: dimensions[1],
      }
    : {
        width: dimensions[0],
        height: dimensions[1],
      };

  return (
    <GoogleMobileAdsCachedBannerView
      ref={ref}
      style={style}
      requestId={requestId}
      onNativeEvent={onNativeEvent}
    />
  );
});
CachedBaseAd.displayName = 'CachedBaseAd';
