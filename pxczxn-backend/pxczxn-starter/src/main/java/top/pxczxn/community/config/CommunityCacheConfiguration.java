package top.pxczxn.community.config;
import org.springframework.cache.CacheManager; import org.springframework.cache.concurrent.ConcurrentMapCacheManager; import org.springframework.cache.annotation.EnableCaching; import org.springframework.context.annotation.*;
@Configuration @EnableCaching public class CommunityCacheConfiguration { @Bean CacheManager communityCacheManager(){return new ConcurrentMapCacheManager("publicEditorial");} }
