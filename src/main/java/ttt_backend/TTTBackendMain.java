package ttt_backend;

import io.vertx.core.Vertx;

/**
*
* TicTacToe Game Server backend - without a clear software architecture
* 
* @author aricci
*
*/

public class TTTBackendMain {

	static final int BACKEND_PORT = 8080;

	public static void main(String[] args) {
		var vertx = Vertx.vertx();
		var server = new TTTBackendController(BACKEND_PORT);
		vertx.deployVerticle(server);	
	}

}

