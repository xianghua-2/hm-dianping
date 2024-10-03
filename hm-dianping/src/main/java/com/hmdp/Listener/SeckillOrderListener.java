/*
package com.hmdp.Listener;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SeckillOrderListener {
    @Autowired
    VoucherOrderServiceImpl voucherOrderService;
    @RabbitListener(queues = {"seckill.order.queue"})
    public void recieveMessage(Message message, Channel channel, VoucherOrder voucherOrder){
        try {
            voucherOrderService.handleVoucherOrder(voucherOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}*/
