package model.client;

import model.data.DBHelper;
import ui.ClientUI;

import java.io.IOException;
import java.util.ArrayList;

public class Client {

    public static void main(String[] args) {

        ClientUI clientUI = null;

        try{
            Client client = new Client(args[0], Integer.parseInt(args[1]));
            clientUI = new ClientUI(client);
        }catch (IOException e){
            System.out.println("Client did not start");
            return;
        }

        clientUI.start();
    }

    public String serverIP;
    public int serverPort;
    public DBHelper dbHelper;

    public Client(String serverIP, int serverPort) throws IOException{
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public DBHelper addDBHelper(String operation, String table, ArrayList<String> insertParams, int id /*, ArrayList<String> userLogin*/) {
        DBHelper dbHelper = new DBHelper();
        if (operation.equals("INSERT")) {
            if (table.equals("UTILIZADORES")) {
                insertParams.add("0");
                insertParams.add("0");
                insertUser(dbHelper, insertParams);
                return dbHelper;
            }
        }

        return null;
    }

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> userParamsToInsert, int id/*, ArrayList<String> userLogin*/){
        dbHelper = addDBHelper(queryOperation, sqlTable, userParamsToInsert, id /*, userLogin*/);
    }

    public boolean insertUser(DBHelper dbHelper,ArrayList<String> parameters){
        dbHelper.setOperation("INSERT");
        dbHelper.setTable("UTILIZADOR");
        dbHelper.setInsertParams(parameters);
        return true;
    }
}
