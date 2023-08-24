package com.ms.controller;

import com.ms.entity.RestBean;
import com.ms.entity.vo.request.ConfirmRestVO;
import com.ms.entity.vo.request.EmailRegisterVO;
import com.ms.entity.vo.request.EmailResetVO;
import com.ms.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "登录校验相关", description = "包括用户登录、注册、验证码请求等操作。")
public class AuthorizeController {

    @Resource
    AccountService accountService;

    @GetMapping("/ask-code")
    public RestBean<Void> askVerifyCode(@RequestParam @Email String email,
                                        @RequestParam @Pattern(regexp = "(register|reset)") String type,
                                        HttpServletRequest request) {
       return this.messageHandle(() ->
               accountService.registerEmailVerifyCode(type, email, request.getRemoteAddr()));

    }

    @PostMapping("/register")
    @Operation(summary = "用户注册操作")
    public RestBean<Void> register(@RequestBody @Valid EmailRegisterVO vo) {
        return this.messageHandle(vo, accountService::registerEmailAccount);
    }


    @PostMapping("/reset-confirm")
    public RestBean<Void> resetConfirm(@RequestBody @Valid ConfirmRestVO vo) {
        return this.messageHandle(vo, accountService::resetConfirm);
    }

    @PostMapping("/reset-password")
    public RestBean<Void> resetConfirm(@RequestBody @Valid EmailResetVO vo) {
        return this.messageHandle(vo, accountService::resetEmailAccountPassword);
    }


    private <T> RestBean<Void> messageHandle(T vo, Function<T, String> function) {
        return messageHandle(() -> function.apply(vo));

    }

    private RestBean<Void> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);

    }

}
