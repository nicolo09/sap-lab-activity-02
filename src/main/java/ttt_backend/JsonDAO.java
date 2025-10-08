package ttt_backend;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.logging.Logger;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import ttt_backend.entities.User;

public class JsonDAO implements UserRepoInterface {

    /* db file */
    private static final String DB_USERS = "users.json";
    static Logger logger = Logger.getLogger("Json-DAO");
    private HashMap<String, User> users;
    private int usersIdCount = 0;

    public JsonDAO() {
        users = new HashMap<>();
        initFromDB();
    }

    @Override
    public User addUser(final String username) {
        final User user = new User("user-" + usersIdCount, username); 
        users.put("user-" + usersIdCount, user);
        usersIdCount++;
        saveOnDB();
        return user;
    }

    @Override
    public User getUserById(final String id) {
        return users.get(id);
    }

    /* DB management */
    private void initFromDB() {
        try {
            var usersDB = new BufferedReader(new FileReader(DB_USERS));
            var sb = new StringBuffer();
            while (usersDB.ready()) {
                sb.append(usersDB.readLine() + "\n");
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
            for (User u : users.values()) {
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
}
