package com.sky.controller.admin;


import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//菜品管理接口实现
@Api(tags = "菜品相关接口")
@RestController
@RequestMapping("/admin/dish")

@Slf4j
public class DishController {
    @Autowired
    private DishService dishService;
    //新增菜品
    @PostMapping
    @ApiOperation("新增菜品")
    public Result save(@RequestBody DishDTO dishDTO) {
        log.info("新增菜品：{}", dishDTO);
        dishService.saveWithFlavor(dishDTO);
        return Result.success();
    }

    //菜品分页查询接口
    @ApiOperation("菜品分页查询")
    @GetMapping("/page")
    public Result<PageResult> page(DishPageQueryDTO dishPageQueryDTO) {
        log.info("菜品分页查询：{}", dishPageQueryDTO);
        PageResult pageResult = dishService.pageQuery(dishPageQueryDTO);
        return Result.success(pageResult);
    }

    //删除菜品的接口
    @ApiOperation("菜品的批量删除")
    @DeleteMapping
    public Result delete(@RequestParam List<Long> ids) {
        log.info("删除菜品：{}", ids);
        dishService.deleteBatch(ids);
        return Result.success();
    }

    //修改菜品的接口

    //第一步是查询回显
    @GetMapping("/{id}")
    @ApiOperation("回显菜品")
    public Result<DishVO> getById(@PathVariable Long id) {
        log.info("回显菜品：{}", id);
        DishVO dishVO = dishService.getById(id);
        return Result.success(dishVO);
    }
    //第二步修改
    @PutMapping
    @ApiOperation("修改菜品")
    public Result update(@RequestBody DishDTO dishDTO) {
        log.info("修改菜品：{}", dishDTO);
        dishService.updateWithFlavor(dishDTO);
        return Result.success();
    }

    //菜品停售和起售功能的接口实现
    @PostMapping("/status/{status}")
    @ApiOperation("菜品停售和起售")
    public Result startOrStop(@PathVariable Integer status, @RequestParam Long id) {
        log.info("菜品停售和起售：状态：{}，id:{}",status, id);
        dishService.startOrStop(status, id);
        return Result.success();
    }

    // 根据菜品分类的id查询菜品
    /**
     * Path： /admin/dish/list
     *
     * Method： GET
     */

    @GetMapping("/list")
    @ApiOperation("根据菜品分类的id查询菜品")
    public Result<List<DishVO>> findDishByCategoryId(@RequestParam Long categoryId) {
        log.info("根据分类id查询菜品：{}", categoryId);
        return Result.success(dishService.findDishByCategoryId(categoryId));
    }
}
