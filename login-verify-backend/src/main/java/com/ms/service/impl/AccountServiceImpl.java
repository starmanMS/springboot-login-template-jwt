package com.ms.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ms.entity.dto.AccountDto;
import com.ms.entity.vo.request.ConfirmRestVO;
import com.ms.entity.vo.request.EmailRegisterVO;
import com.ms.entity.vo.request.EmailResetVO;
import com.ms.mapper.AccountMapper;
import com.ms.service.AccountService;
import com.ms.utils.ConstUtils;
import com.ms.utils.FlowUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, AccountDto> implements AccountService {


    @Autowired
    AmqpTemplate amqpTemplate;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Autowired
    FlowUtils flowUtils;

    @Autowired
    PasswordEncoder encoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AccountDto account = this.findAccountByNameOrEmail(username);
        if (account == null) {
            throw new UsernameNotFoundException("用户名或密码错误!");
        }
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    public AccountDto findAccountByNameOrEmail(String text) {
              return this.query()
                      .eq("username", text).or()
                      .eq("email", text)
                      .one();
    }

    @Override
    public String registerEmailVerifyCode(String type, String email, String ip) {

        synchronized (ip.intern()) {
            if (!this.verifyLimit(ip))
                return "请求频繁，请稍后重试！";
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type", type, "email", email, "code", code);
            amqpTemplate.convertAndSend("mail", data);
            redisTemplate.opsForValue()
                    .set(ConstUtils.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES);

            return null;
        }


    }

    public boolean verifyLimit(String ip) {
        String key = ConstUtils.VERIFY_EMAIL_LIMIT  + ip;
        return flowUtils.limitOnceCheck(key, 60);
    }

    @Override
    public String registerEmailAccount(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String username = vo.getUsername();
        String key = ConstUtils.VERIFY_EMAIL_DATA + email;
        String code = redisTemplate.opsForValue().get(key);
        if (code == null) return "请先获取验证码！";
        if (!code.equals(vo.getCode())) return "验证码输入错误，请重新输入！";
        if (this.existsAccountByEmail(email)) return "此邮箱已被其他用户注册！";
        if (this.existsAccountByUsername(username)) return "此用户名已被其他用户注册！";
        String password = encoder.encode(vo.getPassword());
        AccountDto account = new AccountDto(null, username, password, email, "user", new Date());
        if (this.save(account)) {
            redisTemplate.delete(key);
            return null;
        } else {
            return "内部服务器错误，请联系管理员！";
        }
    }

    private boolean existsAccountByEmail(String email) {
        return this.baseMapper.exists(Wrappers.<AccountDto>query().eq("email", email));
    }

    private boolean existsAccountByUsername(String username) {
        return this.baseMapper.exists(Wrappers.<AccountDto>query().eq("username", username));
    }

    @Override
    public String resetConfirm(ConfirmRestVO vo) {
        String email = vo.getEmail();
        String code = redisTemplate.opsForValue().get(ConstUtils.VERIFY_EMAIL_DATA + email);
        if (code == null) return "请先输入验证码！";
        if (!code.equals(vo.getCode())) return "验证码错误吗，请重新输入！";
        return null;
    }

    @Override
    public String resetEmailAccountPassword(EmailResetVO vo) {
        String email = vo.getEmail();
        String verify = this.resetConfirm(new ConfirmRestVO(email, vo.getCode()));
        if (verify != null) return verify;
        String password = encoder.encode(vo.getPassword());
        boolean update = this.update().eq("email", email).set("password", password).update();
        if (update) {
            redisTemplate.delete(ConstUtils.VERIFY_EMAIL_DATA + email);
        }
        return null;
    }
}
