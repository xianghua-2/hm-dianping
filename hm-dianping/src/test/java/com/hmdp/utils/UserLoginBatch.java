package com.hmdp.utils;

import com.hmdp.entity.User;
import com.hmdp.service.IUserService;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;

import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedWriter;
import java.io.FileWriter;

import java.util.List;
import com.hmdp.entity.PageResult;

@SpringBootTest
public class UserLoginBatch {

    @Autowired
    private IUserService userService;

    @Test
    public void function(){
        String loginUrl = "http://localhost:8080/api/user/login"; // 替换为实际的登录URL
        String tokenFilePath = "tokens.txt"; // 存储Token的文件路径

        try {
            HttpClient httpClient = HttpClients.createDefault();

            BufferedWriter writer = new BufferedWriter(new FileWriter(tokenFilePath));

            int batchSize = 1000;// 每次处理1k个用户
            int pageNo = 1;

            while(true){
                // 从数据库中获取用户手机号
//                List<User> users = userService.list();
                PageResult pageResult = userService.listByPage(pageNo, batchSize);
                pageNo++;
                List<User> users = pageResult.getRecords();
                if(users.size() == 0) {
                    break;
                }
                for(User user : users) {
                    String phoneNumber = user.getPhone();

                    // 构建登录请求
                    HttpPost httpPost = new HttpPost(loginUrl);
                    //（1.如果作为请求参数传递）
                    //List<NameValuePair> params = new ArrayList<>();
                    //params.add(new BasicNameValuePair("phone", phoneNumber));
                    // 如果登录需要提供密码，也可以添加密码参数
                    // params.add(new BasicNameValuePair("password", "user_password"));
                    //httpPost.setEntity(new UrlEncodedFormEntity(params));
                    // (2.如果作为请求体传递)构建请求体JSON对象
                    JSONObject jsonRequest = new JSONObject();
                    jsonRequest.put("phone", phoneNumber);
                    StringEntity requestEntity = new StringEntity(
                            jsonRequest.toString(),
                            ContentType.APPLICATION_JSON);
                    httpPost.setEntity(requestEntity);

                    // 发送登录请求
                    HttpResponse response = httpClient.execute(httpPost);

                    // 处理登录响应，获取token
                    if (response.getStatusLine().getStatusCode() == 200) {
                        HttpEntity entity = response.getEntity();
                        String responseString = EntityUtils.toString(entity);
                        System.out.println(responseString);
                        // 解析响应，获取token，这里假设响应是JSON格式的
                        // 根据实际情况使用合适的JSON库进行解析
                        String token = parseTokenFromJson(responseString);
                        System.out.println("手机号 " + phoneNumber + " 登录成功，Token: " + token);
                        // 将token写入txt文件
                        writer.write(token);
                        writer.newLine();
                    } else {
                        System.out.println("手机号 " + phoneNumber + " 登录失败");
                    }
                }
            }


            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // 解析JSON响应获取token的方法，这里只是示例，具体实现需要根据实际响应格式进行解析
    private static String parseTokenFromJson(String json) {
        try {
            // 将JSON字符串转换为JSONObject
            JSONObject jsonObject = new JSONObject(json);
            // 从JSONObject中获取名为"token"的字段的值
            String token = jsonObject.getString("data");
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null; // 解析失败，返回null或者抛出异常，具体根据实际需求处理
        }
    }
}