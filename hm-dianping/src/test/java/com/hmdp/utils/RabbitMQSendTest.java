package com.hmdp.utils;


import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
//@RunWith(SpringRunner.class)
public class RabbitMQSendTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;



    @Test
    public void testSendDirectMessage() {
        // 交换机名称
        String exchangeName = "hmdianping.direct";
//        String queueName = "direct.test.queue1";
        // 路由键
        String routingKey = "info";
        // 消息
        String message = "Hello, RabbitMQ!";


        // 发送消息
        rabbitTemplate.convertAndSend(exchangeName, routingKey, message);
    }
}
