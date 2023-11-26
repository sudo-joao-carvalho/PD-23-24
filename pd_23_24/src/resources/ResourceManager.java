package resources;

import resources.db.DBManager;

import java.sql.SQLException;
import java.util.ArrayList;

public class ResourceManager {
    private DBManager dbManager;
    public ResourceManager() throws SQLException {
        this.dbManager = new DBManager();
    }

    public boolean connectToDB(String DBDirectory){
        return this.dbManager.connectToDB(DBDirectory);
    }

    //register
    public int insertUser(ArrayList<String> userParameters){
        return this.dbManager.insertUser(userParameters);
    }

    public boolean insertEvent(ArrayList<String> params) {
        return this.dbManager.insertEvent(params);
    }

    public int[] verifyLogin(ArrayList<String> params){return this.dbManager.verifyLogin(params);}

    public boolean editProfile(ArrayList<String> params, Integer idUser){return this.dbManager.editProfile(params, idUser);}

    public String listPresencas(Integer idEvento, Integer idClient) { return this.dbManager.listPresencas(idEvento, idClient); }

    public boolean insertUserInEvent(ArrayList<String> params) {
        return this.dbManager.insertUserInEvent(params);
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        return this.dbManager.deleteEvent(eventId);
    }

    public boolean editEventData(int eventId, ArrayList<String> params) throws SQLException {
        return this.dbManager.editEventData(eventId, params);
    }

    public int addCodeToEvent(Integer eventId, Integer codeExpirationTime) {
        return this.dbManager.addCodeToEvent(eventId, codeExpirationTime);
    }

    public String listPresencasFromUserEmail(String userEmail){
        return this.dbManager.listPresencasFromUserEmail(userEmail);
    }

    public boolean deleteUserFromEvent(ArrayList<String> params) throws SQLException{
        return this.dbManager.deleteUserFromEvent(params);
    }

    public boolean checkEventCodeAndInsertUser(int eventCode, int userID){return this.dbManager.checkEventCodeAndInsertUser(eventCode, userID);}

    public boolean getCSV(int userId) {
        return this.dbManager.getCSV(userId);
    }

    public int getDBVersion(){return this.dbManager.getDBVersion();}

    public boolean getCSVAdmin(int eventId) {
        return this.dbManager.getCSVAdmin(eventId);
    }

    public boolean getCSVAdminListUserAttendanceByEmail(String email) {
        return this.dbManager.getCSVAdminListUserAttendanceByEmail(email);
    }

    public String checkCreatedEvents(String pesquisa) {
        return this.dbManager.checkCreatedEvents(pesquisa);
    }

    public String checkAllRegisteredPresences(int eventId){
        return this.dbManager.checkAllRegisteredPresences(eventId);
    }

    public boolean removeUsersOnEventEnd(){
        return this.dbManager.removeUsersOnEventEnd();
    }

    public String getExecutedQuery(){return this.dbManager.getExecutedQuery();}
}
