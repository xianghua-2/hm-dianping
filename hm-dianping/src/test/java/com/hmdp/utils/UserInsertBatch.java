package com.hmdp.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UserInsertBatch {
    // 数据库连接信息
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/hmdp?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASSWORD = "123456";

    public static void main(String[] args) {
        // 数据库连接
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {
            // 加载数据库驱动
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 建立数据库连接
            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            // 批量插入 SQL
            String sql = "INSERT INTO tb_user (phone, password, nick_name, icon, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, NOW(), NOW())";

            connection.setAutoCommit(false); // 关闭自动提交

            preparedStatement = connection.prepareStatement(sql);

            long startPhone = 13700000000L;

            for (int i = 1; i <= 10000; i++) {
                // 生成递增的手机号
//                String phone = "1360000" + String.format("%04d", i); // 示例：13600000001, 13600000002 ...
                long currentPhone = startPhone + (i-1);
                String phone = String.valueOf(currentPhone);
                // 生成昵称
                String nickName = "user_" + i;

                // 生成头像
                String icon = "/imgs/icons/user_" + i + ".jpg";

                // 设置参数
                preparedStatement.setString(1, phone);
                preparedStatement.setString(2, "");
                preparedStatement.setString(3, nickName);
                preparedStatement.setString(4, icon);

                // 添加到批处理
                preparedStatement.addBatch();

                // 每 1000 条提交一次事务
                if (i % 1000 == 0) {
                    preparedStatement.executeBatch();
                    connection.commit();
                    System.out.println("已插入 " + i + " 条数据");
                }
            }

            // 提交剩余的数据
            preparedStatement.executeBatch();
            connection.commit();

            System.out.println("批量插入完成！");

        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (preparedStatement != null) {
                    preparedStatement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}