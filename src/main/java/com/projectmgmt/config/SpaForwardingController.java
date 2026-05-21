package com.projectmgmt.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaForwardingController {

    @GetMapping(value = {
            "/{path:^(?!api|ws|actuator|swagger-ui|api-docs|v3|health).*$}",
            "/{path:^(?!api|ws|actuator|swagger-ui|api-docs|v3|health).*$}/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}