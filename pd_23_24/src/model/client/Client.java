package model.client;

import model.data.DBHelper;
import ui.ClientUI;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
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

    ConnectToServer sTr;

    public Client(String serverIP, int serverPort) throws IOException{
        this.serverIP = serverIP;
        this.serverPort = serverPort;

        //clientInit();

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

    public void connectToServer(){
        Socket socketSr;
        OutputStream os = null;
        InputStream is = null;

        //parse information about the servers
        //for (String sv : servers){
            //String[] s = sv.split("-");
            try {
                socketSr = new Socket(serverIP, serverPort);
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

        public ConnectToServer(Socket socketServer, OutputStream os, InputStream is){
            this.socketServer = socketServer;
            this.is = is;
            this.os = os;
            this.oos = null;
            this.ois = null;
        }

        @Override
        public void run() {
            try {
                String n = "NEW REQUEST";
                os.write(n.getBytes(), 0, n.length());

                byte[] m = new byte[512];
                int nBytes = is.read(m);
            } catch (IOException e) {
            }
        }
    }

    public void createDBHelper(String queryOperation, String sqlTable, ArrayList<String> userParamsToInsert, int id/*, ArrayList<String> userLogin*/){
        dbHelper = addDBHelper(queryOperation, sqlTable, userParamsToInsert, id /*, userLogin*/);
    }

    public DBHelper addDBHelper(String operation, String table, ArrayList<String> insertParams, int id /*, ArrayList<String> userLogin*/) {
        DBHelper dbHelper = new DBHelper();
        if (operation.equals("INSERT")) {
            if (table.equals("utilizador")) {
                //inseridos anteriormente na UI
                //insertParams.add("0");
                //insertParams.add("0");
                insertUser(dbHelper, insertParams);
                return dbHelper;
            }
        }

        return null;
    }

    public boolean insertUser(DBHelper dbHelper,ArrayList<String> userParameters){
        dbHelper.setOperation("INSERT");
        dbHelper.setTable("utilizador");
        dbHelper.setInsertParams(userParameters);
        return true;
    }
}
