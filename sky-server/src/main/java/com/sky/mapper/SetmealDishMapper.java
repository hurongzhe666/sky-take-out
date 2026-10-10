package com.sky.mapper;

import com.sky.entity.SetmealDish;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SetmealDishMapper {
    /**
     * @param dishIds
     * @author 根据菜品id查询对应的套餐id列表
     * @create 2023-04-07 14:42
     */
    @ApiOperation("根据菜品id查询对应的套餐id列表")
    List<Long> getSetmealDishIdsByDishIds(List<Long> dishIds);


    void insertBatch(List<SetmealDish> setmealDishes);

}
