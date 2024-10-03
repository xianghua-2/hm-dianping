package com.hmdp.utils;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitMQExample {

    private final static String EXCHANGE_NAME = "seckill.order.exchange";
    private final static String QUEUE_NAME = "seckill.order.queue";
    private final static String ROUTING_KEY = "seckill.order";

    public static void main(String[] args) throws IOException, TimeoutException {
        // 创建连接工厂
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // 设置RabbitMQ服务器地址
        factory.setPort(5672); // 设置RabbitMQ服务器端口
        factory.setUsername("admin"); // 设置用户名
        factory.setPassword("123456"); // 设置密码
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            // 创建Direct类型交换机
            channel.exchangeDeclare(EXCHANGE_NAME, "direct");
            System.out.println("Exchange '" + EXCHANGE_NAME + "' declared.");

            // 创建消息队列
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            System.out.println("Queue '" + QUEUE_NAME + "' declared.");

            // 将队列绑定到交换机
            channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);
            System.out.println("Queue '" + QUEUE_NAME + "' bound to exchange '" + EXCHANGE_NAME + "' with routing key '" + ROUTING_KEY + "'.");

            // 发送消息到交换机
            String message = "Hello Seckill Order!";
            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, message.getBytes());
            System.out.println("Sent '" + message + "'");

            // 消费消息
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String receivedMessage = new String(delivery.getBody(), "UTF-8");
                System.out.println("Received '" + receivedMessage + "'");
            };
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        }
    }
}