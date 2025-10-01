package ttt_bbom;

import java.util.logging.Logger;

import io.vertx.core.Vertx;

public class TTTBackendMain {

    static Logger logger = Logger.getLogger("[Main]");	

	public static void main(String[] args) {
		var vertx = Vertx.vertx();
		var server = new TTTBackendVerticle(8080);
		vertx.deployVerticle(server);	
	}

}

