package com.example.game.controller;

import com.example.game.model.Game;
import com.example.game.service.GameService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class GameController {
    @Autowired
    private GameService gameService;

    @GetMapping("/")
    public String showGame() {
        return "redirect:/game";
    }

    @GetMapping("/game")
    public String getGameTemplate() {
        return "game";
    }

    @GetMapping("/api/game")
    public Game getGameState() {
        return gameService.getGame();
    }

    @PostMapping("/api/game/move")
    public Map<String, Object> makeMove(@RequestParam String direction) {
        Map<String, Object> response = new HashMap<>();
        boolean moved = gameService.makeMove(direction);
        response.put("moved", moved);
        response.put("game", gameService.getGame());
        return response;
    }

    @PostMapping("/api/game/reset")
    public Game resetGame() {
        gameService.resetGame();
        return gameService.getGame();
    }
}
