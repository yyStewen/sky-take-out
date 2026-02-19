package com.sky.mapper;


import com.sky.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    //根据openid查询用户

    User getByOpenid(String openid);

    void insert(User user);
}
