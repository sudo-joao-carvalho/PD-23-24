package model.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DBHelper implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer id;
    private String operation;
    private String table;
    private ArrayList<String> params;
    private AtomicReference<String> requestResult;
    private boolean logout;
    private String email;
<<<<<<< Updated upstream
    private Integer idPresenca;

    private boolean isRequestAlreadyProcessed;
=======
    private Integer idEvento;
    private int eventCode;
    private int codeExpirationTime;
    private ArrayList<String> verifyEmail;
    private boolean isRequestAlreadyProcessed;
    private boolean isAdmin;
    private boolean getCSV;

    private String searchFilter;
>>>>>>> Stashed changes


    public DBHelper(){
        this.requestResult = new AtomicReference<>("");
        this.isRequestAlreadyProcessed = false;
<<<<<<< Updated upstream
=======

        this.isAdmin = false;
        this.column = "";
        this.getCSV = false;
        this.email = "";
        this.searchFilter = "";

        this.idEvento = -1;
>>>>>>> Stashed changes
    }


    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public ArrayList<String> getParams() {
        return params;
    }

    public void setParams(ArrayList<String> params) {
        this.params = params;
    }

    public boolean isRequestAlreadyProcessed() {
        return isRequestAlreadyProcessed;
    }

    public void setIsRequestAlreadyProcessed(boolean isRequestAlreadyProcessed) {
        this.isRequestAlreadyProcessed = isRequestAlreadyProcessed;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public String getEmail(){return this.email;}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdPresenca() {
        return idPresenca;
    }

    public void setIdPresenca(Integer idPresenca) {
        this.idPresenca = idPresenca;
    }

    public AtomicReference<String> getRequestResult() {
        return requestResult;
    }

    public void setRequestResult(String requestResult) {
        this.requestResult.set(requestResult);
    }
<<<<<<< Updated upstream
=======

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public int getEventCode() {
        return eventCode;
    }

    public void setEventCode(int eventCode) {
        this.eventCode = eventCode;
    }

    public int getCodeExpirationTime() {
        return codeExpirationTime;
    }

    public void setCodeExpirationTime(int codeExpirationTime) {
        this.codeExpirationTime = codeExpirationTime;
    }

    public boolean isGetCSV() {
        return getCSV;
    }

    public void setGetCSV(boolean getCSV) {
        this.getCSV = getCSV;
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
    }
>>>>>>> Stashed changes
}
