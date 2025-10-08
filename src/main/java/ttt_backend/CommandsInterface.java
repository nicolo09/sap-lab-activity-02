package ttt_backend;

import ttt_backend.entities.Game;
import ttt_backend.entities.Game.GameSymbolType;
import ttt_backend.entities.User;
import ttt_backend.exceptions.InvalidJoinException;
import ttt_backend.exceptions.InvalidMoveException;

public interface CommandsInterface {

    User registerUser(String username);

    Game createGame();

    void joinGame(String userId, String gameId, GameSymbolType symbol) throws InvalidJoinException;

    void makeMove(String userId, String gameId, int x, int y, Game.GameSymbolType symbol) throws InvalidMoveException;

}
