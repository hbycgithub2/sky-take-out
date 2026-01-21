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
import com.sky.utils.WeChatPayUtil;
import com.sky.utils.MockWeChatPayUtil;
import com.sky.websocket.OrderWebSocket;
import com.sky.websocket.AdminWebSocket;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderStatisticsVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
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
    private UserMapper userMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;
    @Autowired
    private MockWeChatPayUtil mockWeChatPayUtil;

    /**
     * 用户下单
     *
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
        
        // 如果前端没传预计送达时间，默认1小时后
        if (order.getEstimatedDeliveryTime() == null) {
            order.setEstimatedDeliveryTime(LocalDateTime.now().plusHours(1));
        }
        
        // ========== 后端计算订单金额 ==========
        // 如果前端没传金额或金额为null，从购物车计算
        if (order.getAmount() == null) {
            BigDecimal totalAmount = BigDecimal.ZERO;
            
            // 累加购物车中所有商品的金额
            for (ShoppingCart cart : shoppingCartList) {
                BigDecimal itemAmount = cart.getAmount().multiply(new BigDecimal(cart.getNumber()));
                totalAmount = totalAmount.add(itemAmount);
            }
            
            // 加上配送费（如果有）
            if (ordersSubmitDTO.getPackAmount() != null && ordersSubmitDTO.getPackAmount() > 0) {
                // 这里假设packAmount是配送费，根据实际业务调整
                // totalAmount = totalAmount.add(new BigDecimal(ordersSubmitDTO.getPackAmount()));
            }
            
            order.setAmount(totalAmount);
            log.info("【订单提交】后端计算订单金额：{}", totalAmount);
        }

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

        // 判断是否使用模拟支付（开发环境使用模拟支付）
        boolean useMockPayment = true; // 可以从配置文件读取
        
        JSONObject jsonObject;
        
        if (useMockPayment) {
            // ========== 使用模拟支付 ==========
            log.info("========== 【模拟支付模式】 ==========");
            log.info("【模拟支付】订单号：{}", ordersPaymentDTO.getOrderNumber());
            
            // 调用模拟支付工具类
            jsonObject = mockWeChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(),
                    new BigDecimal("0.01"),
                    "苍穹外卖订单",
                    user.getOpenid()
            );
            
            // 模拟3秒后自动支付成功（异步执行）
            String orderNumber = ordersPaymentDTO.getOrderNumber();
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("【模拟支付】等待3秒后自动触发支付成功...");
                    Thread.sleep(3000);
                    log.info("【模拟支付】3秒已到，触发支付成功回调");
                    paySuccess(orderNumber);
                } catch (Exception e) {
                    log.error("【模拟支付】自动回调失败", e);
                }
            });
            
        } else {
            // ========== 使用真实支付 ==========
            log.info("========== 【真实支付模式】 ==========");
            jsonObject = weChatPayUtil.pay(
                    ordersPaymentDTO.getOrderNumber(),
                    new BigDecimal(0.01),
                    "苍穹外卖订单",
                    user.getOpenid()
            );
        }

        if (jsonObject.getString("code") != null && jsonObject.getString("code").equals("ORDERPAID")) {
            throw new OrderBusinessException("该订单已支付");
        }

        OrderPaymentVO vo = jsonObject.toJavaObject(OrderPaymentVO.class);
        vo.setPackageStr(jsonObject.getString("package"));

        return vo;
    }

    /**
     * 支付成功，修改订单状态
     *
     * @param outTradeNo
     */
    public void paySuccess(String outTradeNo) {
        log.info("========== 【支付成功回调】开始处理 ==========");
        log.info("【支付成功】订单号：{}", outTradeNo);

        // 根据订单号查询订单
        Orders ordersDB = orderMapper.getByNumber(outTradeNo);
        
        if (ordersDB == null) {
            log.error("【支付成功】订单不存在，订单号：{}", outTradeNo);
            return;
        }
        
        // 检查订单状态，防止重复处理
        if (ordersDB.getPayStatus().equals(Orders.PAID)) {
            log.warn("【支付成功】订单已支付，忽略重复回调，订单号：{}", outTradeNo);
            return;
        }

        // 根据订单id更新订单的状态、支付方式、支付状态、结账时间
        Orders orders = Orders.builder()
                .id(ordersDB.getId())
                .status(Orders.TO_BE_CONFIRMED)
                .payStatus(Orders.PAID)
                .checkoutTime(LocalDateTime.now())
                .build();

        orderMapper.update(orders);
        log.info("【支付成功】订单状态已更新，订单ID：{}，状态：待接单", ordersDB.getId());
        
        // ========== WebSocket推送 ==========
        Long userId = ordersDB.getUserId();
        
        // 构造推送给用户的数据
        Map<String, Object> orderData = new HashMap<>();
        orderData.put("orderId", ordersDB.getId());
        orderData.put("orderNumber", outTradeNo);
        orderData.put("amount", ordersDB.getAmount());
        orderData.put("payTime", LocalDateTime.now());
        
        // 推送支付成功消息给用户
        OrderWebSocket.sendPaymentSuccess(userId, outTradeNo, orderData);
        log.info("【支付成功】WebSocket推送给用户完成，用户ID：{}", userId);
        
        // 构造推送给商家的数据
        Map<String, Object> merchantData = new HashMap<>();
        merchantData.put("orderId", ordersDB.getId());
        merchantData.put("orderNumber", outTradeNo);
        merchantData.put("amount", ordersDB.getAmount());
        merchantData.put("consignee", ordersDB.getConsignee());
        merchantData.put("phone", ordersDB.getPhone());
        merchantData.put("address", ordersDB.getAddress());
        merchantData.put("orderTime", ordersDB.getOrderTime());
        
        // 推送来单提醒给商家
        AdminWebSocket.sendNewOrder(merchantData);
        log.info("【支付成功】WebSocket推送来单提醒给商家完成");
        
        log.info("========== 【支付成功回调】处理完成 ==========");
    }

    /**
     * 查询订单详情
     *
     * @param id 订单ID
     * @return 订单详情
     */
    public OrderVO orderDetail(Long id) {
        // 根据id查询订单
        Orders orders = orderMapper.getById(id);
        
        // 查询该订单对应的菜品/套餐明细
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(orders.getId());

        // 将该订单及其详情封装到OrderVO并返回
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetailList);

        return orderVO;
    }

}
