package com.ms.filter;

import com.ms.entity.RestBean;
import com.ms.utils.ConstUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Order(ConstUtils.ORDER_LIMIT)
public class FlowLimitFilter extends HttpFilter {

    @Autowired
    StringRedisTemplate redisTemplate;

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String address = request.getRemoteAddr();
        if (this.tryCount(address)) {
            chain.doFilter(request, response);
        } else {
            this.writeBlockMessage(response);
        }
    }

    private void writeBlockMessage(HttpServletResponse response) throws IOException{
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=utf-8");
        response.getWriter().write(RestBean.forbidden("操作频繁，请稍后重试").asJsonString());
    }

    private boolean tryCount(String ip) {
        synchronized (ip.intern()) {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(ConstUtils.FLOW_LIMIT_BLOCK + ip)))
                return false;
            return this.limitPeriodCheck(ip);
        }
    }

    private boolean limitPeriodCheck(String ip) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(ConstUtils.FLOW_LIMIT_COUNTER + ip))) {
            long increment = Optional.ofNullable(redisTemplate.opsForValue().increment(ConstUtils.VERIFY_EMAIL_LIMIT + ip)).orElse(0L);
            if (increment > 10) {
                redisTemplate.opsForValue().set(ConstUtils.FLOW_LIMIT_BLOCK + ip, "", 30, TimeUnit.SECONDS);
                return false;
            } else {
                redisTemplate.opsForValue().set(ConstUtils.VERIFY_EMAIL_LIMIT + ip, "1", 3, TimeUnit.SECONDS);
            }
        }
        return true;
    }
}
