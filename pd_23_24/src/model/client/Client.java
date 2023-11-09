package model.client;

import model.data.DBHelper;
import org.sqlite.core.DB;
import ui.ClientUI;

import java.io.*;
import java.lang.reflect.Array;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
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
        //clientInit();
        requestResult = new AtomicReference<>("");

        connectToServer(); //pelo TCP
    }

//    public boolean clientInit() {
//        DatagramSocket socket;
//        InetAddress ip;
//        try {
//            socket = new DatagramSocket();
//            socket.setSoTimeout(1000);
//            ip = InetAddress.getByName(serverIP);
//        } catch (IOException e) {
//            System.out.println("Error");
//            return false;
//        }
//        String message = "CONNECTION";
//        DatagramPacket packetSent = new DatagramPacket(message.getBytes(), message.getBytes().length, ip, serverPort);
//
//        try {
//            socket.send(packetSent);
//        } catch (IOException e) {
//            return false;
//        }
//
//        DatagramPacket packetReceived = new DatagramPacket(new byte[256], 256);
//
//        try {
//            socket.receive(packetReceived);
//        } catch (IOException e) {
//            System.out.println("Error");
//
//            return false;
//        }
//
//        String messageReceived = new String(packetReceived.getData(), 0, packetReceived.getLength());
//
//        if(messageReceived.isEmpty()){
//            return false;
//        }
//
//        String[] strings = messageReceived.split("\\|");
//
//        /*servers.clear();
//        servers.addAll(Arrays.asList(strings));
//        System.out.println(servers);*/
//        socket.close();
//        return true;
//    }

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

                socketSr.setSoTimeout(1000);
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
                //if(isDBHelperReady){
                while (!isDBHelperReady) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                try {
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

                    //System.out.println(msgReceived.length());

                    //System.out.println(msgReceived);


                    if(msgReceived.contains("NEW")) {

                        requestResult.set("true");
                        System.out.println("entrei");
                        //clientConnected = true;
                        /*if (ois == null) {
                            ois = new ObjectInputStream(socketServer.getInputStream());
                        }

                        if (oos == null) {
                            oos = new ObjectOutputStream(socketServer.getOutputStream());
                        }*/

                    }else if(msgReceived.contains("EXISTS")){
                        requestResult.set("false");
                        //clientConnected = false;
                    }else if(msgReceived.contains("USER FOUND")) {
                        requestResult.set("User exists");
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
            verifyLogin(dbHelper, params);
            isDBHelperReady = true;
            return dbHelper;
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

    public String listAllPresencas(DBHelper dbHelper, ArrayList<String> listParams){ // função para atualizar os detalhes do user (nif, email, nome)
        dbHelper.setOperation("SELECT");
        dbHelper.setTable("Presenca");
        dbHelper.setParams(listParams);
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
