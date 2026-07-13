package com.project.mentalhealth.interfaces.api.v1.auth;

import com.project.mentalhealth.interfaces.api.v1.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${app.api.base-path}/auth")
public class AuthController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("Auth service is up");
    }
}
