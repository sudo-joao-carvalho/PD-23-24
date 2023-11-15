package model.data;

import resources.ResourceManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Data {

    private ResourceManager resourceManager;

    public Data(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    public boolean connectToDB(String DBDirectory, int port){
        return this.resourceManager.connectToDB(DBDirectory, port);
    }

    /*public int getVersion() {
        this.resourceManager.getVersion();
    }*/

    public int insertUser(ArrayList<String> parameters){
        return this.resourceManager.insertUser(parameters);
    }

    public boolean insertEvent(ArrayList<String> params) { return this.resourceManager.insertEvent(params); }

    public int[] verifyLogin(ArrayList<String> params){ return this.resourceManager.verifyLogin(params); }

    public boolean editProfile(ArrayList<String> params, String email){ return this.resourceManager.editProfile(params, email); }

    public String listPresencas(Integer idEvento, Integer idClient) {
        return this.resourceManager.listPresencas(idEvento, idClient);
    }

    public boolean editEventData(int eventId, ArrayList<String> params) throws SQLException {
        return this.resourceManager.editEventData(eventId, params);
    }

    public boolean insertUserInEvent(ArrayList<String> params) {
        return this.resourceManager.insertUserInEvent(params);
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        return this.resourceManager.deleteEvent(eventId);
    }

    public int addCodeToEvent(Integer eventId) {
        return this.resourceManager.addCodeToEvent(eventId);
    }
}
