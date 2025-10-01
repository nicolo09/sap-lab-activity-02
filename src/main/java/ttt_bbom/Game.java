package ttt_bbom;

import java.util.HashMap;
import java.util.Optional;

/**
 * 
 * A class representing a running game
 * 
 */
public class Game {

	public record Player(User user, GridSymbol symbol) {}

	private String id;
	public enum GridSymbol { CROSS, CIRCLE, EMPTY};
	public enum GameState { WAITING_PLAYERS, PLAYING, FINISHED }
	
	private GridSymbol[][] grid;
	private GridSymbol currentTurn;
	private HashMap<GridSymbol, Player> players;
	private GameState state;
	private Optional<Player> winner;
	
	public Game(String id) {
		this.id = id;
		grid = new GridSymbol[3][3];
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				grid[y][x] = GridSymbol.EMPTY;
			}
		}
		currentTurn = GridSymbol.CROSS;
		players = new HashMap<>();
		state = GameState.WAITING_PLAYERS;
		winner = Optional.empty();
	}	

	public String getId() {
		return id;
	}
	
	public void joinGame(User user, GridSymbol symbol) throws InvalidJoinException {
		if (!state.equals(GameState.WAITING_PLAYERS) || players.containsKey(symbol)) {
			throw new InvalidJoinException();
		}
		players.put(symbol, new Player(user, symbol));
		if (players.size() == 2) {
			state = GameState.PLAYING;
		}
	}
	
	public void makeAmove(User player, GridSymbol symbol, int x, int y) throws InvalidMoveException {
		if (symbol.equals(currentTurn)) {
			var p = players.get(symbol);
			if (p.user().equals(player)) {
				if (grid[y][x].equals(GridSymbol.EMPTY)) {
					grid[y][x] = symbol;
					currentTurn = adversarial(symbol);
					checkState();			
				} else {
					throw new InvalidMoveException();
				}
			} else {
				throw new InvalidMoveException();				
			}
		} else {
			throw new InvalidMoveException();			
		}
	}

	public boolean isGameEnd() {
		return state.equals(GameState.FINISHED);
	}
	
	public Optional<GridSymbol> getWinner() {
		if (winner.isPresent()) {
			return Optional.of(winner.get().symbol());
		} else {
			return Optional.empty();
		}
	}

	public boolean isTie() {
		return isGameEnd() && winner.isEmpty();
	}
	
	private void checkState() {
		for (int y = 0; y < 3; y++) {
			if (!grid[y][0].equals(GridSymbol.EMPTY) && 
				grid[y][0].equals(grid[y][1]) && 
				grid[y][1].equals(grid[y][2])){
					winner = Optional.of(players.get(grid[y][0]));
					state = GameState.FINISHED;
					return;
			}
		}
		for (int x = 0; x < 3; x++) {
			if (!grid[0][x].equals(GridSymbol.EMPTY) && 	
				grid[0][x].equals(grid[1][x]) && 
				grid[1][x].equals(grid[2][x])){
					winner = Optional.of(players.get(grid[0][x]));
					state = GameState.FINISHED;
					return;
			}
		}
		if (!grid[0][0].equals(GridSymbol.EMPTY) && grid[0][0].equals(grid[1][1]) && grid[1][1].equals(grid[2][2])){
			winner = Optional.of(players.get(grid[0][0]));
			state = GameState.FINISHED;
			return;
		}
		if (!grid[2][0].equals(GridSymbol.EMPTY) && grid[2][0].equals(grid[1][1]) && grid[1][1].equals(grid[0][2])){
			winner = Optional.of(players.get(grid[2][0]));
			state = GameState.FINISHED;
			return;
		}
	}
	
	private GridSymbol adversarial(GridSymbol sym) {
		return sym.equals(GridSymbol.CIRCLE) ? GridSymbol.CROSS : GridSymbol.CIRCLE;
	}
}
