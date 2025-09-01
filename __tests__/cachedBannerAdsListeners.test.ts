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

import {
  requestBannerAd,
  requestGAMBannerAd,
  addCachedAdListeners,
  removeCachedAdListeners,
  getCachedAdInfo,
} from '../src/ads/CachedBannerAds';
import { AdEventType } from '../src/AdEventType';
import { GAMAdEventType } from '../src/GAMAdEventType';
import { BannerAdSize } from '../src/BannerAdSize';

// Mock the native module
jest.mock('../src/specs/modules/NativeCachedBannerModule', () => ({
  __esModule: true,
  default: {
    requestCachedBannerAd: jest.fn(),
    getCachedAdInfo: jest.fn(),
    removeCachedAd: jest.fn(),
    getAllCachedAdIds: jest.fn(),
    clearAllCachedAds: jest.fn(),
  },
}));

// Mock SharedEventEmitter
jest.mock('../src/internal/SharedEventEmitter', () => ({
  SharedEventEmitter: {
    addListener: jest.fn(() => ({
      remove: jest.fn(),
    })),
  },
}));

describe('CachedBannerAds Listeners', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('addCachedAdListeners', () => {
    it('should throw error for invalid requestId', async () => {
      await expect(addCachedAdListeners('')).rejects.toThrow(
        'requestId must be a non-empty string',
      );
      await expect(addCachedAdListeners(null as any)).rejects.toThrow(
        'requestId must be a non-empty string',
      );
    });

    it('should return listener management object', async () => {
      const mockAdInfo = {
        requestId: 'test-request-id',
        unitId: 'test-unit-id',
        isLoaded: true,
      };

      const NativeCachedBannerModule =
        require('../src/specs/modules/NativeCachedBannerModule').default;
      NativeCachedBannerModule.getCachedAdInfo.mockResolvedValue(mockAdInfo);

      const listeners = await addCachedAdListeners('test-request-id');

      expect(listeners).toHaveProperty('addAdEventsListener');
      expect(listeners).toHaveProperty('addAdEventListener');
      expect(listeners).toHaveProperty('removeAllListeners');
      expect(typeof listeners.addAdEventsListener).toBe('function');
      expect(typeof listeners.addAdEventListener).toBe('function');
      expect(typeof listeners.removeAllListeners).toBe('function');
    });
  });

  describe('removeCachedAdListeners', () => {
    it('should throw error for invalid requestId', () => {
      expect(() => removeCachedAdListeners('')).toThrow('requestId must be a non-empty string');
      expect(() => removeCachedAdListeners(null as any)).toThrow(
        'requestId must be a non-empty string',
      );
    });

    it('should not throw error for valid requestId even if no listeners exist', () => {
      expect(() => removeCachedAdListeners('test-request-id')).not.toThrow();
    });
  });

  describe('listener functionality', () => {
    it('should add and manage event listeners', async () => {
      const mockAdInfo = {
        requestId: 'test-request-id',
        unitId: 'test-unit-id',
        isLoaded: true,
      };

      const NativeCachedBannerModule =
        require('../src/specs/modules/NativeCachedBannerModule').default;
      NativeCachedBannerModule.getCachedAdInfo.mockResolvedValue(mockAdInfo);

      const listeners = await addCachedAdListeners('test-request-id');

      // Test adding event listeners
      const mockEventListener = jest.fn();
      const mockEventsListener = jest.fn();

      const removeEventListener = listeners.addAdEventListener(
        AdEventType.LOADED,
        mockEventListener,
      );
      const removeEventsListener = listeners.addAdEventsListener(mockEventsListener);

      expect(typeof removeEventListener).toBe('function');
      expect(typeof removeEventsListener).toBe('function');

      // Test removing all listeners
      expect(() => listeners.removeAllListeners()).not.toThrow();
    });

    it('should validate event types', async () => {
      const mockAdInfo = {
        requestId: 'test-request-id',
        unitId: 'test-unit-id',
        isLoaded: true,
      };

      const NativeCachedBannerModule =
        require('../src/specs/modules/NativeCachedBannerModule').default;
      NativeCachedBannerModule.getCachedAdInfo.mockResolvedValue(mockAdInfo);

      const listeners = await addCachedAdListeners('test-request-id');

      // Test valid event types
      expect(() => listeners.addAdEventListener(AdEventType.LOADED, jest.fn())).not.toThrow();

      expect(() => listeners.addAdEventListener(GAMAdEventType.APP_EVENT, jest.fn())).not.toThrow();

      // Test invalid event type
      expect(() => listeners.addAdEventListener('invalid-event' as any, jest.fn())).toThrow(
        "CachedBannerAd.addAdEventListener(*) 'type' expected a valid event type value.",
      );
    });

    it('should validate listener functions', async () => {
      const mockAdInfo = {
        requestId: 'test-request-id',
        unitId: 'test-unit-id',
        isLoaded: true,
      };

      const NativeCachedBannerModule =
        require('../src/specs/modules/NativeCachedBannerModule').default;
      NativeCachedBannerModule.getCachedAdInfo.mockResolvedValue(mockAdInfo);

      const listeners = await addCachedAdListeners('test-request-id');

      // Test invalid listener functions
      expect(() => listeners.addAdEventListener(AdEventType.LOADED, null as any)).toThrow(
        "CachedBannerAd.addAdEventListener(_, *) 'listener' expected a function.",
      );

      expect(() => listeners.addAdEventsListener(null as any)).toThrow(
        "CachedBannerAd.addAdEventsListener(*) 'listener' expected a function.",
      );
    });
  });
});
