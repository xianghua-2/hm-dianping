package com.hmdp.config;

import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQTset {
    // 利用RabbitListener来声明要监听的队列信息
    // 将来一旦监听的队列中有了消息，就会推送给当前服务，调用当前方法，处理消息。
    // 可以看到方法体中接收的就是消息体的内容

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 测试消息接收
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.test.queue"),
            exchange = @Exchange(name = "hmdianping.direct", type = ExchangeTypes.DIRECT)
    ))
    public void listenDirectQueue1(String msg) {
        System.out.println("消费者1接收到direct.queue1的消息：【" + msg + "】");
    }

    // 测试消息接收
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.test.queue2"),
            exchange = @Exchange(name = "hmdianping.direct", type = ExchangeTypes.DIRECT)
    ))
    public void listenDirectQueue2(String msg) {
        System.out.println("消费者2接收到direct.queue2的消息：【" + msg + "】");
    }
}