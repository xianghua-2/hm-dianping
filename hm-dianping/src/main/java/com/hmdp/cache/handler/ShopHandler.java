package com.hmdp.cache.handler;

import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.entity.Shop;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.javatool.canal.client.annotation.CanalTable;
import top.javatool.canal.client.handler.EntryHandler;

import javax.annotation.Resource;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@CanalTable(value = "tb_shop")
@Component
public class ShopHandler implements EntryHandler<Shop>{


    @Autowired
    private ShopRedisHandler redisHandler;

    @Resource
    private Cache<String, Object> shopCache;


    @Override
    public void insert(Shop shop) {
        // 写数据到JVM进程缓存
        String key = CACHE_SHOP_KEY + shop.getId();
        shopCache.put(key , shop);
        // 写数据到redis
        redisHandler.saveShop(shop);
    }

    @Override
    public void update(Shop before, Shop after) {
        // 写数据到JVM进程缓存
        String key = CACHE_SHOP_KEY + after.getId();
        shopCache.put(key, after);
        // 写数据到redis
        redisHandler.saveShop(after);
    }

    @Override
    public void delete(Shop shop) {
        // 删除数据到JVM进程缓存
        String key = CACHE_SHOP_KEY + shop.getId();
        shopCache.invalidate(key);
        // 删除数据到redis
        redisHandler.deleteShopById(shop.getId());
    }
}
