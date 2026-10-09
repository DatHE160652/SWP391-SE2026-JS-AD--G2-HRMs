package com.hrms.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Locale;

public class CharacterEncodingFilter implements Filter {
    private static final String ENCODING = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        request.setCharacterEncoding(ENCODING);
        response.setCharacterEncoding(ENCODING);
        applyStaticContentType(request, response);
        chain.doFilter(request, response);
    }

    private void applyStaticContentType(ServletRequest request, ServletResponse response) {
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().toLowerCase(Locale.ROOT);

        if (path.endsWith(".html")) {
            httpResponse.setContentType("text/html; charset=" + ENCODING);
        } else if (path.endsWith(".css")) {
            httpResponse.setContentType("text/css; charset=" + ENCODING);
        } else if (path.endsWith(".js")) {
            httpResponse.setContentType("application/javascript; charset=" + ENCODING);
        }
    }

    @Override
    public void destroy() {
    }
}
