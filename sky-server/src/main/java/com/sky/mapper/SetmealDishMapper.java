package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author 根据菜品id查询对应的套餐id列表
 * @create 2023-04-07 14:42
 */
@Mapper
public interface SetmealDishMapper {
    /**
     * @author 根据菜品id查询对应的套餐id列表
     * @param dishIds
     * @create 2023-04-07 14:42
     */
    //select setmeal_id from setmeal_dish where dish_id in (dishIds)
    List<Long> getSetmealDishIdsByDishIds(List<Long> dishIds);
}
