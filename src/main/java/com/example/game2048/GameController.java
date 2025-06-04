package com.example.game2048;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GameController {
    @GetMapping("/")
    public String game() {
        return "forward:/index.html";
    }
}
