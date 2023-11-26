package model;

import model.server.Server;
import resources.ResourceManager;

import java.io.IOException;
import java.sql.SQLException;

public class ModelManager {
    private Server server;

    public ModelManager(int port, String DBDirectory) throws SQLException, IOException, InterruptedException {
        this.server = new Server(port, DBDirectory);
    }
    public Server getServer() {
        return server;
    }
}
