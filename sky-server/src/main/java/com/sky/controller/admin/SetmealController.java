package com.sky.controller.admin;

import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 套餐管理
 */

@RestController
@RequestMapping("/admin/setmeal")
@Slf4j
@Api(tags = "套餐管理接口")
public class SetmealController {

    @Autowired
    SetmealService setmealService;

    //分页查询接口开发
    /**
     * Path： /admin/setmeal/page
     *
     * Method： GET
     */
    @GetMapping("/page")
    @ApiOperation(value = "套餐查询")
    public Result<PageResult> page(SetmealPageQueryDTO setmealPageQueryDTO){
        log.info("分页查询：{}", setmealPageQueryDTO);
        PageResult pageResult = setmealService.pageQuery(setmealPageQueryDTO);
        return Result.success(pageResult);
    }

    //新增套餐的接口开发
    /**
     * Path： /admin/setmeal
     *
     * Method： POST
     */

    @ApiOperation(value = "新增套餐")
    @PostMapping
    @CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
    public Result save(@RequestBody SetmealDTO setmealDTO){
        log.info("新增套餐：{}", setmealDTO);
        setmealService.saveWithSetmealDishes(setmealDTO);
        return Result.success();
    }

    //修改套餐的接口开发
    /**
     * 第一步，查询回显
     * Path： /admin/setmeal/{id}
     *
     * Method： GET
     */
    @ApiOperation(value = "回显套餐")
    @GetMapping("/{id}")
    public Result<SetmealVO> getById(@PathVariable Long id){
        log.info("回显套餐：{}", id);
        SetmealVO setmealVO = setmealService.getSetmealById(id);
        return Result.success(setmealVO);
    }

    /**
     * 第二步，编辑修改
     * Path： /admin/setmeal
     *
     * Method： PUT
     */
    @ApiOperation(value = "修改套餐")
    @PutMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result updateWithSetmealDishes(@RequestBody SetmealDTO setmealDTO){
        log.info("修改套餐：{}", setmealDTO);
        setmealService.updateWithSetmealDishes(setmealDTO);
        return Result.success();
    }

    //删除套餐的接口开发
    /**
     * 批量删除套餐
     * Path： /admin/setmeal
     *
     * Method： DELETE
     */

    @ApiOperation(value = "批量删除套餐")
    @DeleteMapping
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result delete(@RequestParam List<Long> ids){
        log.info("批量删除套餐：{}", ids);
        setmealService.deleteBatch(ids);
        return Result.success();
    }

    //起售停售接口开发
    /**
     * Path： /admin/setmeal/status/{status}
     *
     * Method： POST
     */
    @ApiOperation(value = "起售停售")
    @PostMapping("/status/{status}")
    @CacheEvict(cacheNames = "setmealCache", allEntries = true)
    public Result startOrStop(@PathVariable Integer status, Long id){
        log.info("起售停售：{}", id);
        setmealService.startOrStop(status, id);
        return Result.success();
    }

}
