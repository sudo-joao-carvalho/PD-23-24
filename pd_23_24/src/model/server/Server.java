package model.server;

import model.ModelManager;
import model.data.DBHelper;
import model.data.Data;
import resources.ResourceManager;
import ui.ServerUI;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

public class Server {

    public static void main(String[] args) {
        ServerUI serverUI = null;
        try {
            ModelManager modelManager = new ModelManager(Integer.parseInt(args[0]), args[1]/*, Integer.parseInt(args[2])*/);

            serverUI = new ServerUI(modelManager);
        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }

        serverUI.start();
    }

    private final String DBDirectory;
    private final Data data;

    private HandlerDB handlerDB;
    private TCPHandler tcpHandler;
    private DBHelper dbHelper = null;
    private LinkedList<DBHelper> listDbHelper;
    private int serverPort;

    private ArrayList<HandlerClient> clients;
    private AtomicReference<Boolean> handleDB;
    private AtomicReference<Boolean> handleUserExists;
    public boolean isDbHelperReady = false;

    private final Object lock = new Object();

    public Server(int port, String DBDirectory /*, Integer rmiPort*/) throws SQLException {
        this.serverPort = port;
        this.DBDirectory = DBDirectory;

        this.handleDB = new AtomicReference<>(true);
        this.handleUserExists = new AtomicReference<>(false);

        this.data = new Data(new ResourceManager());

        dbHelper = null;
        listDbHelper = new LinkedList<>();

        this.clients = new ArrayList<>();

        tcpHandler = new TCPHandler();
        tcpHandler.start();

        handlerDB = new HandlerDB();
        this.handlerDB.start();
    }


    //TODO adaptar esta class para posteriormente lidar com os queries todos e operaçoes todas
    class HandlerDB extends Thread {

        @Override
        public void run() {

            if (!data.connectToDB(DBDirectory, serverPort)) {
                System.out.println("Couldnt connect to database");
                return;
            } else
                System.out.println("Successfully connected to database");

            /*if(!dbHelper.isRequestAlreadyProcessed())
                switch (dbHelper.getOperation()){
                    case "INSERT" -> {
                        switch (dbHelper.getTable()){
                            case "UTILIZADOR" -> {
                                data.insertUser(dbHelper.getInsertParams());
                            }
                        }

                    }

                }*/
            ;

            /*while(!isDbHelperReady)
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            if (listDbHelper.size() > 0 ){
                DBHelper dbHelper = listDbHelper.get(0);

                System.out.println("4");

                if(!dbHelper.isRequestAlreadyProcessed())
                    if(dbHelper.getOperation() != null){
                        if(!data.insertUser(dbHelper.getInsertParams())){
                            System.out.println("5");
                            handleUserExists.set(true);
                        }
                        else{
                            isDbHelperReady = false;
                            dbHelper.setIsRequestAlreadyProcessed(true);
                        }
                    }


            }*/

            //aqui tenho que fazer handleDB.set(false) no close do servidor

            System.out.println("4");

            while (handleDB.get()) {

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (listDbHelper.size() > 0) {
                    DBHelper dbHelper = listDbHelper.get(0);

                    if (!dbHelper.isRequestAlreadyProcessed())
                        switch (dbHelper.getOperation()) {
                            case "INSERT" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        if (!data.insertUser(dbHelper.getInsertParams())) {
                                            System.out.println("5");
                                            handleUserExists.set(true);

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        } else {
                                            System.out.println("4");
                                            handleUserExists.set(false);
                                            isDbHelperReady = false;
                                            dbHelper.setIsRequestAlreadyProcessed(true);

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }
                                    }
                                    case "evento" -> {
                                        System.out.println("Olá do evento");
                                        if (data.insertEvent(dbHelper.getInsertParams()) == -1) {
                                            System.out.println("Erro INSERT EVENT\n");

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }
                                        else {
                                            System.out.println("Deu bucela");
                                        }
                                    }
                                    case "Presenca" -> {

                                    }
                                }
                            }
                            case "SELECT" -> {

                            }
                            default -> {
                                System.out.println("Erro!\n");
                            }
                        }
                }
            }
        }
    }

    class HandlerClient extends Thread {

        private Socket clientSocket;
        private OutputStream os;
        private InputStream is;
        private ObjectOutputStream oos;
        private ObjectInputStream ois;
        private DBHelper dbHelper;

        public HandlerClient(Socket clientSocket/*, AtomicReference<Boolean> handle*/,
                             OutputStream os, InputStream is) {
            this.clientSocket = clientSocket;
            //this.handle = handle;
            this.os = os;
            this.is = is;
            this.oos = null;
            this.ois = null;
            this.dbHelper = new DBHelper();
        }

        @Override
        public void run() {
            while(true) {
                try {
                    byte[] msg = new byte[1024];
                    int nBytes = is.read(msg);
                    String msgReceived = new String(msg, 0, nBytes);

                    if (msgReceived.equals("NEW REQUEST")) {
                        //continue;
                        System.out.println("\nServer received a new request from Client with\n\tIP:" + clientSocket.getInetAddress().getHostAddress() + "\tPort: " + clientSocket.getPort());
                    }

                    if (oos == null) {
                        oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    }


                    if (ois == null) {
                        ois = new ObjectInputStream(clientSocket.getInputStream());
                    }

                    this.dbHelper = null;
                    try {
                        this.dbHelper = (DBHelper) ois.readObject();

                        listDbHelper.add(this.dbHelper);
                        isDbHelperReady = true;

                        System.out.println("1");

                        synchronized (lock) {
                            try {
                                lock.wait(); // Aguarda notificação
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        handleDB.set(false);

                        if (handleUserExists.get()) {
                            String stringToSend = "EXISTS\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                            //oos.write(stringToSend.getBytes(), 0, stringToSend.length());
                            System.out.println("2");
                        } else {
                            String stringToSend = "NEW\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                            //oos.write(stringToSend.getBytes(), 0, stringToSend.length());

                            listDbHelper.add(this.dbHelper);
                            System.out.println("3");
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                } catch (IOException e) {
                    clients.remove(this);
                }
            }
        }
    }

    class TCPHandler extends Thread {
        @Override
        public void run() {
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(serverPort);
            } catch (IOException e) {
                return;
            }
            Socket socket = null;

            while (true) {
                try {
                    socket = serverSocket.accept();
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();

                    byte[] msg = new byte[1024];
                    int nBytes = is.read(msg);
                    String msgReceived = new String(msg, 0, nBytes);

                    //                if(msgReceived.equals("SERVER")){ // when server communicates with another server
                    //                    System.out.println("\nServer connected with\n\tIP: " + socket.getInetAddress().getHostAddress() + "\tPort: " + serverPort);
                    //                    byte[] buffer = new byte[512];
                    //                    int readBytes = 0;
                    //                    FileInputStream fi = new FileInputStream(DBDirectory + "/PD-2022-23-TP.db"/*"/PD-2022-23-TP-" + serverPort + ".db"*/);
                    //
                    //                    do
                    //                    {
                    //                        readBytes = fi.read(buffer);
                    //                        if(readBytes == -1)
                    //                            break;
                    //                        os.write(buffer, 0, readBytes);
                    //                    }while(readBytes > 0);
                    //
                    //                    fi.close();
                    //                    socket.close();
                    //                }

                    if (msgReceived.equals("CLIENT")) {
                        System.out.println("\nClient connected with\n\tIP: " + socket.getInetAddress().getHostAddress() + "\tPort: " + socket.getPort());// when the server receives a new request from a client
                        //start a new thread to take care of the new client

                        HandlerClient c = new HandlerClient(socket/*, nHandle*/, os, is);
                        c.start();

                        clients.add(c);
                    }

                } catch (IOException e) {
                    break;
                }

            }

            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //TODO fazer isto depois para fechar a thread
    /*public synchronized void closeServer(){
        this.handlerDB.join(5000);
        this.handlerDB.interrupt();
    }*/

}
