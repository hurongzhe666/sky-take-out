package com.sky.service;

import com.sky.entity.AddressBook;
import java.util.List;

public interface AddressBookService {

    /**
     * 根据条件查询所有地址簿
     * @param addressBook
     * @return
     */
    List<AddressBook> list(AddressBook addressBook);

    /**
     * 保存地址簿
     * @param addressBook
     */
    void save(AddressBook addressBook);

    /**
     * 根据id查询地址簿
     * @param id
     * @return
     */
    AddressBook getById(Long id);
    /**
     * 更新地址簿
     * @param addressBook
     */

    void update(AddressBook addressBook);

    /**
     * 设置默认地址簿
     * @param addressBook
     */
    void setDefault(AddressBook addressBook);
    /**
     * 根据id删除地址簿
     * @param id
     */
    void deleteById(Long id);

}
