package model.data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DBHelper implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String operation;
    private String table;
    private String column;
    private ArrayList<String> params;
    private AtomicReference<String> requestResult;
    private boolean logout;
    private String email;
    private Integer idEvento;
    private ArrayList<String> verifyEmail;

    private boolean isRequestAlreadyProcessed;


    public DBHelper(){
        this.requestResult = new AtomicReference<>("");
        this.isRequestAlreadyProcessed = false;

        this.column = "";
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

    public void setVerifyEmail(ArrayList<String> verifyEmail) {
        this.verifyEmail = verifyEmail;
    }

    public ArrayList<String> getVerifyEmail() {
        return verifyEmail;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getIdEvento() {
        return idEvento;
    }

    public void setIdEvento(Integer idEvento) {
        this.idEvento = idEvento;
    }

    public AtomicReference<String> getRequestResult() {
        return requestResult;
    }

    public void setRequestResult(String requestResult) {
        this.requestResult.set(requestResult);
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }
}
