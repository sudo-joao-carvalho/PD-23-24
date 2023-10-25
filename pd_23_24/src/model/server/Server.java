package model.server;

import model.ModelManager;
import model.data.DBHelper;
import model.data.Data;
import resources.ResourceManager;
import ui.ServerUI;

import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public class Server {

    public static void main(String[] args) {
        ServerUI serverUI = null;
        try {
            ModelManager modelManager = new ModelManager(Integer.parseInt(args[0]), args[1]/*, Integer.parseInt(args[2])*/);

            //String service = REGISTRY_BIND_NAME + "[" + Integer.parseInt(args[2]) + "]" ;
            //Criar o registo Rmi
            //Registry r = LocateRegistry.createRegistry(Integer.parseInt(args[2]));

            /*try {
                //Associar o Rmi a este Servidor
                r.bind(service , modelManager.getServer());
            } catch (AlreadyBoundException e) {
                throw new RuntimeException(e);
            }*/

            serverUI = new ServerUI(modelManager);
        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }

        serverUI.start();
    }

    private final String DBDirectory;
    private final Data data;

    private HandlerDB handlerDB;
    private DBHelper dbHelper = null;
    private int serverPort;

    public Server(int port, String DBDirectory /*, Integer rmiPort*/) throws SQLException {
        this.serverPort = port;
        this.DBDirectory = DBDirectory;

        this.data = new Data(new ResourceManager());

        handlerDB = new HandlerDB();

        //isto nao vai ser feito aqui deve ser feito quando um novo cliente é criado e ele tera o seu proprio dbhelper
        dbHelper = new DBHelper();

        this.handlerDB.start();
    }


    //TODO adaptar esta class para posteriormente lidar com os queries todos e operaçoes todas
    class HandlerDB extends Thread{

        @Override
        public void run(){
            if(!data.connectToDB(DBDirectory, serverPort)){
                System.out.println("Couldnt connect to database");
                return;
            }else
                System.out.println("Successfully connected to database");

            if(!dbHelper.isRequestAlreadyProcessed())
                data.insertUser(dbHelper.getInsertParams());

            dbHelper.setIsRequestAlreadyProcessed(true);
        }
    }

    /*class HandlerClient extends Thread{

        private Socket clientSocket;
        //private AtomicReference<Boolean> handle;
        private OutputStream os;
        private InputStream is;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private DBHelper dbHelper;

        public HandlerClient(Socket clientSocket, AtomicReference<Boolean> handle,
                             OutputStream os, InputStream is){
            this.clientSocket = clientSocket;
            //this.handle = handle;
            this.os = os;
            this.is = is;
            this.oos = null;
            this.ois = null;
            this.dbHelper = new DBHelper();
        }

        @Override
        public void run(){

        }
    }*/

    //TODO fazer isto depois para fechar a thread
    /*public synchronized void closeServer(){
        this.handlerDB.join(5000);
        this.handlerDB.interrupt();
    }*/

}
