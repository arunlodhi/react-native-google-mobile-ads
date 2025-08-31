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
import { isFunction } from '../common';
import { NativeError } from '../internal/NativeError';
import GoogleMobileAdsCachedBannerView from '../specs/components/GoogleMobileAdsCachedBannerViewNativeComponent';
import { getCachedAdInfo } from './CachedBannerAds';
export const CachedBaseAd = /*#__PURE__*/React.forwardRef(({
  requestId,
  ...props
}, ref) => {
  const [dimensions, setDimensions] = useState([0, 0]);
  const [currentRequestId, setCurrentRequestId] = useState(null);
  useEffect(() => {
    if (!requestId) {
      throw new Error("CachedBaseAd: 'requestId' expected a valid string.");
    }

    // Only reset dimensions if requestId actually changed
    if (currentRequestId !== requestId) {
      setCurrentRequestId(requestId);
      setDimensions([0, 0]); // Reset dimensions for new requestId
    }

    // Get cached ad info to determine dimensions
    getCachedAdInfo(requestId).then(adInfo => {
      console.log('CachedBaseAd: getCachedAdInfo success for', requestId, adInfo);
      if (adInfo.width && adInfo.height) {
        setDimensions([adInfo.width, adInfo.height]);
      }
    }).catch(error => {
      console.log('CachedBaseAd: getCachedAdInfo failed for', requestId, error);
      // Ad might not be loaded yet, dimensions will be set when onAdLoaded is called
    });
  }, [requestId, currentRequestId]);
  function onNativeEvent(event) {
    const nativeEvent = event.nativeEvent;
    const {
      type
    } = nativeEvent;
    let eventHandler, eventPayload;
    switch (type) {
      case 'onAdLoaded':
      case 'onSizeChange':
        eventPayload = {
          width: nativeEvent.width,
          height: nativeEvent.height
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
          data: nativeEvent.data
        };
        // onAppEvent only exists on GAM banner ads
        if ('onAppEvent' in props && (eventHandler = props.onAppEvent) && isFunction(eventHandler)) {
          eventHandler(eventPayload);
        }
        break;
      case 'onPaid':
        if ((eventHandler = props[type]) && isFunction(eventHandler)) {
          eventHandler({
            currency: nativeEvent.currency,
            precision: nativeEvent.precision,
            value: nativeEvent.value
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
  const style = {
    width: dimensions[0],
    height: dimensions[1]
  };
  return /*#__PURE__*/React.createElement(GoogleMobileAdsCachedBannerView, {
    ref: ref,
    style: style,
    requestId: requestId,
    onNativeEvent: onNativeEvent
  });
});
CachedBaseAd.displayName = 'CachedBaseAd';
//# sourceMappingURL=CachedBaseAd.js.map