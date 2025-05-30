package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishItemVO;
import com.sky.vo.SetmealVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SetmealMapper {

    /**
     * 根据分类id查询套餐的数量
     * @param id
     * @return
     */
    @Select("select count(id) from setmeal where category_id = #{categoryId}")
    Integer countByCategoryId(Long id);
   //@Insert("insert into setmeal (name,code,category_id,price,status,image,description,create_time,update_time,create_user,update_user) " +
   //        "values (#{name},#{code},#{categoryId},#{price},#{status},#{image},#{description},#{createTime},#{updateTime},#{createUser},#{updateUser)")
    @AutoFill(OperationType.INSERT)
   void insert(Setmeal setmeal);

    Page<SetmealVO> pageQuery(SetmealPageQueryDTO setmealPageQueryDTO);

    SetmealVO getById(Long id);
    @AutoFill(OperationType.UPDATE)
    void updateById(Setmeal setmeal);
    /**
     * 动态条件查询套餐
     * @param setmeal
     * @return
     */
    List<Setmeal> list(Setmeal setmeal);

    /**
     * 根据套餐id查询菜品选项
     * @param setmealId
     * @return
     */
    @Select("select sd.name, sd.copies, d.image, d.description " +
            "from setmeal_dish sd left join dish d on sd.dish_id = d.id " +
            "where sd.setmeal_id = #{setmealId}")
    List<DishItemVO> getDishItemBySetmealId(Long setmealId);
   @Select("select * from setmeal where id = #{setmealId} ")
    Setmeal getSetmealById(Long setmealId);
}
