package model.client;

import model.data.DBHelper;
import ui.ClientUI;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
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

                this.sTr = new ConnectToServer(socketSr, os, is);
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

        public ConnectToServer(Socket socketServer, OutputStream os, InputStream is){
            this.socketServer = socketServer;
            this.is = is;
            this.os = os;
            this.oos = null;
            this.ois = null;
        }

        @Override
        public void run() {
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
                    os.write(n.getBytes(), 0, n.length());

                    /*byte[] bArray = new byte[1024];
                    int nBytes = is.read(bArray);
                    String msgReceived = new String(bArray, 0, nBytes);*/

                    oos = new ObjectOutputStream(socketServer.getOutputStream());

                    oos.writeObject(dbHelper);
                    isDBHelperReady = false;

                    BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(socketServer.getInputStream()));
                    String msgReceived = bufferedReaderIn.readLine();


                    //bufferedReaderIn.reset();
                    //System.out.println(msgReceived.length());

                    //System.out.println(msgReceived);

                    if(msgReceived.contains("NEW")) {
                        System.out.println(msgReceived);
                        requestResult.set("true");
                    }else if(msgReceived.contains("EXISTS")){
                        System.out.println(msgReceived);
                        requestResult.set("false");
                        //clientConnected = false;
                    }else if(msgReceived.contains("USER FOUND")) {
                        System.out.println(msgReceived);
                        requestResult.set("User logged in");
                        /*int startIndex = msgReceived.lastIndexOf(":") + 2;
                        String numberStr = msgReceived.substring(startIndex);
                        int idClient = Integer.parseInt(numberStr);

                        dbHelper.setId(idClient);
                        requestResult.set("User exists: " + idClient);*/
                    }else if(msgReceived.contains("USER NOT FOUND")){
                        System.out.println(msgReceived);
                        requestResult.set("User doesnt exist");
                    }else if(msgReceived.contains("EVENT CREATED")){
                        System.out.println(msgReceived);
                        requestResult.set("Event created");
                    }else if(msgReceived.contains("EVENT NOT CREATED")){
                        System.out.println(msgReceived);
                        requestResult.set("Event not created");
                    }else if(msgReceived.contains("UPDATE DONE")){
                        System.out.println(msgReceived);
                        requestResult.set("Update done");
                    }else if(msgReceived.contains("UPDATE NOT DONE")){
                        System.out.println(msgReceived);
                        requestResult.set("Update failed");
                    }else if(msgReceived.contains("PRESENCE LIST")){
                        System.out.println(msgReceived);
                        requestResult.set(msgReceived);
                        //System.out.println(msgReceived);
                    }

                    //dbHelper.setIsRequestAlreadyProcessed(false);

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
            if(table.equals("evento")){
                listPresencas(dbHelper, params, id);
                listPresencas(dbHelper, id);

                isDBHelperReady = true;
                return dbHelper;
            }

            if (table.equals("utilizador")){
                verifyLogin(dbHelper, params);
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
        dbHelper.setEmail(email);
        return true;
    }

    public String listPresencas(DBHelper dbHelper, Integer idEvento){ // função para atualizar os detalhes do user (nif, email, nome)
        dbHelper.setId(idEvento == -1 ? null : idEvento); // se for igual a -1 então null, senão busca o evento com idEvento
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
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
