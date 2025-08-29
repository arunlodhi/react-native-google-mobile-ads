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
import { hasAd } from './AdRequestManager';
export const BaseCachedAd = /*#__PURE__*/React.forwardRef(({
  requestId,
  ...props
}, ref) => {
  const [dimensions, setDimensions] = useState([0, 0]);
  const [adExists, setAdExists] = useState(false);
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
        var _props$onAdFailedToLo;
        const error = new Error(`Ad with requestId '${requestId}' not found or has been released.`);
        (_props$onAdFailedToLo = props.onAdFailedToLoad) === null || _props$onAdFailedToLo === void 0 || _props$onAdFailedToLo.call(props, error);
      }
    }
  }, [requestId, props.onAdFailedToLoad]);
  function onNativeEvent(event) {
    const nativeEvent = event.nativeEvent;
    const {
      type
    } = nativeEvent;
    if (isFunction(props[type])) {
      let eventHandler, eventPayload;
      switch (type) {
        case 'onAdLoaded':
        case 'onSizeChange':
          eventPayload = {
            width: nativeEvent.width,
            height: nativeEvent.height
          };
          if (eventHandler = props[type]) eventHandler(eventPayload);
          break;
        case 'onAdFailedToLoad':
          eventPayload = NativeError.fromEvent(nativeEvent, 'googleMobileAds');
          if (eventHandler = props[type]) eventHandler(eventPayload);
          break;
        case 'onAppEvent':
          eventPayload = {
            name: nativeEvent.name,
            data: nativeEvent.data
          };
          if (eventHandler = props[type]) eventHandler(eventPayload);
          break;
        case 'onPaid':
          const handler = props[type];
          if (handler) {
            handler({
              currency: nativeEvent.currency,
              precision: nativeEvent.precision,
              value: nativeEvent.value
            });
          }
          break;
        default:
          if (eventHandler = props[type]) eventHandler();
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
    height: dimensions[1]
  };
  return /*#__PURE__*/React.createElement(GoogleMobileAdsCachedBannerView, {
    ref: ref,
    style: style,
    requestId: requestId,
    onNativeEvent: onNativeEvent
  });
});
BaseCachedAd.displayName = 'BaseCachedAd';
//# sourceMappingURL=BaseCachedAd.js.map