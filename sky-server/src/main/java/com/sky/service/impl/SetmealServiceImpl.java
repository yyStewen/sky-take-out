package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SetmealServiceImpl implements SetmealService {
    //分页查询接口开发

    @Autowired
    SetmealMapper setmealMapper;

    @Autowired
    SetmealDishMapper setmealDishMapper;

    @Autowired
    DishMapper dishMapper;
    @Override
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {

        //Pagehelper分页查询
        PageHelper.startPage(setmealPageQueryDTO.getPage(),setmealPageQueryDTO.getPageSize());
        //执行查询

        List<SetmealVO> setmealVOList= setmealMapper.pageQuery(setmealPageQueryDTO);

        //处理结果并返回
        Page<SetmealVO> page=(Page<SetmealVO>) setmealVOList;
        return new PageResult(page.getTotal(), page.getResult());

    }

    @Override

    //新增套餐接口开发。新增的有套餐和多条套餐-菜品关系，考虑事务管理
    @Transactional
    public void saveWithSetmealDishes(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);

        //向套餐表插入数据
        setmealMapper.insert(setmeal);


        //向套餐-菜品关系表插入多条数据
        List<SetmealDish>setmealDishes = setmealDTO.getSetmealDishes();
        Long setmealId = setmeal.getId();//主键返回获取id

        for (SetmealDish setmealDish : setmealDishes) {
            setmealDish.setSetmealId(setmealId);
        }

        //批量插入
        setmealDishMapper.insertSetmealDishes(setmealDishes);
    }

    @Override
    @Transactional
    public SetmealVO getSetmealById(Long id) {//根据id查询套餐，执行多条sql语句考虑事务。
        SetmealVO setmealVO = new SetmealVO();
        //先查询菜品信息
        Setmeal setmeal = setmealMapper.getSetmealById(id);

        //同时还需要查询菜品信息并把它们封装
        List<SetmealDish> setmealDishes = setmealDishMapper.getSetmealDishesBySetmealId(id);
        BeanUtils.copyProperties(setmeal,setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    @Override
    @Transactional
    public void updateWithSetmealDishes(SetmealDTO setmealDTO) {//修改套餐接口开发，执行多条sql语句考虑事务
        //首先，完成套餐的修改
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO,setmeal);
        setmealMapper.update(setmeal);

        //然后，再完成对套餐关联的菜品的修改。一律删除后再重新插入
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());

        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if(setmealDishes != null && setmealDishes.size() > 0){
            for (SetmealDish setmealDish : setmealDishes) {
                setmealDish.setSetmealId(setmealDTO.getId());
            }//设置id
        }

        //批量插入
        setmealDishMapper.insertSetmealDishes(setmealDishes);

    }

    @Override
    @Transactional
    public void deleteBatch(List<Long> ids) {
        //首先判断能不能删除
        for (Long id : ids) {
            Setmeal setmeal = setmealMapper.getSetmealById(id);
            if(setmeal.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
            }
        }

        //根据id批量删除套餐
        setmealMapper.deleteByIds(ids);
        //根据关联的id删除对应的菜品套餐关系
        setmealDishMapper.deleteBySetmealIds(ids);
    }

    @Override
    public void startOrStop(Integer status, Long id) {
        //首先进行条件判断。如果是起售套餐，那么如果套餐中存在着停售的菜品，那么就不允许起售
        if(status == StatusConstant.ENABLE){ //起售
            //查询套餐中的菜品
            ArrayList<Long> ids = new ArrayList<>();
            ids.add(id);
            List<Long> dishIds = setmealDishMapper.getDishIdsBySetmealIds(ids);
            //判断是否存在停售的菜品
            for (Long dishId : dishIds) {
                Dish dish = dishMapper.getById(dishId);
                if(dish.getStatus() == StatusConstant.DISABLE){
                    throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ENABLE_FAILED);
                }

            }
            //直接用已经有的update方法修改即可。
            Setmeal setmeal = Setmeal.builder().id(id).status(status).build();
            setmealMapper.update(setmeal);
        }

    }

    /**
     * 条件查询
     * @param setmeal
     * @return
     */
    @Override
    public List<Setmeal> list(Setmeal setmeal) {
        List<Setmeal> list = setmealMapper.list(setmeal);
        return list;
    }

    /**
     * 根据id查询菜品选项
     * @param id
     * @return
     */
    @Override
    public List<DishItemVO> getDishItemById(Long id) {
        return setmealMapper.getDishItemBySetmealId(id);
    }



}

