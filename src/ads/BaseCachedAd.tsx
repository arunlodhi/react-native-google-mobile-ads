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
import { CachedGAMBannerAdProps } from '../types/CachedAdProps';
import { hasAd } from './AdRequestManager';

export const BaseCachedAd = React.forwardRef<
  React.ElementRef<typeof GoogleMobileAdsCachedBannerView>,
  CachedGAMBannerAdProps
>(({ requestId, ...props }, ref) => {
  const [dimensions, setDimensions] = useState<(number | DimensionValue)[]>([0, 0]);
  const [adExists, setAdExists] = useState<boolean>(false);

  useEffect(() => {
    if (!requestId) {
      throw new Error("BaseCachedAd: 'requestId' expected a valid string request ID.");
    }

    // Check if ad exists
    const exists = hasAd(requestId);
    setAdExists(exists);

    if (!exists) {
      // Call onAdFailedToLoad if ad doesn't exist
      if (isFunction(props.onAdFailedToLoad)) {
        const error = new Error(`Ad with requestId '${requestId}' not found or has been released.`);
        props.onAdFailedToLoad?.(error);
      }
    }
  }, [requestId, props.onAdFailedToLoad]);

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

    if (isFunction(props[type])) {
      let eventHandler, eventPayload;
      switch (type) {
        case 'onAdLoaded':
        case 'onSizeChange':
          eventPayload = {
            width: nativeEvent.width,
            height: nativeEvent.height,
          };
          if ((eventHandler = props[type])) eventHandler(eventPayload);
          break;
        case 'onAdFailedToLoad':
          eventPayload = NativeError.fromEvent(nativeEvent, 'googleMobileAds');
          if ((eventHandler = props[type])) eventHandler(eventPayload);
          break;
        case 'onAppEvent':
          eventPayload = {
            name: nativeEvent.name,
            data: nativeEvent.data,
          };
          if ((eventHandler = props[type])) eventHandler(eventPayload);
          break;
        case 'onPaid':
          const handler = props[type];
          if (handler) {
            handler({
              currency: nativeEvent.currency,
              precision: nativeEvent.precision,
              value: nativeEvent.value,
            });
          }
          break;
        default:
          if ((eventHandler = props[type])) eventHandler();
      }
    }

    if (type === 'onAdLoaded' || type === 'onSizeChange') {
      const width = Math.ceil(nativeEvent.width);
      const height = Math.ceil(nativeEvent.height);

      if (width && height && JSON.stringify([width, height]) !== JSON.stringify(dimensions)) {
        setDimensions([width, height]);
      }
    }
  }

  // Don't render if ad doesn't exist
  if (!adExists) {
    return null;
  }

  const style = {
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

BaseCachedAd.displayName = 'BaseCachedAd';
