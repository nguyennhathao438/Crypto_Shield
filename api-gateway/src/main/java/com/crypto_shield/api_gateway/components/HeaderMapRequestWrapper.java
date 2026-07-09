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
    public Enumeration<String> getHeaders(String name) {
        if (customHeaders.containsKey(name)) {
            return Collections.enumeration(List.of(customHeaders.get(name)));
        }
        return super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        // Tránh trùng tên nếu request gốc vô tình đã có header cùng tên
        names.removeIf(customHeaders::containsKey);
        names.addAll(customHeaders.keySet());
        return Collections.enumeration(names);
    }
}