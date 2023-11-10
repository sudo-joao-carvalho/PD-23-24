package model.client;

import model.data.DBHelper;
import ui.ClientUI;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    private String email;
    private int clientID;
    ConnectToServer sTr;


    public Client(String serverIP, int serverPort) throws IOException{
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        requestResult = new AtomicReference<>("");

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

        //parse information about the servers
        //for (String sv : servers){
            //String[] s = sv.split("-");
            try {
                socketSr = new Socket(serverIP, serverPort);

                socketSr.setSoTimeout(10000);
                os = socketSr.getOutputStream();
                is = socketSr.getInputStream();

                String client = "CLIENT";
                os.write(client.getBytes(), 0, client.length());

                this.sTr = new ConnectToServer(socketSr);
                sTr.start();
                return;
            } catch (IOException e) {
                //indexSV.set(indexSV.get()+1 > servers.size()-1? 0 : indexSV.get()+1);
                //continue;
            }
        //}
    }



    class ConnectToServer extends Thread{
        private Socket socketServer;
        private OutputStream os;
        private InputStream is;

        private ObjectOutputStream oos;
        private ObjectInputStream ois;

        private boolean clientConnected;

        private Semaphore resultSemaphore = new Semaphore(0);

        public ConnectToServer(Socket socketServer) throws IOException {
            this.socketServer = socketServer;
            this.is = socketServer.getInputStream();
            this.os = socketServer.getOutputStream();
            this.oos = null;
            this.ois = null;
        }

        @Override
        public void run() {
            BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(is));

            while(true){
                while (!isDBHelperReady) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    //requestResult.set(""); //reset requestResult
                    String n = "NEW REQUEST";
                    os.write(n.getBytes(StandardCharsets.UTF_8), 0, n.length());
                    os.flush();

                    oos = new ObjectOutputStream(socketServer.getOutputStream());

                    oos.writeObject(dbHelper);
                    isDBHelperReady = false;

                    String msgReceived = bufferedReaderIn.readLine();

                    if(msgReceived.contains("NEW")) {
                        StringBuilder idS = new StringBuilder();

                        for(int i = 0; msgReceived.charAt(i) != 'N'; i++){
                            idS.append(msgReceived.charAt(i));
                        }

                        int id = Integer.parseInt(idS.toString());

                        requestResult.set(id + "true");
                    }else if(msgReceived.contains("EXISTS")){
                        requestResult.set("false");
                    }else if(msgReceived.contains("USR FND")) {
                        String idS = "";

                        for(int i = 0; msgReceived.charAt(i) != 'U'; i++){
                            idS += msgReceived.charAt(i);
                        }

                        int id = Integer.parseInt(idS);

                        requestResult.set(id + "User logged in");

                    }else if(msgReceived.equals("USER NOT FOUND")){

                        requestResult.set("User doesnt exist");
                    }else if(msgReceived.equals("EVENT CREATED")){

                        requestResult.set("Event created");
                    }else if(msgReceived.equals("EVENT NOT CREATED")){

                        requestResult.set("Event not created");
                    }else if(msgReceived.equals("UPDATE DONE")){

                        requestResult.set("Update done");
                    }else if(msgReceived.equals("UPDATE NOT DONE")){

                        requestResult.set("Update failed");
                    }else if(msgReceived.equals("PRESENCE LIST")){

                        requestResult.set(msgReceived);
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
                //}
            }

        }
    }

    // gets e sets
    public boolean getIsAdmin() {
        return admin;
    }

    // funções de DBHelper

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> params, int id/*, ArrayList<String> userLogin*/){
        dbHelper = addDBHelper(queryOperation, sqlTable, params, id /*, userLogin*/);
    }

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> params, String email){
        dbHelper = addDBHelper(queryOperation, sqlTable, params, email);
    }

    public void createDBHelper(String queryOperation, String sqlTable, int idEvento, int idUser){
        dbHelper = addDBHelper(queryOperation, sqlTable, idEvento, idUser);
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
        }

        if(operation.equals("SELECT")){
            if (table.equals("utilizador")){
                verifyLogin(dbHelper, params);
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

        return null;
    }

    public DBHelper addDBHelper(String operation, String table, ArrayList<String> params, String email) {
        DBHelper dbHelper = new DBHelper();
        if (operation.equals("UPDATE")) {
            if (table.equals("utilizador")) {
                updateParamUser(dbHelper, params, email);
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
        return true;
    }

    public boolean updateParamUser(DBHelper dbHelper, ArrayList<String> updateParams, String email){ // função para atualizar os detalhes do user (nif, email, nome)
        dbHelper.setOperation("UPDATE");
        dbHelper.setTable("utilizador");
        dbHelper.setParams(updateParams);
        this.email = email;
        dbHelper.setEmail(email);
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

    public int getClientID() {
        return clientID;
    }

    public void setClientID(int clientID) {
        this.clientID = clientID;
    }

    public String getEmail() {
        return email;
    }
}
