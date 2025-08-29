import React from 'react';
import { BannerAdProps } from '../types/BannerAdProps';
export interface CachedBannerAdProps extends Omit<BannerAdProps, 'unitId' | 'size'> {
    /**
     * The request ID of the cached ad to display
     */
    requestId: string;
}
export declare class CachedBannerAd extends React.Component<CachedBannerAdProps> {
    private ref;
    render(): React.JSX.Element;
}
//# sourceMappingURL=CachedBannerAd.d.ts.map