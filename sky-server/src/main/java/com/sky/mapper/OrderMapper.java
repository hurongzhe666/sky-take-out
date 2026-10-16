package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.dto.OrdersPageQueryDTO;
import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OrderMapper {
    /**
     * 插入订单数据
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号更新订单数据
     * @param orders
     */
    void updateByNumber(Orders orders);

    /**
     * 分页查询订单数据
     * @param ordersPageQueryDTO
     * @return
     */
    Page<Orders> pageQuery(OrdersPageQueryDTO ordersPageQueryDTO);

    /**
     * 根据id查询订单数据
     * @param id
     * @return
     */
    @Select("select * from orders where id = #{id}")
    Orders getOrderById(Long id);

    /**
     * 根据id更新订单数据
     * @param orders
     */

    void update(Orders orders);
}
