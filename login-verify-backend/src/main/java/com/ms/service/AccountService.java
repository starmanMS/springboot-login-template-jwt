package com.ms.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ms.entity.dto.AccountDto;
import com.ms.entity.vo.request.ConfirmRestVO;
import com.ms.entity.vo.request.EmailRegisterVO;
import com.ms.entity.vo.request.EmailResetVO;
import org.springframework.security.core.userdetails.UserDetailsService;

public interface AccountService extends IService<AccountDto>, UserDetailsService {

    public AccountDto findAccountByNameOrEmail(String text);
    public String registerEmailVerifyCode(String type, String email, String ip);
    public String registerEmailAccount(EmailRegisterVO vo);
    public String resetConfirm(ConfirmRestVO vo);
    public String resetEmailAccountPassword(EmailResetVO vo);
}
