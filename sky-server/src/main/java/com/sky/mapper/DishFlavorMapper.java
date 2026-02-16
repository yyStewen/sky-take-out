package com.sky.mapper;

import com.sky.entity.DishFlavor;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface DishFlavorMapper {


    void insertBatch(List<DishFlavor> flavors);

    //删除，根据菜品id删除口味数据
    void deleteByDishId(Long dishId);

    void deleteByDishIds(List<Long> dishIds);

    List<DishFlavor> getByDishId(Long dishId);
}
