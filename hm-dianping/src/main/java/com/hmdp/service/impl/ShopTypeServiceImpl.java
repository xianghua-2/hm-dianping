package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public ShopTypeServiceImpl(StringRedisTemplate stringRedisTemplate) {
    }

    @Override
    public Result queryTypeList() {
        //1.从redis中查询类别数据
        String cacheKey = CACHE_SHOP_TYPE_KEY;
        String shopTypeJson = stringRedisTemplate.opsForValue().get(cacheKey);

        //2.判断Redis中是否存在数据
        if(StrUtil.isNotBlank(shopTypeJson)){
            List<ShopType> shopTypes = JSONUtil.toList(shopTypeJson,ShopType.class);
            return Result.ok(shopTypes);
        } //JSONUtil.toList()将查询得到的json转换为ShopType类型的List存储起来。

        //2.2 不存在则从数据库中查询
        List<ShopType> shopTypes = query().orderByAsc("sort").list();

        //3.判断数据库中是否存在
        if(shopTypes == null){
            return Result.fail("分类不存在！");
        }
        //3.2存在，则存入redis中
        stringRedisTemplate.opsForValue().set(cacheKey,JSONUtil.toJsonStr(shopTypes));
        return Result.ok(shopTypes);
    }
}