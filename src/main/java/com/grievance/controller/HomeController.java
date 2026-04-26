package com.grievance.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Forwards all non-API routes to index.html so the
     * single-page app handles its own client-side routing.
     */
    @GetMapping(value = {"/", "/login", "/register",
                         "/dashboard", "/grievances",
                         "/submit", "/pending", "/resolved"})
    public String home() {
        return "forward:/index.html";
    }
}