package model.server;

import model.ModelManager;
import model.data.DBHelper;
import model.data.Data;
import resources.ResourceManager;
import ui.ServerUI;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
    //private AtomicReference<Boolean> handleUserExists;
    //private AtomicReference<Boolean> handleVerifyLogin;
    private AtomicReference<Boolean> handlerClient;

    private AtomicReference<String> operationResult;
    public boolean isDbHelperReady = false;
    private String presenceList;

    private final Object lock = new Object();

    public Server(int port, String DBDirectory /*, Integer rmiPort*/) throws SQLException {
        this.serverPort = port;
        this.DBDirectory = DBDirectory;

        this.handleDB = new AtomicReference<>(true);
        //this.handleUserExists = new AtomicReference<>(false);
        //this.handleVerifyLogin = new AtomicReference<>(false);
        this.handlerClient = new AtomicReference<>(true);
        this.operationResult = new AtomicReference<>("");


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

                    if (!dbHelper.isRequestAlreadyProcessed()) {
                        switch (dbHelper.getOperation()) {
                            case "INSERT" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        if (!data.insertUser(dbHelper.getParams())) {
                                            System.out.println("5");
                                            operationResult.set("insert user fail");
                                            dbHelper.setIsRequestAlreadyProcessed(true);

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        } else {
                                            System.out.println("4");
                                            operationResult.set("insert user done");
                                            //isDbHelperReady = false;
                                            dbHelper.setIsRequestAlreadyProcessed(true);

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }
                                    }
                                    case "evento" -> {
                                        if (data.insertEvent(dbHelper.getParams()) == -1) {
                                            operationResult.set("insert event fail");
                                            dbHelper.setIsRequestAlreadyProcessed(true);

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        } else {
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            operationResult.set("insert event done");

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }
                                    }
                                    case "presenca" -> {

                                    }
                                }
                            }
                            case "SELECT" -> {
                                switch (dbHelper.getTable()){
                                    case "utilizador" -> {
                                        int id = data.verifyLogin(dbHelper.getParams());
                                        System.out.println(id);
                                        if( id != 0){
                                            operationResult.set("select user exist");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            //System.out.println("Usuario ja existe");
                                            dbHelper.setId(id);
                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }else{
                                            operationResult.set("select user doesnt exist");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            //System.out.println("Usuario nao existe");

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }
                                    }
                                    case "evento" -> {
                                        System.out.println("SELECT evento");
                                        presenceList = data.listPresencas(dbHelper.getIdPresenca(), dbHelper.getId());
                                        System.out.println(presenceList);
                                        operationResult.set("select evento done");
                                        dbHelper.setIsRequestAlreadyProcessed(true);
                                        //System.out.println("Usuario ja existe");

                                        synchronized (lock) {
                                            lock.notify();
                                        }
                                    }
                                    case "presenca" -> {


                                    }
                                    default -> System.out.println("default");
                                }
                            }
                            case "UPDATE" -> {
                                switch (dbHelper.getTable()){
                                    case "utilizador" -> {
                                        if(data.editProfile(dbHelper.getParams(), dbHelper.getEmail())){
                                            operationResult.set("update user done");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            //System.out.println("Usuario ja existe");

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }else{
                                            operationResult.set("update user fail");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            //System.out.println("Usuario nao existe");

                                            synchronized (lock) {
                                                lock.notify();
                                            }
                                        }
                                    }
                                }
                            }
                            default -> {
                                System.out.println("Erro!\n");
                            }
                        }
                        listDbHelper.remove(0);
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
            while(handlerClient.get()) {
                try {
                    byte[] msg = new byte[1024];
                    int nBytes = is.read(msg);
                    String msgReceived = new String(msg, 0, nBytes);

                    if (msgReceived.equals("NEW REQUEST")) {
                        //continue;
                        System.out.println("\nServer received a new request from Client with\n\tIP:" + clientSocket.getInetAddress().getHostAddress() + "\tPort: " + clientSocket.getPort());
                        dbHelper.setIsRequestAlreadyProcessed(false);
                        handleDB.set(true);
                        operationResult.set("");
                    }

                    //if (oos == null) {
                        oos = new ObjectOutputStream(clientSocket.getOutputStream());
                    //}


                    //if (ois == null) {
                        ois = new ObjectInputStream(clientSocket.getInputStream());
                    //}

                    //this.dbHelper = null;
                    try {
                        System.out.println("1");
                        this.dbHelper = (DBHelper) ois.readObject();
                        System.out.println("2");
                        listDbHelper.add(this.dbHelper);
                        //isDbHelperReady = true;
                        System.out.println("3");
                        System.out.println(dbHelper.getTable());
                        synchronized (lock) {
                            try {
                                System.out.println("entrei no lock");
                                lock.wait(); // Aguarda notificação
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        if(operationResult.get().equalsIgnoreCase("insert user fail")){
                            String stringToSend = "EXISTS\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("insert user done")){
                            String stringToSend = "NEW\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("select user exist")){
                            String stringToSend = "USER FOUND\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("select user doesnt exist")){
                            String stringToSend = "USER NOT FOUND\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("select evento done")) {
                            String stringToSend = "PRESENCE LIST " + presenceList + "\n";

<<<<<<< Updated upstream
=======
                        if (listDbHelper.size() > 0) {
                            DBHelper dbHelper = listDbHelper.get(0);
                            if (!dbHelper.isRequestAlreadyProcessed()) {
                                switch (dbHelper.getOperation()) {
                                    case "INSERT" -> {
                                        switch (dbHelper.getTable()) {
                                            case "utilizador" -> {
                                                if (!data.insertUser(dbHelper.getParams())) {
                                                    operationResult.set("insert user fail");
                                                    dbHelper.setIsRequestAlreadyProcessed(true);

                                                    /*synchronized (lock) {
                                                        lock.notify();
                                                    }*/
                                                } else {
                                                    operationResult.set("insert user done");
                                                    //isDbHelperReady = false;
                                                    dbHelper.setIsRequestAlreadyProcessed(true);

                                                    /*synchronized (lock) {
                                                        lock.notify();
                                                    }*/
                                                }
                                            }
                                            case "evento" -> {
                                                if (data.insertEvent(dbHelper.getParams()) == -1) {
                                                    operationResult.set("insert event fail");
                                                    dbHelper.setIsRequestAlreadyProcessed(true);

                                                    /*synchronized (lock) {
                                                        lock.notify();
                                                    }*/
                                                } else {
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    operationResult.set("insert event done");

                                                    /*synchronized (lock) {
                                                        lock.notify();
                                                    }*/
                                                }
                                            }
                                            case "presenca" -> {

                                            }
                                        }
                                    }
                                    case "SELECT" -> {
                                        switch (dbHelper.getTable()){
                                            case "utilizador" -> {
                                                int id = data.verifyLogin(dbHelper.getParams());
                                                if (id != 0) {
                                                    operationResult.set("select user exist");
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    dbHelper.setId(id);
                                                    /*synchronized (lock) {
                                                        lock.notify();
                                                    }*/
                                                }else{
                                                    operationResult.set("select user doesnt exist");
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    //System.out.println("Usuario nao existe");

                                                    /*synchronized (lock) {
                                                        lock.notify();
                                                    }*/
                                                }
                                            }
                                            case "evento" -> {
                                                //System.out.println("SELECT evento");
                                                presenceList = data.listPresencas(Integer.parseInt(dbHelper.getParams().get(0)), dbHelper.getId());
                                                //System.out.println(presenceList);
                                                operationResult.set("select evento done");
                                                dbHelper.setIsRequestAlreadyProcessed(true);

                                            /*synchronized (lock) {
                                                lock.notify();
                                            }*/
                                            }
                                            case "presenca" -> {

                                            }
                                            default -> System.out.println("default");
                                    }
                                }
                                case "UPDATE" -> {
                                    switch (dbHelper.getTable()){
                                        case "utilizador" -> {
                                            if(data.editProfile(dbHelper.getParams(), dbHelper.getEmail())){
                                                operationResult.set("update user done");
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                //System.out.println("Usuario ja existe");

                                                /*synchronized (lock) {
                                                    lock.notify();
                                                }*/
                                            }else{
                                                operationResult.set("update user fail");
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                //System.out.println("Usuario nao existe");

                                                /*synchronized (lock) {
                                                    lock.notify();
                                                }*/
                                            }
                                        }
                                    }
                                }
                                default -> {
                                    System.out.println("Erro!\n");
                                }
                            }
                            listDbHelper.remove(0);
                        }
                    //}
                }

                if(operationResult.get().equalsIgnoreCase("insert user fail")){
                        String stringToSend = "EXISTS\n";
                        pso.println(stringToSend);
                    }else if(operationResult.get().equalsIgnoreCase("insert user done")){
                        String stringToSend = "NEW\n";
                        pso.println(stringToSend);
                    }else if(operationResult.get().equalsIgnoreCase("select user exist")){
                        String stringToSend = "USER FOUND\n";
                        pso.println(stringToSend);
                    }else if(operationResult.get().equalsIgnoreCase("select user doesnt exist")){
                        String stringToSend = "USER NOT FOUND\n";

                        pso.println(stringToSend);
                    }else if(operationResult.get().equalsIgnoreCase("select evento done")) {
                        String stringToSend = "PRESENCE LIST " + presenceList + "\n";
>>>>>>> Stashed changes
                            //System.out.println(stringToSend);
                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                            //printStreamOut.println(presenceList);
                        }else if(operationResult.get().equalsIgnoreCase("insert event fail")) {
                            String stringToSend = "EVENT NOT CREATED\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("insert event done")) {
                            String stringToSend = "EVENT CREATED\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("update user done")) {
                            String stringToSend = "UPDATE DONE\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
                        }else if(operationResult.get().equalsIgnoreCase("update user fail")) {
                            String stringToSend = "UPDATE NOT DONE\n";

                            PrintStream printStreamOut = new PrintStream(clientSocket.getOutputStream(), true);
                            printStreamOut.println(stringToSend);
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
                    socket.setSoTimeout(10000);
                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();

                    byte[] msg = new byte[1024];
                    int nBytes = is.read(msg);
                    String msgReceived = new String(msg, 0, nBytes, StandardCharsets.UTF_8);

                    Arrays.fill(msg, (byte) 0);

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
