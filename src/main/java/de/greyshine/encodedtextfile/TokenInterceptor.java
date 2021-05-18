package de.greyshine.encodedtextfile;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Slf4j
public class TokenInterceptor extends HandlerInterceptorAdapter {

    private final boolean skipInterceptorHandling = true;
    @Autowired
    private Controller controller;

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws Exception {

        log.info("{}:{}, handler: {}, {}", request.getMethod(), request.getRequestURI(), handler, handler.getClass());

        if (skipInterceptorHandling || !(handler instanceof HandlerMethod)) {
            return super.preHandle(request, response, handler);
        }

        final HandlerMethod handlerMethod = (HandlerMethod) handler;

        final String token = request.getHeader("TKN");
        final String currentToken = controller.getPwd();

        if (currentToken != null && !currentToken.equals(token)) {
            throw new HttpClientErrorException(HttpStatus.FORBIDDEN);
        }

        return true;
    }


}
