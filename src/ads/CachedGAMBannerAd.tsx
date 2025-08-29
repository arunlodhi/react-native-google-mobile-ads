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

import React, { createRef } from 'react';
import { GAMBannerAdProps } from '../types/BannerAdProps';
import { CachedBaseAd } from './CachedBaseAd';
import GoogleMobileAdsCachedBannerView, {
  Commands,
} from '../specs/components/GoogleMobileAdsCachedBannerViewNativeComponent';

export interface CachedGAMBannerAdProps extends Omit<GAMBannerAdProps, 'unitId' | 'sizes'> {
  /**
   * The request ID of the cached ad to display
   */
  requestId: string;
}

export class CachedGAMBannerAd extends React.Component<CachedGAMBannerAdProps> {
  private ref = createRef<React.ElementRef<typeof GoogleMobileAdsCachedBannerView>>();

  recordManualImpression() {
    if (this.ref.current) {
      Commands.recordManualImpression(this.ref.current);
    }
  }

  render() {
    return <CachedBaseAd ref={this.ref} {...this.props} />;
  }
}
