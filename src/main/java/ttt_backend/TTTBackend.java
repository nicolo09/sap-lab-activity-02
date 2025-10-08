package ttt_backend;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.*;
import io.vertx.ext.web.*;
import io.vertx.ext.web.handler.StaticHandler;
import ttt_backend.entities.Game;
import ttt_backend.entities.User;

import java.io.*;

/**
*
* Big raw monolithic back end with no clean (software) architecture
* 
* It features a reactive/event-loop control architecture, based on Vert.x 
* 
* @author aricci
*
*/
public class TTTBackend extends VerticleBase {

	static Logger logger = Logger.getLogger("[TicTacToe Backend]");
	
	//static final String TTT_CHANNEL = "ttt-events";

	/* list of registered users */
	private HashMap<String, User> users;
	
	/* list on ongoing games*/ 
	private HashMap<String, Game> games;

	/* counters to create ids */
	private int usersIdCount;
	private int gamesIdCount;

	/* db file */
	static final String DB_USERS = "users.json";
	
	/* port of the endpoint */
	private int port;
	
	
	public TTTBackend(int port) {
		this.port = port;
		logger.setLevel(Level.INFO);
	}

	public Future<?> start() {
		logger.log(Level.INFO, "TTT Server initializing...");
		HttpServer server = vertx.createHttpServer();

		gamesIdCount = 0;
		usersIdCount = 0;
		
		users = new HashMap<>();
		games = new HashMap<>();
		
		/* configuring API routes */
		
		Router router = Router.router(vertx);
		router.route(HttpMethod.POST, "/api/registerUser").handler(this::registerUser);
		router.route(HttpMethod.POST, "/api/createGame").handler(this::createNewGame);
		router.route(HttpMethod.POST, "/api/joinGame").handler(this::joinGame);
		router.route(HttpMethod.POST, "/api/makeAMove").handler(this::makeAMove);
		
		/* configuring websocket handler */
		
		handleEventSubscription(server, "/api/events");

		/* enabling access to static files (web app page) */
		
		router.route("/public/*").handler(StaticHandler.create());

		/* restore from the DB */
		
		initFromDB();
				
		/* start the server */
		
		var fut = server
			.requestHandler(router)
			.listen(port);
		
		fut.onSuccess(res -> {
			logger.log(Level.INFO, "TTT Game Server ready - port: " + port);
		});

		return fut;
	}
	

	/* List of handlers mapping the API */
	
	/**
	 * 
	 * Register a new user
	 * 
	 * @param context
	 */
	protected void registerUser(RoutingContext context) {
		logger.log(Level.INFO, "RegisterUser request");
		usersIdCount++;		
		context.request().handler(buf -> {

			/* add the new user */
			JsonObject userInfo = buf.toJsonObject();
			var userName = userInfo.getString("userName");
			var newUserId = "user-"+usersIdCount;
			var user = new User(newUserId, userName);
			users.put(newUserId, user);
			
			/* save on DB */
			saveOnDB();
			
			var reply = new JsonObject();
			reply.put("userId", newUserId);
			reply.put("userName", userName);		
			try {
				sendReply(context.response(), reply);
			} catch (Exception ex) {
				sendError(context.response());
			}
		});
	}
	
	/**
	 * 
	 * Create a New Game
	 * 
	 * @param context
	 */
	protected void createNewGame(RoutingContext context) {
		logger.log(Level.INFO, "CreateNewGame request - " + context.currentRoute().getPath());
		gamesIdCount++;		
		var newGameId = "game-"+gamesIdCount;		
		var game = new Game(newGameId);
		games.put(newGameId, game);		
		var reply = new JsonObject();
		reply.put("gameId", newGameId);
		try {
			sendReply(context.response(), reply);
		} catch (Exception ex) {
			sendError(context.response());
		}
	}

	/**
	 * 
	 * Join a Game
	 * 
	 * @param context
	 */
	protected void joinGame(RoutingContext context) {
		logger.log(Level.INFO, "JoinGame request - " + context.currentRoute().getPath());
		context.request().handler(buf -> {
			JsonObject joinInfo = buf.toJsonObject();			
			String userId = joinInfo.getString("userId");
			String gameId = joinInfo.getString("gameId");
			String symbol = joinInfo.getString("symbol");
			var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;		
			var user = users.get(userId);
			var game = games.get(gameId);
			
			var reply = new JsonObject();
			try {
				game.joinGame(user, gameSym);			
				reply.put("result", "accepted");
				try {
					sendReply(context.response(), reply);
					logger.log(Level.INFO, "Join succeeded");
				} catch (Exception ex) {
					sendError(context.response());
				}	
								
			} catch (Exception ex) {
				reply.put("result", "denied");
				try {
					sendReply(context.response(), reply);
					logger.log(Level.INFO, "Join failed");
				} catch (Exception ex2) {
					sendError(context.response());
				}				
			}
		});
	}
	
	/**
	 * 
	 * Make a move in a game
	 * 
	 * @param context
	 */
	protected void makeAMove(RoutingContext context) {
		logger.log(Level.INFO, "MakeAMove request - " + context.currentRoute().getPath());
		context.request().handler(buf -> {
			var  reply = new JsonObject();
			try {
				JsonObject moveInfo = buf.toJsonObject();
				logger.log(Level.INFO, "move info: " + moveInfo);
				
				String userId = moveInfo.getString("userId");
				String gameId = moveInfo.getString("gameId");
				String symbol = moveInfo.getString("symbol");
				int x = Integer.parseInt(moveInfo.getString("x"));
				int y = Integer.parseInt(moveInfo.getString("y"));
		
				var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;		
				var user = users.get(userId);
				var game = games.get(gameId);				

				game.makeAmove(user, gameSym, x, y);						
				reply.put("result", "accepted");
				try {
					sendReply(context.response(), reply);
				} catch (Exception ex) {
					sendError(context.response());
				}	

				/* notifying events */
				
				var eb = vertx.eventBus();
				
				/* about the new move */
				
				var evMove = new JsonObject();
				evMove.put("event", "new-move");
				evMove.put("x", x);
				evMove.put("y", y);
				evMove.put("symbol", symbol);		
				
				/* the event is notified on the event bus 'address' of the specific game */
				
				var gameAddress = getBusAddressForAGame(gameId);
				eb.publish(gameAddress, evMove);
	
				/* a game-ended event is notified too if the game is ended */

				if (game.isGameEnd()) {

					var evEnd = new JsonObject();
					evEnd.put("event", "game-ended");
					
					if (game.isTie()) {
						evEnd.put("result", "tie");					
					} else {
						var sym = game.getWinner().get();
						if (sym.equals(Game.GameSymbolType.CROSS)) {
							evEnd.put("winner", "cross");											
						} else {
							evEnd.put("winner", "circle");											
						}
					}				
					eb.publish(gameAddress, evEnd);
				}
				
			} catch (Exception ex) {
				reply.put("result", "invalid-move");
				try {
					sendReply(context.response(), reply);
				} catch (Exception ex2) {
					sendError(context.response());
				}				
			}
		});
	}


	/* 
	 * 
	 * Handling frontend subscriptions to receive events 
	 * when joining a game, using websockets
	 * 
	 */
	protected void handleEventSubscription(HttpServer server, String path) {
		server.webSocketHandler(webSocket -> {
			logger.log(Level.INFO, "New TTT subscription accepted.");

			/* 
			 * 
			 * Receiving a first message including the id of the game
			 * to observe 
			 * 
			 */
			webSocket.textMessageHandler(openMsg -> {
				logger.log(Level.INFO, "For game: " + openMsg);
				JsonObject obj = new JsonObject(openMsg);
				String gameId = obj.getString("gameId");
								
				/* 
				 * Subscribing events on the event bus to receive
				 * events concerning the game, to be notified 
				 * to the frontend using the websocket
				 * 
				 */
				EventBus eb = vertx.eventBus();
				
				var gameAddress = getBusAddressForAGame(gameId);				
				eb.consumer(gameAddress, msg -> {
					JsonObject ev = (JsonObject) msg.body();
					logger.log(Level.INFO, "Notifying event to the frontend: " + ev.encodePrettily());
					webSocket.writeTextMessage(ev.encodePrettily());
				});
				
				/*
				 * 
				 * When both players joined the game and both
				 * have the websocket connection ready, 
				 * the game can start 
				 * 
				 */
				var game = games.get(gameId);
				if (game.bothPlayersJoined()) {
					try {
						game.start();					
						var evGameStarted = new JsonObject();
						evGameStarted.put("event", "game-started");
						eb.publish(gameAddress, evGameStarted);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
				
				
			});
		});
	}
	
	/* DB management */
	
	private void initFromDB() {
		try {
			var usersDB = new BufferedReader(new FileReader(DB_USERS));
			var sb = new StringBuffer();
			while (usersDB.ready()) {
				sb.append(usersDB.readLine()+"\n");
			}
			usersDB.close();
			var array = new JsonArray(sb.toString());
			for (int i = 0; i < array.size(); i++) {
				var user = array.getJsonObject(i);
				var key = user.getString("userId");
				users.put(key, new User(key, user.getString("userName")));
				usersIdCount++;
			}
			
		} catch (Exception ex) {
			// ex.printStackTrace();
			logger.info("No dbase, creating a new one");
			saveOnDB();
		}
	}
	
	private void saveOnDB() {
		try {
			JsonArray list = new JsonArray();
			for (User u: users.values()) {
				var obj = new JsonObject();
				obj.put("userId", u.id());
				obj.put("userName", u.name());
				list.add(obj);
			}
			var usersDB = new FileWriter(DB_USERS);
			usersDB.append(list.encodePrettily());
			usersDB.flush();
			usersDB.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}	
	}

	/**
	 * 
	 * Get the address on the Vert.x event bus 
	 * to handle events related to a specific game
	 *  
	 * @param gameId
	 * @return
	 */
	private String getBusAddressForAGame(String gameId) {
		return "ttt-events-" + gameId;
	}

	/* Aux methods */
		
	private void sendReply(HttpServerResponse response, JsonObject reply) {
		response.putHeader("content-type", "application/json");
		response.end(reply.toString());
	}

	private void sendError(HttpServerResponse response) {
		response.setStatusCode(500);
		response.putHeader("content-type", "application/json");
		response.end();
	}

	static final int BACKEND_PORT = 8080;

	/**
	 * 
	 * Main method to launch the backend.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		var vertx = Vertx.vertx();
		var server = new TTTBackend(BACKEND_PORT);
		vertx.deployVerticle(server);	
	}	
	
}
