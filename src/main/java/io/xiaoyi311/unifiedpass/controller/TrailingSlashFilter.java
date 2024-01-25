package io.xiaoyi311.unifiedpass.controller;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 自动去除路径最后 /
 * @author xiaoyi311
 */
@WebFilter
public class TrailingSlashFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI();
        if (path.endsWith("/")) {
            httpResponse.sendRedirect(path.substring(0, path.length() - 1));
            return;
        }
        chain.doFilter(request, response);
    }
}
