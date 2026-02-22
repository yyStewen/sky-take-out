package com.sky.controller.admin;


import com.sky.dto.OrdersCancelDTO;
import com.sky.dto.OrdersConfirmDTO;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersRejectionDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/order")
@Api(tags = "订单管理接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;
    /**
     * 订单搜索
     *  Path： /admin/order/conditionSearch
     *
     *  Method： GET
     * @param ordersPageQueryDTO
     * @return
     */
    @GetMapping("/conditionSearch")
    @ApiOperation("订单搜索")
    public Result<PageResult> conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO){
        log.info("订单搜索：{}",ordersPageQueryDTO);
        PageResult pageResult = orderService.conditionSearch(ordersPageQueryDTO);

        return Result.success(pageResult);
    }

    /**
     * 各个状态的订单数量统计
     *  Path： /admin/order/statistics
     *
     * Method： GET
     * @return
     */
    @GetMapping("/statistics")

    public Result<OrderStatisticsVO> statistics(){
        log.info("各个状态的订单数量统计");
        return Result.success(orderService.statistics());
    }

    /**
     * 根据订单id查询订单详情
     * Path： /admin/order/details/{id}
     *
     * Method： GET
     */
    @GetMapping("/details/{id}")
    @ApiOperation("根据订单id查询订单详情")
    public Result<OrderVO> show(@PathVariable Long id){
        log.info("查询订单详情，订单id为：{}",id);
        orderService.showOrderDetailById(id);
        return Result.success(orderService.showOrderDetailById(id));
    }
    /**
     * 拒单
     * Path： /admin/order/rejection
     *
     * Method： PUT
     */
    @PutMapping("/rejection")
    @ApiOperation("拒单")
    public Result rejection(@RequestBody OrdersRejectionDTO ordersRejectionDTO) throws Exception {
        log.info("拒单：{}",ordersRejectionDTO);
        orderService.rejection(ordersRejectionDTO);
        return Result.success();
    }

    /**
     * 接单
     * Path： /admin/order/confirm
     *
     * Method： PUT
     */
    @PutMapping("/confirm")
    @ApiOperation("接单")
    public Result confirm(@RequestBody OrdersConfirmDTO ordersConfirmDTO){
        log.info("接单：{}",ordersConfirmDTO);
        orderService.confirm(ordersConfirmDTO);
        return Result.success();
    }

    /**
     * 派送订单
     * Path： /admin/order/delivery/{id}
     *
     * Method： PUT
     */
    @PutMapping("/delivery/{id}")
    @ApiOperation("派送订单")
    public Result delivery(@PathVariable Long id){
        log.info("派送订单：{}",id);
        orderService.delivery(id);
        return Result.success();
    }

    /**
     * 完成订单
     * Path： /admin/order/complete/{id}
     *
     * Method： PUT
     */
    @PutMapping("/complete/{id}")
    @ApiOperation("完成订单")
    public Result complete(@PathVariable Long id){
        log.info("完成订单：{}",id);
        orderService.complete(id);
        return Result.success();
    }

    /**
     * 取消订单
     * Path： /admin/order/cancel
     * Method： PUT
     */
    @PutMapping("/cancel")
    @ApiOperation("取消订单")
    public Result cancel(@RequestBody OrdersCancelDTO ordersCancelDTO){
        log.info("取消订单：{}",ordersCancelDTO);
        orderService.cancelByAdmin(ordersCancelDTO);
        return Result.success();
    }

}
