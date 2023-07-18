package com.jawnz.gate.config;

import com.google.code.ssm.CacheFactory;
import com.google.code.ssm.config.DefaultAddressProvider;
import com.google.code.ssm.providers.xmemcached.MemcacheClientFactoryImpl;
import com.google.code.ssm.providers.xmemcached.XMemcachedConfiguration;
import com.google.code.ssm.spring.SSMCache;
import com.google.code.ssm.spring.SSMCacheManager;
import com.jawnz.gate.repository.UserRepository;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cache.support.NoOpCacheManager;
import org.springframework.context.annotation.*;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private GitProperties gitProperties;
    private BuildProperties buildProperties;

    private final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    public CacheManager memcachedCacheManager(JHipsterProperties jHipsterProperties, List<CacheFactory> caches) throws Exception {
        if (!jHipsterProperties.getCache().getMemcached().isEnabled()) {
            // Note that Memcached cannot work with Spring Boot devtools
            // So it should be disabled in development mode
            log.debug("Memcached is disabled");
            return new NoOpCacheManager();
        }
        log.debug("Starting Memcached configuration");
        SSMCacheManager cacheManager = new SSMCacheManager();
        List<SSMCache> ssmCaches = new ArrayList<>();
        for (CacheFactory cache : caches) {
            SSMCache ssmCache = new SSMCache(cache.getObject(), jHipsterProperties.getCache().getMemcached().getExpiration(), false);

            ssmCaches.add(ssmCache);
        }
        cacheManager.setCaches(ssmCaches);
        return cacheManager;
    }

    @Bean
    CacheFactory defaultCacheFactory(JHipsterProperties jHipsterProperties) {
        return createCache("default", jHipsterProperties);
    }

    @Bean
    public CacheFactory usersByLoginCache(JHipsterProperties jHipsterProperties) {
        return this.createCache(UserRepository.USERS_BY_LOGIN_CACHE, jHipsterProperties);
    }

    @Bean
    public CacheFactory usersByEmailCache(JHipsterProperties jHipsterProperties) {
        return this.createCache(UserRepository.USERS_BY_EMAIL_CACHE, jHipsterProperties);
    }

    private CacheFactory createCache(String cacheName, JHipsterProperties jHipsterProperties) {
        if (!jHipsterProperties.getCache().getMemcached().isEnabled()) {
            // Note that Memcached cannot work with Spring Boot devtools
            // So it should be disabled in development mode
            return null;
        }
        CacheFactory defaultCache = new CacheFactory();
        defaultCache.setCacheName(cacheName);
        defaultCache.setCacheClientFactory(new MemcacheClientFactoryImpl());

        DefaultAddressProvider addressProvider = new DefaultAddressProvider();
        addressProvider.setAddress(jHipsterProperties.getCache().getMemcached().getServers());
        defaultCache.setAddressProvider(addressProvider);

        Map<InetSocketAddress, AuthInfo> authInfoMap = new HashMap<>();

        if (jHipsterProperties.getCache().getMemcached().getAuthentication().isEnabled()) {
            InetSocketAddress memcachedServers = AddrUtil.getOneAddress(jHipsterProperties.getCache().getMemcached().getServers());
            AuthInfo authInfo = AuthInfo.plain(
                jHipsterProperties.getCache().getMemcached().getAuthentication().getUsername(),
                jHipsterProperties.getCache().getMemcached().getAuthentication().getPassword()
            );

            authInfoMap.put(memcachedServers, authInfo);
        }
        XMemcachedConfiguration cacheConfiguration = new XMemcachedConfiguration();
        if (!authInfoMap.isEmpty()) {
            cacheConfiguration.setAuthInfoMap(authInfoMap);
        }
        cacheConfiguration.setConsistentHashing(true);
        cacheConfiguration.setUseBinaryProtocol(jHipsterProperties.getCache().getMemcached().isUseBinaryProtocol());
        defaultCache.setConfiguration(cacheConfiguration);

        return defaultCache;
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }
}
