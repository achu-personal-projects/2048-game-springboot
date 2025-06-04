package com.example.game.service;

import com.example.game.model.Game;
import org.springframework.stereotype.Service;

@Service
public class GameService {
    private Game currentGame = new Game();

    public Game getGame() {
        return currentGame;
    }

    public void resetGame() {
        currentGame.reset();
    }

    public boolean makeMove(String direction) {
        return currentGame.move(direction);
    }
}
