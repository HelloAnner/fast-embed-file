package com.anner.embed.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping(value = { "/", "/tasks", "/tasks/**" })
    public String forward() {
        return "forward:/index.html";
    }
}