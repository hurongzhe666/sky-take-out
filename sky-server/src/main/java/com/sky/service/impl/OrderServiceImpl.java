package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderDetailMapper orderDetailMapper;

    @Autowired
    private AddressBookMapper addressBookMapper;

    @Autowired
    private ShoppingCartMapper shoppingCartMapper;

    /**
     * 用户提交订单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {

        //处理各种业务异常（地址薄为空;购物车数据为空）
        AddressBook addressBook= addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null)
            //抛出业务异常
            throw new RuntimeException(MessageConstant.ADDRESS_BOOK_IS_NULL);

        //查询当前用户的购物车数据

        ShoppingCart shoppingCart = new ShoppingCart();
        Long userId= BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);
        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);

        if(shoppingCartList == null || shoppingCartList.size() == 0)
            //抛出业务异常
            throw new RuntimeException(MessageConstant.SHOPPING_CART_IS_NULL);



        //2.向订单表中插入一条数据
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);
        orders.setOrderTime(LocalDateTime.now());
        //支付状态未支付
        orders.setPayStatus(Orders.UN_PAID);
        //订单状态待支付
        orders.setStatus(orders.PENDING_PAYMENT);
        //订单号唯一，使用时间戳
        orders.setNumber(String.valueOf(System.currentTimeMillis()));
        orders.setPhone(addressBook.getPhone());
        //收货人
        orders.setConsignee(addressBook.getConsignee());
        orders.setUserId(userId);

        orderMapper.insert(orders);


        List<OrderDetail> orderDetailList = new ArrayList<>();
        //3.向订单明细表中插入N条数据
        for (ShoppingCart cart : shoppingCartList)
        {
            OrderDetail orderDetail = new OrderDetail();//订单明细
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());//设置当前订单明细关联的订单ID
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        //4.用户下单成功后清空购物车数据
        shoppingCartMapper.deleteByUserId(userId);

        //5.封装一下VO然后返回
        OrderSubmitVO orderSubmitVO = OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();

        return orderSubmitVO;
    }

    @Transactional
    public void paySuccess(String orderNumber) {
        log.info("支付成功，订单号：{}",orderNumber);
        Orders orders = new Orders();
        orders.setNumber(orderNumber);
        orders.setStatus(Orders.PAID); // 常量 PAID = 2
        orders.setCheckoutTime(LocalDateTime.now());
        orderMapper.updateByNumber(orders);
    }

}
