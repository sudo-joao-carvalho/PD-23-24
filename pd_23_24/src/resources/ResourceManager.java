package resources;

import resources.db.DBManager;

import java.sql.SQLException;

public class ResourceManager {
    private DBManager dbManager;
    public ResourceManager() throws SQLException {
        this.dbManager = new DBManager();
    }

    public String listUsers(Integer userId) throws SQLException {
        return this.dbManager.listAllUsers(userId);
    }

    // funções de BD (add, remove, consulta)

    // aqui só puxa do DBManager, só chama basicamente
}
