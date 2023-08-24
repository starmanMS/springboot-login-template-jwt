package com.ms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ms.entity.dto.AccountDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AccountMapper extends BaseMapper<AccountDto> {
    public AccountDto findAccountByNameOrEmail(String text);

}
