package fr.mossaab.security.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class LogAllRequestsFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        log.debug("[REQUEST]: {} {}?{}", req.getMethod(), req.getRequestURI(), req.getQueryString());
        chain.doFilter(request, response);
    }
}
