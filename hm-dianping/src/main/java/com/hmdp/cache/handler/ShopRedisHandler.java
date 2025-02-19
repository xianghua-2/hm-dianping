package com.hmdp.cache.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@Component
public class ShopRedisHandler implements InitializingBean {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private IShopService shopService;


    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Resource
    private Cache<String, Object> shopCache;

    // 缓存预热

    @Override
    public void afterPropertiesSet() throws Exception {
        // 初始化缓存
        // 1.查询商品信息
        List<Shop> shopList = shopService.list();
        // 2.放入缓存
        for (Shop shop : shopList) {
            // 2.1.item序列化为JSON
            String json = MAPPER.writeValueAsString(shop);
            // 2.2 存入caffeind
            String key = CACHE_SHOP_KEY + shop.getId();
            shopCache.put(key, shop);
            // 2.2.存入redis
            redisTemplate.opsForValue().set(key, json);
        }

//        // 3.查询商品库存信息
//        List<ItemStock> stockList = stockService.list();
//        // 4.放入缓存
//        for (ItemStock stock : stockList) {
//            // 2.1.item序列化为JSON
//            String json = MAPPER.writeValueAsString(stock);
//            // 2.2.存入redis
//            redisTemplate.opsForValue().set("item:stock:id:" + stock.getId(), json);
//        }
    }

    public void saveShop(Shop shop) {
        try {
            String json = MAPPER.writeValueAsString(shop);
            String key = CACHE_SHOP_KEY  + shop.getId();
            redisTemplate.opsForValue().set(key + shop.getId(), json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteShopById(Long id) {
        String key = CACHE_SHOP_KEY  + id;
        redisTemplate.delete(key + id);
    }
}