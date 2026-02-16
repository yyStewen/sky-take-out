package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DishServiceImpl implements DishService {
    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Override
    @Transactional//涉及到多条sql语句，使用事务注解，确保数据的一致性
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);


        //向菜品表插入一条数据
        dishMapper.insert(dish);

        //向口味表插入n条数据
        List<DishFlavor> flavors = dishDTO.getFlavors();
        Long dishId = dish.getId();


        if(flavors != null && !flavors.isEmpty()){//判断集合是否为空
            //主键返回，设置id
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishId);
            }

            //直接传入集合对象批量插入
            dishFlavorMapper.insertBatch(flavors);

        }


    }

    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //使用Pagehelper分页查询
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());

        //执行查询
        List<DishVO> dishList = dishMapper.pageQuery(dishPageQueryDTO);

        //解析和封装结果
        Page<DishVO> page = (Page<DishVO>)dishList;
        return new PageResult(page.getTotal(), page.getResult());

    }

    /**
     * 批量删除
     * @param ids
     * 业务规则：
     * 可以一次删除一个菜品，也可以批量删除菜品
     * 起售中的菜品不能删除
     * 被套餐关联的菜品不能删除
     * 删除菜品后，关联的口味数据也需要删除掉
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //先判断菜品能否被删除--起售中状态
        //遍历id，如存在起售中菜品则不允许删除
        for (Long id : ids) {
            Dish dish = dishMapper.getById(id);
            if(dish.getStatus()==StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }

        //判断菜品是否被套餐关联
        List<Long> setmealIds = setmealDishMapper.getSetmealIdsByDishIds(ids);
        if(setmealIds != null && setmealIds.size() > 0){
            //起售中的菜品不能删除
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        //删除菜品的数据
//        for (Long id : ids) {
//            dishMapper.deleteById(id);
//            //删除菜品关联的口味数据
//            dishFlavorMapper.deleteByDishId(id);
//        }

        //根据id批量删除菜品
        dishMapper.deleteByIds(ids);
        //根据id批量删除口味
        dishFlavorMapper.deleteByDishIds(ids);


    }


    //查询并返回菜品和口味
    @Override
    public DishVO getById(Long id) {
        //先查询菜品基本信息并封装
        Dish dish = dishMapper.getById(id);

        //再查询到相关口味信息并封装
        List<DishFlavor> dishFlavors = dishFlavorMapper.getByDishId(id);

        //封装并返回数据
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(dishFlavors);
        return dishVO;

    }

    @Override
    public void updateWithFlavor(DishDTO dishDTO) {
        //修改菜品表基本信息
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);

        dishMapper.update(dish);
        //包含口味信息。先删除原有的口味，再添加口味信息
        dishFlavorMapper.deleteByDishId(dishDTO.getId());

        List<DishFlavor> flavors = dishDTO.getFlavors();
        if(flavors != null && !flavors.isEmpty()){
            //遍历 flavors，设置 dishId 属性
            for (DishFlavor flavor : flavors) {
                flavor.setDishId(dishDTO.getId());
            }
            //添加新的口味数据
            dishFlavorMapper.insertBatch(flavors);
        }

    }

    @Override
    public void updateStatus(Integer status, Long id) {
        //不需要创建新的mapper接口方法。直接使用已有的修改菜品。
        Dish dish = Dish.builder().id(id).status(status).build();
        dishMapper.update(dish);
    }
}
