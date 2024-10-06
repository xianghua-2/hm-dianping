package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private IVoucherOrderService iVoucherOrderService;

    @Resource
    private RedisIdWorker redisIdWorker;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;


    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);

    //异步处理线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();


    /*//在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
    @PostConstruct
    private void init() {
        SECKILL_ORDER_EXECUTOR.submit(new VoucherOrderHandler());
    }

    // 用于线程池处理的任务
// 当初始化完毕后，就会去从对列中去拿信息
    private class VoucherOrderHandler implements Runnable{
        public void run(){
            while(true){
                //1.获取队列中的信息
                try {
                    VoucherOrder voucherOrder =  orderTasks.take();
                    //2.创建订单
                    handleVoucherOrder(voucherOrder);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }*/

    @Transactional
    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        //1.所有信息从当前消息实体中拿
        Long voucherId = voucherOrder.getVoucherId();
        //2.扣减库存
        boolean success = seckillVoucherService.update().setSql("stock=stock-1")
                .eq("voucher_id", voucherId)
                //======判断当前库存是否大于0就可以决定是否能抢池子中的券了
                .gt("stock", 0)
                .update();
        //3.创建订单
        if(success) save(voucherOrder);
    }

    @Resource
    RabbitTemplate rabbitTemplate;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //1.执行lua脚本，判断当前用户的购买资格
        Long userId = UserHolder.getUser().getId();
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(), userId.toString());
        if (result != 0) {
            //2.不为0说明没有购买资格
            return Result.fail(result==1?"库存不足":"不能重复下单");
        }
        //3.走到这一步说明有购买资格，将订单信息存到消息队列
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        //存入消息队列等待异步消费
        rabbitTemplate.convertAndSend("hmdianping.direct","direct.seckill",voucherOrder);
        return Result.ok(orderId);
    }

/*    @Override
    public Result seckillVoucher(Long voucherId) {
        //获取用户
        Long userId = UserHolder.getUser().getId();
        long orderId = redisIdWorker.nextId("order");
        //1.执行lua脚本
        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                voucherId.toString(),userId.toString(),
                String.valueOf(orderId)
        );
        int r = result.intValue();
        //2.判断结果是否为0
        if(r != 0){
            //2.1 不为0 ,代表没有购买资格
            return Result.fail(r == 1?"库存不足":"不能重复下单");
        }
        //保存阻塞队列
        VoucherOrder voucherOrder = new VoucherOrder();

        voucherOrder.setId(orderId);
        // 2.4.用户id
        voucherOrder.setUserId(userId);
        // 2.5.代金券id
        voucherOrder.setVoucherId(voucherId);
        // 2.6.放入阻塞队列
        orderTasks.add(voucherOrder);
        //3.获取代理对象
        proxy = (IVoucherOrderService)AopContext.currentProxy();

        //3.返回订单id
        return Result.ok(orderId);
    }*/
/*    @Override
    public Result seckillVoucher(Long voucherId) {
        //1. 查询优惠券
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            //尚未开始
            return Result.fail("秒杀尚未开始！");
        }
        //3.判断秒杀是否已经结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            // 尚未开始
            return Result.fail("秒杀已经结束！");
        }
        //4.判断库存是否充足
        if (voucher.getStock() < 1) {
            // 库存不足
            return Result.fail("库存不足！");
        }

        Long userId = UserHolder.getUser().getId();

        //创建锁对象 这个代码不用了，因为我们现在要使用分布式锁
        //SimpleRedisLock lock = new SimpleRedisLock("order:" + userId, stringRedisTemplate);
        RLock lock = redissonClient.getLock("lock:order:" + userId);
        //获取锁
        boolean isLock =  lock.tryLock();
        if(!isLock){
            //获取锁失败，返回错误或重试
            return Result.fail("不允许重复下单");
        }
        try{
            //获取代理对象（事务）
            IVoucherOrderService prox = (IVoucherOrderService) AopContext.currentProxy();
            return prox.createVoucherOrder(voucherId);
        }finally {
            //释放锁
            lock.unlock();
        }


*//*        synchronized (userId.toString().intern()){
            //获取代理对象（事务）
            IVoucherOrderService prox = (IVoucherOrderService) AopContext.currentProxy();
            return prox.createVoucherOrder(voucherId);
        }*//*
    }*/

    @Transactional
    public void createVoucherOrder(VoucherOrder voucherOrder){
        // 5.一人一单逻辑
        // 5.1.用户id
        Long userId = voucherOrder.getId();

        int count = query().eq("user_id", userId).eq("voucher_id", voucherOrder).count();
        // 5.2.判断是否存在
        if (count > 0) {
            // 用户已经购买过了
            log.error("用户已经购买过一次！");
            return ;
        }

        //6.扣减库存
        boolean success = seckillVoucherService.update()
                .setSql("stock= stock -1") // set stock = stock -1
                .eq("voucher_id", voucherOrder)
                .gt("stock",0)// where id = ? and stock > 0
                .update();
        if (!success) {
            //扣减库存
            log.error("库存不足！");
            return ;
        }


        save(voucherOrder);

    }
}