package com.localdeals.service;

import com.localdeals.dto.Result;
import com.localdeals.entity.ShopType;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopTypeService extends IService<ShopType> {
    public List<ShopType> queryTypeList();
}
