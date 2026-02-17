package com.sky.service;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.vo.SetmealVO;

import java.util.List;

public interface SetmealService {
    PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    void saveWithSetmealDishes(SetmealDTO setmealDTO);

    SetmealVO getSetmealById(Long id);

    void updateWithSetmealDishes(SetmealDTO setmealDTO);

    void deleteBatch(List<Long> ids);

    void startOrStop(Integer status, Long id);
}
