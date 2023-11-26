package model.client;

import model.data.DBHelper;
import ui.ClientUI;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

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
    public boolean isDBHelperReady = false;
    private boolean admin = false;
    public AtomicReference<String> requestResult;
    public AtomicReference<Boolean> srHandle;
    public AtomicReference<Boolean> hasNewRequest;
    private String email;
    private String password;
    private int clientID;
    ConnectToServer sTr;


    public Client(String serverIP, int serverPort) throws IOException{
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        requestResult = new AtomicReference<>("");

        srHandle = new AtomicReference<>(true);
        hasNewRequest = new AtomicReference<>(false);

        connectToServer(); //pelo TCP
    }

    public String waitToReceiveResultRequest(){
        while(true){
            if(!requestResult.get().equals("")){
                return requestResult.get();
            }
        }
    }

    public void connectToServer(){
        Socket socketSr;
        OutputStream os = null;
        InputStream is = null;

        try {
            socketSr = new Socket(serverIP, serverPort);

            os = socketSr.getOutputStream();
            is = socketSr.getInputStream();

            String client = "CLIENT";
            os.write(client.getBytes(), 0, client.length());

            this.sTr = new ConnectToServer(socketSr);
            sTr.start();
        }catch(SocketException e){
            System.out.println("Can't read from server!\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    class ConnectToServer extends Thread{
        private Socket socketServer;
        private OutputStream os;
        private InputStream is;

        private ObjectOutputStream oos;
        private ObjectInputStream ois;

        public ConnectToServer(Socket socketServer) throws IOException {
            this.socketServer = socketServer;
            this.is = socketServer.getInputStream();
            this.os = socketServer.getOutputStream();
            this.oos = null;
            this.ois = null;;
        }

        @Override
        public void run() {
            ObjectOutputStream oos = null;
            try {
                oos = new ObjectOutputStream(os);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ObjectInputStream ois = null;
            try{
                ois = new ObjectInputStream(is);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            while(srHandle.get()){

                if(hasNewRequest.get()){
                    requestResult.set(""); //reset requestResult
                    try {

                        oos.writeObject(dbHelper);

                        isDBHelperReady = false;

                        try {
                            Object receivedObject = ois.readObject();

                            if (receivedObject instanceof AtomicReference) {
                                AtomicReference<String> atomicReference = (AtomicReference<String>) receivedObject;
                                String result = atomicReference.get();

                                requestResult.set(result);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            e.printStackTrace();
                        }

                        hasNewRequest.set(false);

                    } catch (SocketException e) {
                        System.out.println("Can't read or write to server! Reason: you took too long to login\n");
                        System.exit(-1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    // funções de DBHelper

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> params, int id/*, ArrayList<String> userLogin*/){
        dbHelper = addDBHelper(queryOperation, sqlTable, params, id /*, userLogin*/);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> params, int id, boolean getCSV){
        dbHelper = addDBHelper(queryOperation, sqlTable, params, id, getCSV);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> params, String email, int userID){
        dbHelper = addDBHelper(queryOperation, sqlTable, params, email, userID);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, int idEvento, int idUser){
        dbHelper = addDBHelper(queryOperation, sqlTable, idEvento, idUser);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, int idEvento, int idUser, boolean isAdmin, boolean getCSV){
        dbHelper = addDBHelper(queryOperation, sqlTable, idEvento, idUser, isAdmin, getCSV);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, int idUser, boolean getCSV){
        dbHelper = addDBHelper(queryOperation, sqlTable, idUser, getCSV);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, int idEvento, int codeExpirationTime, int idUser){
        dbHelper = addDBHelper(queryOperation, sqlTable, idEvento, codeExpirationTime, idUser);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, String eventCode, int userID){
        dbHelper = addDBHelper(queryOperation, sqlTable, eventCode, userID);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, String search){
        dbHelper = addDBHelper(queryOperation, sqlTable, search);
        hasNewRequest.set(true);
    }

    public DBHelper addDBHelper(String operation, String table, ArrayList<String> params, int id /*, ArrayList<String> userLogin*/) {
        DBHelper dbHelper = new DBHelper();
        if (operation.equals("INSERT")) {
            if (table.equals("utilizador")) {
                insertUser(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
            }
            if (table.equals("evento")) {
                insertEvento(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
            }
            if(table.equals("presenca")){
                insertUserInEvent(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        if(operation.equals("SELECT")){
            if (table.equals("utilizador")){
                verifyLogin(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
            }

            if(table.equals("evento")){
                listPresencasFromUserEmail(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
            }
        }


        if(operation.equals("UPDATE")){
            if(table.equals("evento")){
                editEventData(dbHelper, params, id);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        if(operation.equals("DELETE")){
            if(table.equals("presenca")){
                deleteUserInEvent(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
            }
        }
        return null;
    }

    public DBHelper addDBHelper(String operation, String table, ArrayList<String> params, int id, boolean getCSV) {
        DBHelper dbHelper = new DBHelper();
        if(operation.equals("SELECT")){
            if(table.equals("evento")){
                listPresencasFromUserEmailCSV(dbHelper, params, getCSV);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, int idEvento, int idUser) {
        DBHelper dbHelper = new DBHelper();
        if(operation.equals("SELECT")){
            if(table.equals("evento")){
                listPresencas(dbHelper, idEvento, idUser);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        if(operation.equals("DELETE")){
            if(table.equals("evento")){
                deleteEvento(dbHelper, idEvento, idUser);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, int idEvento, int idUser, boolean isAdmin, boolean getCSV) {
        DBHelper dbHelper = new DBHelper();
        if(operation.equals("SELECT")){
            if(table.equals("evento")){
                if(getCSV)
                    getCSVEventPresences(dbHelper, idEvento, idUser, isAdmin, true);
                else checkRegisteredPresences(dbHelper, idEvento, idUser, isAdmin, false);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, int idUser, boolean getCSV) {
        DBHelper dbHelper = new DBHelper();
        if(operation.equals("SELECT")){
            if(table.equals("evento")){
                getCSV(dbHelper, idUser, getCSV);
                isDBHelperReady = true;
                return dbHelper;
            }
        }
        return null;
    }

    public DBHelper addDBHelper(String operation, String table, ArrayList<String> params, String email, int userID) {
        DBHelper dbHelper = new DBHelper();
        if (operation.equals("UPDATE")) {
            if (table.equals("utilizador")) {
                updateParamUser(dbHelper, params, email, userID);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, String eventCode, int userID) {
        DBHelper dbHelper = new DBHelper();
        if (operation.equals("INSERT")) {
            if (table.equals("presenca")) {
                checkEventCodeAndInsertUser(dbHelper, eventCode, userID);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, int idEvento, int codeExpirationTIme, int idUser){
        DBHelper dbHelper = new DBHelper();
        if(operation.equals("UPDATE")){
            if(table.equals("evento")){
                addCodeToEvent(dbHelper, idEvento, codeExpirationTIme, idUser);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, String search) {
        DBHelper dbHelper = new DBHelper();
        if(operation.equals("SELECT")){
            if(table.equals("evento")){
                checkCreatedEvents(dbHelper, search);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        return null;
    }


    public boolean insertUser(DBHelper dbHelper, ArrayList<String> userParameters){
        dbHelper.setOperation("INSERT");
        dbHelper.setTable("utilizador");
        dbHelper.setParams(userParameters);
        this.email = userParameters.get(1);

        return true;
    }

    public boolean insertEvento(DBHelper dbHelper, ArrayList<String> eventParams) {
        dbHelper.setOperation("INSERT");
        dbHelper.setTable("evento");
        dbHelper.setParams(eventParams);
        return true;
    }

    public boolean verifyLogin(DBHelper dbHelper, ArrayList<String> loginParams) {
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("utilizador");
        dbHelper.setParams(loginParams);

        this.email = loginParams.get(0);
        this.password = loginParams.get(1);
        return true;
    }

    public boolean updateParamUser(DBHelper dbHelper, ArrayList<String> updateParams, String email, int userID){ // função para atualizar os detalhes do user (nif, email, nome)
        dbHelper.setOperation("UPDATE");
        dbHelper.setTable("utilizador");
        dbHelper.setParams(updateParams);
        this.email = email;
        dbHelper.setEmail(email);
        dbHelper.setId(userID);
        return true;
    }

    public String listPresencas(DBHelper dbHelper, Integer idEvento, Integer idUser){ // função para atualizar os detalhes do user (nif, email, nome)
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setIdEvento(idEvento);
        dbHelper.setId(idUser);
        //dbHelper.setParams(listParams);
        return "";
    }

    public boolean deleteEvento(DBHelper dbHelper, Integer idEvento, Integer idClient){
        dbHelper.setOperation("DELETE");
        dbHelper.setTable("evento");
        dbHelper.setIdEvento(idEvento);
        return true;
    }

    public boolean addCodeToEvent(DBHelper dbHelper, Integer idEvento, Integer codeExpirationTime, Integer idClient){
        dbHelper.setOperation("UPDATE");
        dbHelper.setTable("evento");
        dbHelper.setIdEvento(idEvento);
        dbHelper.setColumn("codigo");
        dbHelper.setCodeExpirationTime(codeExpirationTime);
        return true;
    }

    public boolean editEventData(DBHelper dbHelper, ArrayList<String> params, Integer idEvento){
        dbHelper.setOperation("UPDATE");
        dbHelper.setTable("evento");
        dbHelper.setParams(params);
        dbHelper.setIdEvento(idEvento);
        dbHelper.setColumn(params.get(0));
        return true;
    }

    public boolean listPresencasFromUserEmail(DBHelper dbHelper, ArrayList<String> params){
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setParams(params);
        dbHelper.setIsAdmin(true);
        return true;
    }

    public boolean insertUserInEvent(DBHelper dbHelper, ArrayList<String> params){
        dbHelper.setOperation("INSERT");
        dbHelper.setTable("presenca");
        dbHelper.setParams(params);
        dbHelper.setIsAdmin(true);
        return true;
    }

    public boolean deleteUserInEvent(DBHelper dbHelper, ArrayList<String> params){
        dbHelper.setOperation("DELETE");
        dbHelper.setTable("presenca");
        dbHelper.setParams(params);
        dbHelper.setIsAdmin(true);
        return true;
    }

    public boolean checkEventCodeAndInsertUser(DBHelper dbHelper, String eventCode, int clientID) {
        dbHelper.setOperation("INSERT");
        dbHelper.setTable("presenca");
        dbHelper.setEventCode(Integer.parseInt(eventCode));
        dbHelper.setId(clientID);
        dbHelper.setIsAdmin(false);
        return true;
    }

    public boolean getCSV(DBHelper dbHelper, int clientId, boolean getCSV) { // isto é para o que não é ADMIN
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setId(clientId);
        dbHelper.setIsAdmin(false);
        dbHelper.setGetCSV(getCSV);
        return true;
    }

    public boolean checkRegisteredPresences(DBHelper dbHelper, int idEvento, int idUser, boolean isAdmin, boolean getCSV){
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setIsAdmin(isAdmin);
        dbHelper.setIdEvento(idEvento);
        dbHelper.setGetCSV(getCSV);
        return true;
    }

    public boolean getCSVEventPresences(DBHelper dbHelper, int idEvento, int idUser, boolean isAdmin, boolean getCSV){
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setIsAdmin(isAdmin);
        dbHelper.setIdEvento(idEvento);
        dbHelper.setGetCSV(getCSV);
        return true;
    }

    public boolean listPresencasFromUserEmailCSV(DBHelper dbHelper, ArrayList<String> params, boolean getCSV){
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setEmail(params.get(0));
        dbHelper.setIsAdmin(true);
        dbHelper.setGetCSV(getCSV);
        return true;
    }

    public boolean checkCreatedEvents(DBHelper dbHelper, String pesquisa) {
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setIsAdmin(true);
        dbHelper.setSearchFilter(pesquisa);
        return true;
    }

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public String getEmail() {
        return email;
    }

    // gets e sets
    public boolean getIsAdmin() {
        return admin;
    }

    public void setIsAdmin(boolean isAdmin) {
        this.admin = isAdmin;
    }

    public void closeClient(){
        srHandle.set(false);
    }
}
