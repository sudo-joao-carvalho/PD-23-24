package model.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

public class DBHelper implements Serializable {

    private Integer id;
    private String operation;
    private String table;
    private ArrayList<String> insertParams;

    private AtomicReference<String> requestResult;
    private boolean logout;
    private String username;


    public DBHelper(){
        this.requestResult = new AtomicReference<>("");
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

    public ArrayList<String> getInsertParams() {
        return insertParams;
    }

    public void setInsertParams(ArrayList<String> insertParams) {
        this.insertParams = insertParams;
    }
}
