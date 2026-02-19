package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;


    @Override
    public User wxLogin(UserLoginDTO userLoginDTO) {


        //1 调用微信接口服务获取当前微信用户的openid
       String openid = getOpenid(userLoginDTO.getCode());


        //2 判断openid是否为空，如果为空则登录失败，抛出业务异常
        if (openid == null) {
            //登录失败
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }




        //3 根据openid查询当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);

        //4 是新用户，自动完成注册
        if (user == null) {
            user=User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //5 登录成功，返回用户对象
        return user;
    }


    /**
     * 获取微信用户openid。
     * @param code
     * @return
     */
    private String getOpenid(String code) {
        //1 调用微信接口服务，获取当前微信用户openid
        Map<String, String> map = new HashMap<>();
        map.put("appid",  weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code" );
        String json = HttpClientUtil.doGet(WX_LOGIN, map);

        JSONObject jsonObject = JSONObject.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
