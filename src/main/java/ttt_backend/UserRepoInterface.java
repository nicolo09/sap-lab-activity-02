package ttt_backend;

import ttt_backend.entities.User;

public interface UserRepoInterface {

    User addUser(String username);

    User getUserById(String id);

}
