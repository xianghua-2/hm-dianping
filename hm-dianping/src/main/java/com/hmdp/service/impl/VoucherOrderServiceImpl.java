package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
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
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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


    // 初始化lua脚本
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;
    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //=========================================1.使用阻塞队列实现异步秒杀=======================================
/*    //初始化阻塞队列
    private BlockingQueue<VoucherOrder> orderTasks = new ArrayBlockingQueue<>(1024*1024);

    //异步处理线程池
    private static final ExecutorService SECKILL_ORDER_EXECUTOR = Executors.newSingleThreadExecutor();


    //在类初始化之后执行，因为当这个类初始化好了之后，随时都是有可能要执行的
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


    //==================== 使用stream消息队列实现异步秒杀

/*    private class VoucherOrderHandler implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    // 1.获取消息队列中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 >
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1).block(Duration.ofSeconds(2)),
                            StreamOffset.create("stream.orders", ReadOffset.lastConsumed())
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有消息，继续下一次循环
                        continue;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    createVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理订单异常", e);
                    //处理异常消息
                    handlePendingList();
                }
            }
        }

        private void handlePendingList() {
            while (true) {
                try {
                    // 1.获取pending-list中的订单信息 XREADGROUP GROUP g1 c1 COUNT 1 BLOCK 2000 STREAMS s1 0
                    List<MapRecord<String, Object, Object>> list = stringRedisTemplate.opsForStream().read(
                            Consumer.from("g1", "c1"),
                            StreamReadOptions.empty().count(1),
                            StreamOffset.create("stream.orders", ReadOffset.from("0"))
                    );
                    // 2.判断订单信息是否为空
                    if (list == null || list.isEmpty()) {
                        // 如果为null，说明没有异常消息，结束循环
                        break;
                    }
                    // 解析数据
                    MapRecord<String, Object, Object> record = list.get(0);
                    Map<Object, Object> value = record.getValue();
                    VoucherOrder voucherOrder = BeanUtil.fillBeanWithMap(value, new VoucherOrder(), true);
                    // 3.创建订单
                    createVoucherOrder(voucherOrder);
                    // 4.确认消息 XACK
                    stringRedisTemplate.opsForStream().acknowledge("s1", "g1", record.getId());
                } catch (Exception e) {
                    log.error("处理pendding订单异常", e);
                    try{
                        Thread.sleep(20);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }*/



    //======================================2.使用RabbitMQ实现消息队列===================================
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


