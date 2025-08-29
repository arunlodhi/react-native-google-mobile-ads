"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
exports.BaseCachedAd = void 0;
var _react = _interopRequireWildcard(require("react"));
var _common = require("../common");
var _NativeError = require("../internal/NativeError");
var _GoogleMobileAdsCachedBannerViewNativeComponent = _interopRequireDefault(require("../specs/components/GoogleMobileAdsCachedBannerViewNativeComponent"));
var _AdRequestManager = require("./AdRequestManager");
function _interopRequireDefault(e) { return e && e.__esModule ? e : { default: e }; }
function _getRequireWildcardCache(e) { if ("function" != typeof WeakMap) return null; var r = new WeakMap(), t = new WeakMap(); return (_getRequireWildcardCache = function (e) { return e ? t : r; })(e); }
function _interopRequireWildcard(e, r) { if (!r && e && e.__esModule) return e; if (null === e || "object" != typeof e && "function" != typeof e) return { default: e }; var t = _getRequireWildcardCache(r); if (t && t.has(e)) return t.get(e); var n = { __proto__: null }, a = Object.defineProperty && Object.getOwnPropertyDescriptor; for (var u in e) if ("default" !== u && {}.hasOwnProperty.call(e, u)) { var i = a ? Object.getOwnPropertyDescriptor(e, u) : null; i && (i.get || i.set) ? Object.defineProperty(n, u, i) : n[u] = e[u]; } return n.default = e, t && t.set(e, n), n; }
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

const BaseCachedAd = exports.BaseCachedAd = /*#__PURE__*/_react.default.forwardRef(({
  requestId,
  ...props
}, ref) => {
  const [dimensions, setDimensions] = (0, _react.useState)([0, 0]);
  const [adExists, setAdExists] = (0, _react.useState)(false);
  (0, _react.useEffect)(() => {
    if (!requestId) {
      throw new Error("BaseCachedAd: 'requestId' expected a valid string request ID.");
    }

    // Check if ad exists
    const exists = (0, _AdRequestManager.hasAd)(requestId);
    setAdExists(exists);
    if (!exists) {
      // Call onAdFailedToLoad if ad doesn't exist
      if ((0, _common.isFunction)(props.onAdFailedToLoad)) {
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
    if ((0, _common.isFunction)(props[type])) {
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
          eventPayload = _NativeError.NativeError.fromEvent(nativeEvent, 'googleMobileAds');
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
  return /*#__PURE__*/_react.default.createElement(_GoogleMobileAdsCachedBannerViewNativeComponent.default, {
    ref: ref,
    style: style,
    requestId: requestId,
    onNativeEvent: onNativeEvent
  });
});
BaseCachedAd.displayName = 'BaseCachedAd';
//# sourceMappingURL=BaseCachedAd.js.map