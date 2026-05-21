package com.projectmgmt.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/{path:^(?!api|ws|actuator|swagger-ui|api-docs|v3|health).*$}",
            "/{path:^(?!api|ws|actuator|swagger-ui|api-docs|v3|health).*$}/**"
    })
    public ResponseEntity<?> forward() {
        Resource index = new ClassPathResource("static/index.html");
        if (!index.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(index);
    }
}