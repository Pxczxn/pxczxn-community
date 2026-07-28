package top.pxczxn.community.web.performance;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Keeps time-bound editorial collections fresh without discarding write-through invalidation. */
@Component
@RequiredArgsConstructor
public class PublicEditorialCacheExpiry {
    private final CacheManager cacheManager;

    @Scheduled(fixedRate = 60_000)
    public void evictExpiredEditorialSnapshots() {
        Cache cache = cacheManager.getCache("publicEditorial");
        if (cache != null) {
            cache.clear();
        }
    }
}
