package model.data;

import resources.ResourceManager;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

public class Data {

    private ResourceManager resourceManager;

    public Data() throws SQLException {
        this.resourceManager = new ResourceManager();
    }

    public boolean connectToDB(String DBDirectory){
        return this.resourceManager.connectToDB(DBDirectory);
    }

    //login
    public int insertUser(ArrayList<String> parameters){
        return this.resourceManager.insertUser(parameters);
    }

    public boolean insertEvent(ArrayList<String> params) { return this.resourceManager.insertEvent(params); }

    public int[] verifyLogin(ArrayList<String> params){ return this.resourceManager.verifyLogin(params); }

    public boolean editProfile(ArrayList<String> params, Integer idUser){ return this.resourceManager.editProfile(params, idUser); }

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

    public int addCodeToEvent(Integer eventId, Integer codeExpirationTime) {
        return this.resourceManager.addCodeToEvent(eventId, codeExpirationTime);
    }

    public String listPresencasFromUserEmail(String userEmail){
        return this.resourceManager.listPresencasFromUserEmail(userEmail);
    }

    public boolean deleteUserFromEvent(ArrayList<String> params) throws SQLException {
        return this.resourceManager.deleteUserFromEvent(params);
    }

    public boolean checkEventCodeAndInsertUser(int eventCode, int userID){return this.resourceManager.checkEventCodeAndInsertUser(eventCode, userID);}

    public boolean getCSV(int userId) {
        return this.resourceManager.getCSV(userId);
    }

    public int getDBVersion() {
        return this.resourceManager.getDBVersion();
    }

    public boolean getCSVAdmin(int eventId) {
        return this.resourceManager.getCSVAdmin(eventId);
    }

    public boolean getCSVAdminListUserAttendanceByEmail(String email) {
        return this.resourceManager.getCSVAdminListUserAttendanceByEmail(email);
    }

    public String checkCreatedEvents(String pesquisa) {
        return this.resourceManager.checkCreatedEvents(pesquisa);
    }

    public String checkAllRegisteredPresences(int eventId) {
        return this.resourceManager.checkAllRegisteredPresences(eventId);
    }

    public boolean removeUsersOnEventEnd(){
        return this.resourceManager.removeUsersOnEventEnd();
    }

    public String getExecutedQuery(){return this.resourceManager.getExecutedQuery();}
}
