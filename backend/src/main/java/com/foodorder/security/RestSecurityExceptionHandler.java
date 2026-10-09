package com.foodorder.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class RestSecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private final ObjectMapper objectMapper;

    public RestSecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "Authentication is required or has expired.", request);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "You do not have permission to perform this action.", request);
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message,
                            HttpServletRequest request) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(),
                new SecurityError(Instant.now(), status.value(), message, request.getRequestURI(), Map.of()));
    }

    private record SecurityError(Instant timestamp, int status, String message, String path,
                                 Map<String, String> fieldErrors) {}
}
