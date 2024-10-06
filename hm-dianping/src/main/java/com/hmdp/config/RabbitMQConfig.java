package com.hmdp.config;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.IVoucherOrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.io.IOException;

@Configuration
public class RabbitMQConfig {

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
    public void recieveMessage(Message message, Channel channel, VoucherOrder voucherOrder){
        try {
            voucherOrderService.handleVoucherOrder(voucherOrder);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("监听到了"+message);
    }



    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
