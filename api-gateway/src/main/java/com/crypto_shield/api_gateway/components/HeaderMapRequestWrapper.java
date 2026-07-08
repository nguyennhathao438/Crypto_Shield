package com.crypto_shield.api_gateway.components;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

public class HeaderMapRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> customHeaders;

    public HeaderMapRequestWrapper(HttpServletRequest request, Map<String, String> customHeaders) {
        super(request);
        this.customHeaders = customHeaders;
    }

    @Override
    public String getHeader(String name) {
        String value = customHeaders.get(name);
        return value != null ? value : super.getHeader(name);
    }
    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.addAll(customHeaders.keySet());
        return Collections.enumeration(names);
    }
}