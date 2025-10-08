package ttt_backend;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.Future;
import io.vertx.core.VerticleBase;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;
import ttt_backend.entities.Game;

public class HttpCommandHandler extends VerticleBase {

    static Logger logger = Logger.getLogger("RestCommands");
    private final CommandsInterface commandable;
    private final HttpServer server;
    private final int port;
    private final Vertx vertx;

    public HttpCommandHandler(final Vertx vertx, final CommandsInterface commandable, HttpServer server, int port) {
        this.vertx = vertx;
        this.commandable = commandable;
        this.server = server;
        this.port = port;
    }

    public Future<?> start() {
        logger.log(Level.INFO, "TTT RestCommands initializing...");

        /* configuring API routes */

        Router router = Router.router(vertx);
        router.route(HttpMethod.POST, "/api/registerUser").handler(this::registerUser);
        router.route(HttpMethod.POST, "/api/createGame").handler(this::createNewGame);
        router.route(HttpMethod.POST, "/api/joinGame").handler(this::joinGame);
        router.route(HttpMethod.POST, "/api/makeAMove").handler(this::makeAMove);

        /* enabling access to static files (web app page) */

        router.route("/public/*").handler(StaticHandler.create());

        /* start the server */

        var fut = server
                .requestHandler(router)
                .listen(port);

        fut.onSuccess(res -> {
            logger.log(Level.INFO, "TTT Game Server ready - port: " + port);
        });

        return fut;
    }

    /**
     * 
     * Register a new user
     * 
     * @param context
     */
    protected void registerUser(RoutingContext context) {
        logger.log(Level.INFO, "RegisterUser request");
        context.request().handler(buf -> {

            /* add the new user */
            JsonObject userInfo = buf.toJsonObject();
            var userName = userInfo.getString("userName");
            var user = this.commandable.registerUser(userName);

            var reply = new JsonObject();
            reply.put("userId", user.id());
            reply.put("userName", user.name());
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
        final Game game = this.commandable.createGame();
        var reply = new JsonObject();
        reply.put("gameId", game.getId());
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
            var reply = new JsonObject();
            try {
                this.commandable.joinGame(userId, gameId, gameSym);
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
            var reply = new JsonObject();
            try {
                JsonObject moveInfo = buf.toJsonObject();
                logger.log(Level.INFO, "move info: " + moveInfo);

                String userId = moveInfo.getString("userId");
                String gameId = moveInfo.getString("gameId");
                String symbol = moveInfo.getString("symbol");
                int x = Integer.parseInt(moveInfo.getString("x"));
                int y = Integer.parseInt(moveInfo.getString("y"));
                var gameSym = symbol.equals("cross") ? Game.GameSymbolType.CROSS : Game.GameSymbolType.CIRCLE;

                this.commandable.makeMove(userId, gameId, x, y, gameSym);

                reply.put("result", "accepted");
                try {
                    sendReply(context.response(), reply);
                } catch (Exception ex) {
                    sendError(context.response());
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
}
