package com.hmdp.listener;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@Slf4j
@Component
public class seckillvoucherListener {

    @Resource
    IVoucherOrderService voucherOrderService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.seckill.queue"),
            key = "direct.seckill",
            exchange = @Exchange(name = "hmdianping.direct", type = ExchangeTypes.DIRECT)
    ))
/*    public void recieveMessage(Object message){
        System.out.println("监听到了"+message);
    }*/
    public void recieveMessage(Message message, Channel channel, VoucherOrder voucherOrder) throws IOException {

//        voucherOrderService.handleVoucherOrder(voucherOrder);

        try {
//            throw new RuntimeException("测试异常");
            voucherOrderService.handleVoucherOrder(voucherOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("监听到了"+message);
    }




 }
