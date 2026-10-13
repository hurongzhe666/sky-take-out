package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.impl.UserServiceImpl;
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
public class UserServiceImpl implements UserService{

    @Autowired
    WeChatProperties weChatProperties;
    @Autowired
    UserMapper userMapper;

    //微信服务接口地址
    public static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session";
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */

    public User wxLogin(UserLoginDTO userLoginDTO) {
        String openid = getOpenId(userLoginDTO.getCode());

        //去表里判断openid是否存在,如果为空则登录失败，这个用户登录不合法
        if (openid==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //若openid存在，则判断当前用户是否为新用户
        User user = userMapper.getByOpenId(openid);
        //是就自动注册
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //不是就直接返回
        return user;
    }

    /**
     * 调用微信接口根据code获取openid
     * @param code
     * @return
     */
    private String getOpenId(String code) {
        //通过微信接口服务，用前端的code和配置文件中两个值来获得微信用户的openid
        Map<String, String> map=new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code",code);
        map.put("grant_type", "authorization_code");
        String json=HttpClientUtil.doGet(WX_LOGIN,map);

        //将json字符串转换为json对象
        JSONObject jsonObject=JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;
    }
}
