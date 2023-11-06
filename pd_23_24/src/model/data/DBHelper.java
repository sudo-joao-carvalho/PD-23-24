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

    private boolean isRequestAlreadyProcessed;


    public DBHelper(){
        this.requestResult = new AtomicReference<>("");
        this.isRequestAlreadyProcessed = false;
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
}
