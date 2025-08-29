import React from 'react';
import { GAMBannerAdProps } from '../types/BannerAdProps';
export interface CachedGAMBannerAdProps extends Omit<GAMBannerAdProps, 'unitId' | 'sizes'> {
    /**
     * The request ID of the cached ad to display
     */
    requestId: string;
}
export declare class CachedGAMBannerAd extends React.Component<CachedGAMBannerAdProps> {
    private ref;
    recordManualImpression(): void;
    render(): React.JSX.Element;
}
//# sourceMappingURL=CachedGAMBannerAd.d.ts.map