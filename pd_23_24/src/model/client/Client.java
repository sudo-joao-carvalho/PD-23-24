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

                socketSr.setSoTimeout(10000);
                os = socketSr.getOutputStream();
                is = socketSr.getInputStream();

                String client = "CLIENT";
                os.write(client.getBytes(), 0, client.length());

                this.sTr = new ConnectToServer(socketSr);
                sTr.start();
                return;
            } catch (IOException e) {
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
            this.ois = null;;
        }

        @Override
        public void run() {
            /*BufferedReader bufferedReaderIn = new BufferedReader(new InputStreamReader(is));*/
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
                                // Agora você pode usar a variável 'result'
                                requestResult.set(result);
                            }
                        } catch (IOException | ClassNotFoundException e) {
                            // Trate as exceções aqui, se necessário
                            e.printStackTrace(); // ou qualquer outra lógica de tratamento desejada
                        }

                        hasNewRequest.set(false);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
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
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> params, String email){
        dbHelper = addDBHelper(queryOperation, sqlTable, params, email);
        hasNewRequest.set(true);
    }

    public void createDBHelper(String queryOperation, String sqlTable, int idEvento, int idUser){
        dbHelper = addDBHelper(queryOperation, sqlTable, idEvento, idUser);
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

        if(operation.equals("DELETE")){
            if(table.equals("evento")){
                deleteEvento(dbHelper, idEvento, idUser);
                isDBHelperReady = true;
                return dbHelper;
            }
        }

        if(operation.equals("UPDATE")){
            if(table.equals("evento")){
                addCodeToEvent(dbHelper, idEvento, idUser);
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
        this.password = loginParams.get(1);
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

    public boolean deleteEvento(DBHelper dbHelper, Integer idEvento, Integer idClient){
        dbHelper.setOperation("DELETE");
        dbHelper.setTable("evento");
        dbHelper.setIdEvento(idEvento);
        return true;
    }

    public boolean addCodeToEvent(DBHelper dbHelper, Integer idEvento, Integer idClient){
        dbHelper.setOperation("UPDATE");
        dbHelper.setTable("evento");
        dbHelper.setIdEvento(idEvento);
        dbHelper.setColumn("codigo");
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
}
