package model;

import model.server.Server;
import resources.ResourceManager;

import java.io.IOException;
import java.sql.SQLException;

public class ModelManager {
    private Server server;

    public ModelManager(int port, String DBDirectory/*, int rmiPort*/) throws SQLException, IOException, InterruptedException {
        this.server = new Server(port, DBDirectory/*, rmiPort*/);
    }

    /*public String listAllAvailableServers(){
        return this.server.listAllAvailableServers();
    }
    public void closeServer() throws IOException, InterruptedException, SQLException {
        this.server.closeServer();
    }*/

    public Server getServer() {
        return server;
    }
}
