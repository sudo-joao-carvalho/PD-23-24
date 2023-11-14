package model.client;

import model.data.DBHelper;
import ui.ClientUI;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

        public ConnectToServer(Socket socketServer) throws IOException {
            this.socketServer = socketServer;
            this.is = socketServer.getInputStream();
            this.os = socketServer.getOutputStream();
            this.oos = null;
            this.ois = null;
        }

        @Override
        public void run() {
            BufferedReader bufferedReaderIn = null;
            bufferedReaderIn = new BufferedReader(new InputStreamReader(is));

            while(true){
                while (!isDBHelperReady) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
                    requestResult.set(""); //reset requestResult
                    String n = "NEW REQUEST";
                    os.write(n.getBytes(StandardCharsets.UTF_8), 0, n.length());
                    os.flush();
                    //os.write("Updated successfully!".getBytes(StandardCharsets.UTF_8));
                    //os.flush();

                    /*byte[] bArray = new byte[1024];
                    int nBytes = is.read(bArray);
                    String msgReceived = new String(bArray, 0, nBytes);*/

                    oos = new ObjectOutputStream(socketServer.getOutputStream());

                    oos.writeObject(dbHelper);
                    isDBHelperReady = false;

                    String msgReceived = bufferedReaderIn.readLine();

                    /*if(ois == null){
                        ois = new ObjectInputStream(socketServer.getInputStream());
                    }*/

                    // Receba a resposta do servidor como um objeto (provavelmente uma String)

                    /*requestResult.set((String) ois.readObject());

                    System.out.println(requestResult);*/

                    //bufferedReaderIn.reset();
                    //System.out.println(msgReceived.length());

                    //System.out.println(msgReceived);

                    if(msgReceived.contains("NEW")) {

                        requestResult.set("true");
                    }else if(msgReceived.contains("EXISTS")){

                        requestResult.set("false");
                        //clientConnected = false;
                    }else if(msgReceived.contains("USER FOUND")) {

                        requestResult.set("User logged in");
                        /*int startIndex = msgReceived.lastIndexOf(":") + 2;
                        String numberStr = msgReceived.substring(startIndex);
                        int idClient = Integer.parseInt(numberStr);

                        dbHelper.setId(idClient);
                        requestResult.set("User exists: " + idClient);*/
                    }else if(msgReceived.contains("USER NOT FOUND")){

                        requestResult.set("User doesnt exist");
                    }else if(msgReceived.contains("EVENT CREATED")){

                        requestResult.set("Event created");
                    }else if(msgReceived.contains("EVENT NOT CREATED")){

                        requestResult.set("Event not created");
                    }else if(msgReceived.contains("UPDATE DONE")){

                        requestResult.set("Update done");
                    }else if(msgReceived.contains("UPDATE NOT DONE")){

                        requestResult.set("Update failed");
                    }else if(msgReceived.contains("PRESENCE LIST")){

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
            /*if(table.equals("evento")){
                listPresencas(dbHelper, params, id);
                isDBHelperReady = true;
                return dbHelper;
            }*/

            //if (table.equals("utilizador")){
                verifyLogin(dbHelper, params);
                isDBHelperReady = true;
                return dbHelper;
<<<<<<< Updated upstream
            //}
=======
            }
        }

        if (operation.equals("DELETE")) {
            if (table.equals("evento")) {

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

>>>>>>> Stashed changes
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

    public String listPresencas(DBHelper dbHelper, ArrayList<String> listParams, int id){ // função para atualizar os detalhes do user (nif, email, nome)
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("evento");
        dbHelper.setParams(listParams);
        dbHelper.setIdPresenca(id);
        return "";
    }
<<<<<<< Updated upstream
=======

    /*public boolean deleteEvent(DBHelper dbHelper) {

    }*/

>>>>>>> Stashed changes
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
