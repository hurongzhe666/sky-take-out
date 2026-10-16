package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.AddressBook;
import com.sky.entity.OrderDetail;
import com.sky.entity.Orders;
import com.sky.entity.ShoppingCart;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.AddressBookMapper;
import com.sky.mapper.OrderDetailMapper;
import com.sky.mapper.OrderMapper;
import com.sky.mapper.ShoppingCartMapper;
import com.sky.result.PageResult;
import com.sky.service.OrderService;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.stream.Collectors;

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

    /**
     * 用户端订单分页查询
     *
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    //需要查询到每一条前端需要的订单信息以及每一条订单的所有订单明细信息
    public PageResult pageQuery4User(int pageNum, int pageSize, Integer status) {
        // 设置分页
        PageHelper.startPage(pageNum, pageSize);

        // 构建查询条件
        OrdersPageQueryDTO ordersPageQueryDTO = new OrdersPageQueryDTO();
        ordersPageQueryDTO.setUserId(BaseContext.getCurrentId());
        // 设置查询状态,可以不设置
        ordersPageQueryDTO.setStatus(status);

        // 分页条件查询
        Page<Orders> page = orderMapper.pageQuery(ordersPageQueryDTO);

        //用来存传到前端的订单数据
        List<OrderVO> list = new ArrayList();

        // 查询出订单明细，并封装入OrderVO进行响应
        if (page != null && page.getTotal() > 0) {
            //把page里每条订单数据
            for (Orders orders : page) {
                Long orderId = orders.getId();// 订单id

                // 根据订单id查询订单明细
                List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(orderId);

                //用来封装每条订单数据，存入list
                OrderVO orderVO = new OrderVO();
                //把每一条订单数据复制到OrderVO
                BeanUtils.copyProperties(orders, orderVO);
                //把每一条订单明细数据设置到OrderVO
                orderVO.setOrderDetailList(orderDetails);

                list.add(orderVO);
            }
        }
        return new PageResult(page.getTotal(), list);
    }

    /**
     * 根据id查询订单详情
     *
     * @param id
     * @return
     */

    public OrderVO details(Long id) {
        //先通过订单id将所需订单的数据查出来
        Orders orders = orderMapper.getOrderById(id);

        //再通过订单id将关联的订单明细数据查出来
        List<OrderDetail> orderDetails = orderDetailMapper.getByOrderId(id);

        //创建一个orderVO对象来封装订单数据
        OrderVO orderVO = new OrderVO();
        BeanUtils.copyProperties(orders, orderVO);
        orderVO.setOrderDetailList(orderDetails);
        return orderVO;
    }

    /**
     * 用户取消订单
     *
     * @param id
     */
    public void userCancelById(Long id) throws Exception {

        /*- 待支付和待接单状态下，用户可直接取消订单
                - 商家已接单状态下，用户取消订单需电话沟通商家
                - 派送中状态下，用户取消订单需电话沟通商家
                - 如果在待接单状态下取消订单，需要给用户退款
                - 取消订单后需要将订单状态修改为“已取消”
        */


        // 根据id查询订单
        Orders ordersDB = orderMapper.getOrderById(id);

        // 校验订单是否存在
        if (ordersDB == null) {
            throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
        }

        //订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
        if (ordersDB.getStatus() > 2) {
            throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
        }

        Orders orders = new Orders();
        orders.setId(ordersDB.getId());

        //订单处于待接单状态下取消，需要进行退款
        if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
            /*调用微信支付退款接口
            weChatPayUtil.refund(
                    ordersDB.getNumber(), //商户订单号
                    ordersDB.getNumber(), //商户退款单号
                    new BigDecimal(0.01),//退款金额，单位 元
                    new BigDecimal(0.01));//原订单金额
             */

            //支付状态修改为 退款
            orders.setPayStatus(Orders.REFUND);
        }

        // 更新订单状态、取消原因、取消时间
        orders.setStatus(Orders.CANCELLED);
        orders.setCancelReason("用户取消");
        orders.setCancelTime(LocalDateTime.now());
        orderMapper.update(orders);
    }

    /**
     * 再来一单
     *
     * @param id
     */
    public void repetition(Long id) {
        // 查询当前用户id
        Long userId = BaseContext.getCurrentId();

        // 根据订单id查询当前订单详情
        List<OrderDetail> orderDetailList = orderDetailMapper.getByOrderId(id);

        // 将订单详情对象转换为购物车对象
        List<ShoppingCart> shoppingCartList = orderDetailList.stream().map(x -> {
            ShoppingCart shoppingCart = new ShoppingCart();

            // 将原订单详情里面的菜品信息重新复制到购物车对象中
            BeanUtils.copyProperties(x, shoppingCart, "id");
            shoppingCart.setUserId(userId);
            shoppingCart.setCreateTime(LocalDateTime.now());

            return shoppingCart;
        }).collect(Collectors.toList());

        // 将购物车对象批量添加到数据库
        shoppingCartMapper.insertBatch(shoppingCartList);
    }
}
