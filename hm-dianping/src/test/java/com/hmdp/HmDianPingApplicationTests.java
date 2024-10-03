package com.hmdp;

import cn.hutool.core.collection.AvgPartition;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisIdWorker;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

@SpringBootTest
@RunWith(SpringRunner.class)
public class HmDianPingApplicationTests {

    @Resource
    private CacheClient cacheClient;
    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private RedisIdWorker redisIdWorkder;

    private ExecutorService es = Executors.newFixedThreadPool(500);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    public void testIdWorker() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(300);
        Runnable task = ()->{
            for(int i = 0;i<100;i++){
                long id = redisIdWorkder.nextId("order");
                System.out.println("id = " + id);
            }
            latch.countDown();
        };
        long begin = System.currentTimeMillis();
        for(int i = 0;i<300;i++){
            es.submit(task);
        }
        latch.await();
        long end = System.currentTimeMillis();
        System.out.println("time += "+ (end - begin));
    }

    @Test
    public void testSaveShop() throws InterruptedException {
        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(CACHE_SHOP_KEY + 1L,shop,10L, TimeUnit.SECONDS);
    }

    @Test
    public void loadShopData(){
        //1.查询店铺信息
        List<Shop> list = shopService.list();
        Map<Long,List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop :: getTypeId));

        for(Map.Entry<Long,List<Shop>> entry: map.entrySet()){
            Long typeId = entry.getKey();
            String key = "shop:geo:" + typeId;
            List<Shop> value = entry.getValue();
            List<RedisGeoCommands.GeoLocation<String>> locations = new ArrayList<>(value.size());
            //3.写入redis GEOADD
            for(Shop shop:value){
//                stringRedisTemplate.opsForGeo().add(key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
                locations.add(new RedisGeoCommands.GeoLocation<>(
                        shop.getId().toString(),
                        new Point(shop.getX(),shop.getY())
                ));
            }
            stringRedisTemplate.opsForGeo().add(key,locations);
        }

    }
}
