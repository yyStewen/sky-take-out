package com.sky.mapper;

import com.sky.entity.SetmealDish;
import org.apache.ibatis.annotations.Mapper;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface SetmealDishMapper {


    List<Long> getSetmealIdsByDishIds(List<Long> dishIds);

    void insertSetmealDishes(List<SetmealDish> setmealDishes);

    List<SetmealDish> getSetmealDishesBySetmealId(Long setmealId);

    void deleteBySetmealId(Long id);

    void deleteBySetmealIds(List<Long> setmealIds);

    List<Long> getDishIdsBySetmealIds(ArrayList<Long> setmealIds);
}
