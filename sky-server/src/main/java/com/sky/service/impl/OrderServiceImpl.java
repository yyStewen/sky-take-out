package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.*;
import com.sky.entity.*;
import com.sky.exception.AddressBookBusinessException;
import com.sky.exception.OrderBusinessException;
import com.sky.exception.ShoppingCartBusinessException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 订单
 */
@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private UserMapper userMapper;
    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {



        //异常情况的处理（收货地址为空、购物车为空）
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
        }

        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        //查询当前用户的购物车数据
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
        }

        // 【新增逻辑】：配送距离校验
        // 拼接完整地址：省+市+区+详细地址
        String address = addressBook.getProvinceName() + addressBook.getCityName()
                + addressBook.getDistrictName() + addressBook.getDetail();
        checkOutOfRange(address);

        //构造订单数据
        Orders order = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO,order);
        order.setPhone(addressBook.getPhone());
        order.setAddress(addressBook.getDetail());
        order.setConsignee(addressBook.getConsignee());
        order.setNumber(String.valueOf(System.currentTimeMillis()));
        order.setUserId(userId);
        order.setStatus(Orders.PENDING_PAYMENT);
        order.setPayStatus(Orders.UN_PAID);
        order.setOrderTime(LocalDateTime.now());

        //向订单表插入1条数据
        orderMapper.insert(order);


        //订单明细数据
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(order.getId());
            orderDetailList.add(orderDetail);
        }
        //向明细表插入n条数据
        orderDetailMapper.insertBatch(orderDetailList);

        //清理购物车中的数据
        shoppingCartMapper.deleteByUserId(userId);

        //封装返回结果
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(order.getId())
                .orderNumber(order.getNumber())
                .orderAmount(order.getAmount())
                .orderTime(order.getOrderTime())
                .build();
        return orderSubmitVO;
    }

    /**
     * 订单支付
     *
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 当前登录用户id
        Long userId = BaseContext.getCurrentId();
        User user = userMapper.getById(userId);

        //调用微信支付接口，生成预支付交易单
        //这里因为是个人开发，所以跳过。
//        JSONObject jsonObject = weChatPayUtil.pay(
//                ordersPaymentDTO.getOrderNumber(), //商户订单号
//                new BigDecimal(0.01), //支付金额，单位 元
//                "苍穹外卖订单", //商品描述
//                user.getOpenid() //微信用户的openid
//        );
        JSONObject jsonObject = new  JSONObject();

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        //直接调用支付成功接口修改订单状态
        paySuccess(ordersPaymentDTO.getOrderNumber());

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);

        // 根据订单id更新订单，修改订单状态
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
    }

    @Override
    public PageResult historyOrders(OrdersPageQueryDTO ordersPageQueryDTO) {

        //分页查询功能调用
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //执行查询，获取订单信息
        List<OrderVO> list = orderMapper.pageQuery(ordersPageQueryDTO);

        //执行查询，获取订单细节信息
        for (OrderVO orderVO : list) {
            Long orderId = orderVO.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orderId);
            orderVO.setOrderDetailList(orderDetailList);
        }


        //解析和封装结果
        Page<OrderVO> page = (Page<OrderVO>)list;
        return new PageResult(page.getTotal(), page.getResult());
    }


    @Override
    public OrderVO showOrderDetailById(Long id) {
        //第一步，查询回这个订单

        OrderVO orderVO = orderMapper.showOrderDetailById(id);

        //第二步，查询这个订单的细节
        Long orderId = orderVO.getId();
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orderId);
        orderVO.setOrderDetailList(orderDetailList);
        return orderVO;
    }


    //取消订单
    @Override
    public void cancel(Long id) {
        //第一步，查询回这个订单

        Orders ordersDB = orderMapper.showOrderDetailById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //第二步，建立一个新的订单数据然后用它修改订单状态
        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.CANCELLED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .cancelReason("用户取消")
                .build();

        orderMapper.update(orders);

        //第三步，调用退款接口。因为微信支付功能无法实现所以跳过。

    }

    @Override
    public void repetition(Long id) {
        //第一步，查询回这个订单。
        Orders ordersDB = orderMapper.showOrderDetailById(id);
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }
        //第二步，查询回这个订单的细节信息。
        List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(ordersDB.getId());
        if (orderDetailList == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //第三步，把该订单内所有商品添加到购物车
        for (OrderDetail orderDetail : orderDetailList) {
            ShoppingCart shoppingCart=new ShoppingCart();
            BeanUtils.copyProperties(orderDetail,shoppingCart);
            shoppingCart.setUserId(BaseContext.getCurrentId());
            shoppingCart.setCreateTime(LocalDateTime.now());
            shoppingCartMapper.insert(shoppingCart);
        }


    }

    @Override
    public PageResult conditionSearch(OrdersPageQueryDTO ordersPageQueryDTO) {
        //分页查询功能调用
        PageHelper.startPage(ordersPageQueryDTO.getPage(), ordersPageQueryDTO.getPageSize());

        //执行查询，获取订单信息
        List<OrderVO> list = orderMapper.pageQuery(ordersPageQueryDTO);

        //执行查询，获取订单细节信息
        for (OrderVO orderVO : list) {
            Long orderId = orderVO.getId();
            List<OrderDetail> orderDetailList = orderDetailMapper.listByOrderId(orderId);
            orderVO.setOrderDetailList(orderDetailList);
            //订单包含的菜品要以字符串展示
            orderVO.setOrderDishes(orderDetailList.stream().map(orderDetail ->
                    orderDetail.getName() + " " +
                    orderDetail.getDishFlavor() + "*" +
                            orderDetail.getNumber()).collect(Collectors.joining(",")));
        }


        //解析和封装结果
        Page<OrderVO> page = (Page<OrderVO>)list;
        return new PageResult(page.getTotal(), page.getResult());

    }

    @Override
    public OrderStatisticsVO statistics() {
        //统计三种订单状态：
        // 待接单，待派送，派送中
        Integer toBeConfirmed = orderMapper.countStatus(Orders.TO_BE_CONFIRMED);
        Integer confirmed = orderMapper.countStatus(Orders.CONFIRMED);
        Integer deliveryInProgress = orderMapper.countStatus(Orders.DELIVERY_IN_PROGRESS);
        OrderStatisticsVO orderStatisticsVO = new OrderStatisticsVO();
        orderStatisticsVO.setToBeConfirmed(toBeConfirmed);
        orderStatisticsVO.setConfirmed(confirmed);
        orderStatisticsVO.setDeliveryInProgress(deliveryInProgress);
        return orderStatisticsVO;
    }

    //这几个方法都是修改订单的状态。我们需要在mapper中定义一个修改订单状态的方法。

    /**
     *
     * TODO
     * 这里因为没有微信支付功能所以跳过退款的执行。。
     */
    @Override
    public void rejection(OrdersRejectionDTO ordersRejectionDTO) {
        //调用mapper，插入拒单的理由并修改订单状态
        Orders orders = Orders.builder()
                .id(ordersRejectionDTO.getId())
                .status(Orders.CANCELLED)
                .rejectionReason(ordersRejectionDTO.getRejectionReason())
                .cancelTime(LocalDateTime.now())
                .build();
        //这里应该调用退款逻辑

        orderMapper.update(orders);
    }

    @Override
    public void confirm(OrdersConfirmDTO ordersConfirmDTO) {
        //调用mapper，修改订单状态。订单状态改为派送中
        Orders orders = Orders.builder()
                .id(ordersConfirmDTO.getId())
                .status(Orders.CONFIRMED)
                .build();
        orderMapper.update(orders);

    }

    @Override
    public void delivery(Long id) {
        //调用mapper，修改订单状态。订单状态改为派送中
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.DELIVERY_IN_PROGRESS)
                .build();
        orderMapper.update(orders);

    }

    @Override
    public void complete(Long id) {
        //调用mapper，修改订单状态。订单状态改为已经完成。
        Orders orders = Orders.builder()
                .id(id)
                .status(Orders.COMPLETED)
                .deliveryTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);

    }

    @Override
    public void cancelByAdmin(OrdersCancelDTO ordersCancelDTO) {
        //调用mapper，修改订单状态。订单状态改为取消
        Orders orders = Orders.builder()
                .id(ordersCancelDTO.getId())
                .status(Orders.CANCELLED)
                .cancelReason(ordersCancelDTO.getCancelReason())
                .cancelTime(LocalDateTime.now())
                .build();
        orderMapper.update(orders);
    }

    //地址校验功能代码
    @org.springframework.beans.factory.annotation.Value("${sky.baidu.ak}")
    private String ak;

    @org.springframework.beans.factory.annotation.Value("${sky.shop.address}")
    private String shopAddress;

    /**
     * 检查收货地址是否在配送范围内
     */
    private void checkOutOfRange(String address) {
        Map<String, String> map = new HashMap<>();
        map.put("address", address);
        map.put("ak", ak);
        map.put("output", "json");

        // 1. 获取用户收货地址的经纬度
        String userCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject userJson = JSON.parseObject(userCoordinate);
        if (!userJson.getString("status").equals("0")) {
            throw new OrderBusinessException("收货地址解析失败");
        }
        // 提取经纬度 (lng,lat)
        JSONObject location = userJson.getJSONObject("result").getJSONObject("location");
        String lat = location.getString("lat");
        String lng = location.getString("lng");
        String userLocation = lat + "," + lng;

        // 2. 获取商家门店的经纬度（实际开发中可以缓存商家坐标，避免重复调用）
        map.put("address", shopAddress);
        String shopCoordinate = HttpClientUtil.doGet("https://api.map.baidu.com/geocoding/v3", map);
        JSONObject shopJson = JSON.parseObject(shopCoordinate);
        JSONObject shopLocation = shopJson.getJSONObject("result").getJSONObject("location");
        String shopLat = shopLocation.getString("lat");
        String shopLng = shopLocation.getString("lng");
        String originLocation = shopLat + "," + shopLng;

        // 3. 计算路线距离
        map.clear();
        map.put("origin", originLocation);
        map.put("destination", userLocation);
        map.put("ak", ak);
        // 使用轻量级路径规划接口
        String direction = HttpClientUtil.doGet("https://api.map.baidu.com/directionlite/v1/driving", map);
        JSONObject directionJson = JSON.parseObject(direction);
        if (!directionJson.getString("status").equals("0")) {
            throw new OrderBusinessException("配送路线规划失败");
        }

        // 解析距离（单位：米）
        Integer distance = directionJson.getJSONObject("result")
                .getJSONArray("routes")
                .getJSONObject(0)
                .getInteger("distance");

        if (distance > 5000) {
            throw new OrderBusinessException("超出配送范围（5公里内），无法下单");
        }
    }

}

