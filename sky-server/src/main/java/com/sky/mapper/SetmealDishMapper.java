package com.sky.mapper;

import com.sky.annotation.AutoFill;
import com.sky.entity.SetmealDish;
import com.sky.enumeration.OperationType;
import io.swagger.annotations.ApiOperation;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Autowired;

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

    /**
     * @param setmealDishes
     * @author 批量保存套餐菜品信息
     * @create 2023-04-07 14:42
     */
    @AutoFill(value = OperationType.INSERT)
    void insertBatch(List<SetmealDish> setmealDishes);

    /**
     * @param setmealId
     * @author 根据套餐id删除套餐中的菜品信息
     * @create 2023-04-07 14:42
     */
    @Delete("delete from setmeal_dish where setmeal_id = #{setmealId}")
    void deleteBySetmealId(Long setmealId);

    /**
     * @param id
     * @author 根据套餐id查询套餐中的菜品信息
     * @create 2023-04-07 14:42
     */
    @Select("select * from setmeal_dish where setmeal_id = #{setmealId}")
    List<SetmealDish> getSetmealDishesBySetmealId(Long setmealId);
}
