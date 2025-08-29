import type { TurboModule } from 'react-native';
import type { UnsafeObject } from 'react-native/Libraries/Types/CodegenTypes';
export interface CachedAdInfo {
    requestId: string;
    unitId: string;
    isLoaded: boolean;
    width?: number;
    height?: number;
}
export interface Spec extends TurboModule {
    requestCachedBannerAd(config: UnsafeObject): Promise<CachedAdInfo>;
    getCachedAdInfo(requestId: string): Promise<CachedAdInfo>;
    removeCachedAd(requestId: string): Promise<void>;
    getAllCachedAdIds(): Promise<string[]>;
    clearAllCachedAds(): Promise<void>;
}
declare const _default: Spec;
export default _default;
//# sourceMappingURL=NativeCachedBannerModule.d.ts.map