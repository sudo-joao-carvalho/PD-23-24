package resources;

import resources.db.DBManager;

import java.sql.SQLException;
import java.util.ArrayList;

public class ResourceManager {
    private DBManager dbManager;
    public ResourceManager() throws SQLException {
        this.dbManager = new DBManager();
    }

    public boolean connectToDB(String DBDirectory, int port){
        return this.dbManager.connectToDB(DBDirectory, port);
    }

    /*public String listUsers(Integer userId) throws SQLException {
        return this.dbManager.listAllUsers(userId);
    }*/

    public int insertUser(ArrayList<String> userParameters){
        return this.dbManager.insertUser(userParameters);
    }

    public int insertEvent(ArrayList<String> params) {
        return this.dbManager.insertEvent(params);
    }

    public int verifyLogin(ArrayList<String> params){return this.dbManager.verifyLogin(params);}

    public boolean editProfile(ArrayList<String> params, String email){return this.dbManager.editProfile(params, email);}

    public String listPresencas(Integer idEvento, Integer idClient) { return this.dbManager.listPresencas(idEvento, idClient); }

    // funções de BD (add, remove, consulta)

    // aqui só puxa do DBManager, só chama basicamente
}
