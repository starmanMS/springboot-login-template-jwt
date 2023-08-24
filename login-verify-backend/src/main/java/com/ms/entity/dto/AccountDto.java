package com.ms.entity.dto;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ms.entity.BaseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.Date;


@Data
@TableName("db_account")
@AllArgsConstructor
public class AccountDto implements BaseData {
    private Integer id;
    private String username;
    private String password;
    private String email;
    private String role;
    private Date registerTime;
}
