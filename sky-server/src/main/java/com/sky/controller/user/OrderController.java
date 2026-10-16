package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.vo.OrderVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController("userOrderController")
@RequestMapping("/user/order")
@Slf4j
@Api(tags = "用户订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping("/submit")
    @ApiOperation("用户提交订单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO){
        log.info("用户提交订单，参数为：{}", ordersSubmitDTO);
        OrderSubmitVO orderSubmitVO = orderService.submitOrder(ordersSubmitDTO);
        return Result.success(orderSubmitVO);
    }

    @PutMapping("/payment")
    @ApiOperation("订单支付")
    public Result payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) {
        log.info("模拟支付，订单号：{}", ordersPaymentDTO.getOrderNumber());
        // 注释掉原本微信预下单
        // String payJson = weChatPayUtil.pay(ordersPaymentDTO.getOrderNumber(), money);
        // 模拟支付成功，调用支付成功业务
        orderService.paySuccess(ordersPaymentDTO.getOrderNumber());
        return Result.success();
    }


    /**
     * 历史订单查询
     *
     * @param page
     * @param pageSize
     * @param status   订单状态 1待付款 2待接单 3已接单 4派送中 5已完成 6已取消
     * @return
     */
    @GetMapping("/historyOrders")
    @ApiOperation("历史订单查询")
    public Result<PageResult> page(int page, int pageSize, Integer status) {
        PageResult pageResult = orderService.pageQuery4User(page, pageSize, status);
        return Result.success(pageResult);
    }

    /**
     * 订单详情查询
     *
     * @param id
     * @return
     */
    @GetMapping("/orderDetail/{id}")
    @ApiOperation("订单详情查询")
    public Result<OrderVO> orderDetail(@PathVariable Long id) {
        log.info("订单详情查询，订单号：{}", id);
        OrderVO orderVO = orderService.details(id);
        return Result.success(orderVO);
    }

    /**
     * 用户取消订单
     *
     * @return
     */
    @PutMapping("/cancel/{id}")
    @ApiOperation("取消订单")
    public Result cancel(@PathVariable("id") Long id) throws Exception {
        orderService.userCancelById(id);
        return Result.success();
    }

    /**
     * 再来一单
     *
     * @param id
     * @return
     */
    @PostMapping("/repetition/{id}")
    @ApiOperation("再来一单")
    public Result repetition(@PathVariable Long id) {
        orderService.repetition(id);
        return Result.success();
    }
}
