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
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

//public class Server {
//
//    public static void main(String[] args) {
//        ServerUI serverUI = null;
//        try {
//            ModelManager modelManager = new ModelManager(Integer.parseInt(args[0]), args[1]/*, Integer.parseInt(args[2])*/);
//
//            serverUI = new ServerUI(modelManager);
//        } catch (SQLException | IOException | InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        serverUI.start();
//    }
//
//    private final String DBDirectory;
//    private final Data data;
//
//    private HandlerDB handlerDB;
//    private TCPHandler tcpHandler;
//    private DBHelper dbHelper = null;
//    private LinkedList<DBHelper> listDbHelper;
//    private int serverPort;
//
//    private ArrayList<HandlerClient> clients;
//    private AtomicReference<Boolean> handleDB;
//    //private AtomicReference<Boolean> handleUserExists;
//    //private AtomicReference<Boolean> handleVerifyLogin;
//    private AtomicReference<Boolean> handlerClient;
//
//    private AtomicReference<String> operationResult;
//    public boolean isDbHelperReady = false;
//    private String presenceList;
//
//    private final Object lock = new Object();
//
//    public Server(int port, String DBDirectory /*, Integer rmiPort*/) throws SQLException {
//        this.serverPort = port;
//        this.DBDirectory = DBDirectory;
//
//        this.handleDB = new AtomicReference<>(true);
//        //this.handleUserExists = new AtomicReference<>(false);
//        //this.handleVerifyLogin = new AtomicReference<>(false);
//        this.handlerClient = new AtomicReference<>(true);
//        this.operationResult = new AtomicReference<>("");
//
//
//        this.data = new Data(new ResourceManager());
//
//        dbHelper = null;
//        listDbHelper = new LinkedList<>();
//
//        this.clients = new ArrayList<>();
//
//        tcpHandler = new TCPHandler();
//        tcpHandler.start();
//
//        handlerDB = new HandlerDB();
//        this.handlerDB.start();
//    }

//    class HandlerClient extends Thread {
//
//        private Socket clientSocket;
//        private OutputStream os;
//        private InputStream is;
//        private ObjectOutputStream oos;
//        private ObjectInputStream ois;
//        private DBHelper dbHelper;
//
//        public HandlerClient(Socket clientSocket/*, AtomicReference<Boolean> handle*/) throws IOException {
//            this.clientSocket = clientSocket;
//            //this.handle = handle;
//            this.os = clientSocket.getOutputStream();
//            this.is = clientSocket.getInputStream();
//            this.oos = null;
//            this.ois = null;
//            this.dbHelper = new DBHelper();
//        }
//
//        @Override
//        public void run() {
//            PrintStream pso = new PrintStream(os, true);
//
//            while(handlerClient.get()) {
//                try{
//                    byte[] msg = new byte[1024];
//                    int nBytes = is.read(msg);
//                    String msgReceived = new String(msg, 0, nBytes);
//
//                    if (msgReceived.equals("NEW REQUEST")) {
//                        System.out.println("\nServer received a new request from Client with\n\tIP:" + clientSocket.getInetAddress().getHostAddress() + "\tPort: " + clientSocket.getPort());
//                        dbHelper.setIsRequestAlreadyProcessed(false);
//                        handleDB.set(true);
//                        //operationResult.set("");
//                    }
//
//                    ois = new ObjectInputStream(clientSocket.getInputStream());
//
//                    this.dbHelper = (DBHelper) ois.readObject();
//                    listDbHelper.add(this.dbHelper);
//
//                    if (!data.connectToDB(DBDirectory, serverPort)) {
//                        System.out.println("Couldnt connect to database");
//                        return;
//                    } else
//                        System.out.println("Successfully connected to database");
//
//                    //aqui tenho que fazer handleDB.set(false) no close do servidor
//
//                    //while (handleDB.get()) {
//                    try {
//                        Thread.sleep(100);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//
//                    if (listDbHelper.size() > 0) {
//                        DBHelper dbHelper = listDbHelper.get(0);
//                        if (!dbHelper.isRequestAlreadyProcessed()) {
//                            switch (dbHelper.getOperation()) {
//                                case "INSERT" -> {
//                                    switch (dbHelper.getTable()) {
//                                        case "utilizador" -> {
//                                            int id = data.insertUser(dbHelper.getParams());
//                                            if (id == 0) { // id quando é 0 falha
//                                                operationResult.set("insert user fail");
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                            } else {
//                                                operationResult.set("insert user done: " + id);
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                            }
//                                        }
//                                        case "evento" -> {
//                                            if (data.insertEvent(dbHelper.getParams()) == -1) {
//                                                operationResult.set("insert event fail");
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                            } else {
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                                operationResult.set("insert event done");
//
//                                            }
//                                        }
//                                        case "presenca" -> {
//
//                                        }
//                                    }
//                                }
//                                case "SELECT" -> {
//                                    switch (dbHelper.getTable()){
//                                        case "utilizador" -> {
//                                            int id = data.verifyLogin(dbHelper.getParams());
//                                            if(id != 0){
//                                                operationResult.set("select user exist: " + id);
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                            }else{
//                                                operationResult.set("select user doesnt exist");
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                            }
//                                        }
//                                        case "evento" -> {
//                                            presenceList = data.listPresencas(dbHelper.getIdEvento(), dbHelper.getId());
//                                            operationResult.set("select evento done");
//                                            dbHelper.setIsRequestAlreadyProcessed(true);
//                                        }
//                                        case "presenca" -> {
//
//
//                                        }
//                                        default -> System.out.println("default");
//                                    }
//                                }
//                                case "UPDATE" -> {
//                                    switch (dbHelper.getTable()){
//                                        case "utilizador" -> {
//                                            if(data.editProfile(dbHelper.getParams(), dbHelper.getEmail())){
//                                                operationResult.set("update user done");
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//                                            }else{
//                                                operationResult.set("update user fail");
//                                                dbHelper.setIsRequestAlreadyProcessed(true);
//
//                                            }
//                                        }
//                                    }
//                                }
//                                default -> {
//                                    System.out.println("Erro!\n");
//                                }
//                            }
//                            listDbHelper.remove(0);
//                        }
//                        //}
//                    }
//
//                    if(operationResult.get().equalsIgnoreCase("insert user fail")){
//                        String stringToSend = "EXISTS";
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().contains("insert user done")){
//                        int startIndex = operationResult.get().lastIndexOf(":") + 2;
//                        String numberStr = operationResult.get().substring(startIndex);
//                        int idClient = Integer.parseInt(numberStr);
//
//                        String stringToSend = idClient + "NEW";
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().contains("select user exist: ")){
//                        System.out.println("entrei select user exist");
//                        int startIndex = operationResult.get().lastIndexOf(":") + 2;
//                        String numberStr = operationResult.get().substring(startIndex);
//                        int idClient = Integer.parseInt(numberStr);
//
//                        String stringToSend = idClient + "USR FND";
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().equalsIgnoreCase("select user doesnt exist")){
//                        String stringToSend = "USER NOT FOUND";
//
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().equalsIgnoreCase("select evento done")) {
//                        String stringToSend = "PRESENCE LIST " + presenceList;
//                        //System.out.println(stringToSend);
//                        pso.println(stringToSend);
//                        //printStreamOut.println(presenceList);
//                    }else if(operationResult.get().equalsIgnoreCase("insert event fail")) {
//                        String stringToSend = "EVENT NOT CREATED";
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().equalsIgnoreCase("insert event done")) {
//                        String stringToSend = "EVENT CREATED";
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().equalsIgnoreCase("update user done")) {
//                        String stringToSend = "UPDATE DONE";
//                        pso.println(stringToSend);
//                    }else if(operationResult.get().equalsIgnoreCase("update user fail")) {
//                        String stringToSend = "UPDATE NOT DONE";
//                        pso.println(stringToSend);
//                    }
//
//                } catch (IOException e) {
//                    clients.remove(this);
//                } catch (ClassNotFoundException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//    }
//
//    class TCPHandler extends Thread {
//        @Override
//        public void run() {
//            ServerSocket serverSocket = null;
//            try {
//                serverSocket = new ServerSocket(serverPort);
//            } catch (IOException e) {
//                return;
//            }
//            Socket socket = null;
//
//            while (true) {
//                try {
//                    socket = serverSocket.accept();
//                    //socket.setSoTimeout(10000);
//                    InputStream is = socket.getInputStream();
//                    OutputStream os = socket.getOutputStream();
//
//                    byte[] msg = new byte[1024];
//                    int nBytes = is.read(msg);
//                    String msgReceived = new String(msg, 0, nBytes, StandardCharsets.UTF_8);
//
//                    Arrays.fill(msg, (byte) 0);
//
//                    if (msgReceived.equals("CLIENT")) {
//                        System.out.println("\nClient connected with\n\tIP: " + socket.getInetAddress().getHostAddress() + "\tPort: " + socket.getPort());// when the server receives a new request from a client
//                        //start a new thread to take care of the new client
//
//                        HandlerClient c = new HandlerClient(socket /* nHandle */);
//                        c.start();
//
//                        clients.add(c);
//                    }
//
//                } catch (IOException e) {
//                    break;
//                }
//
//            }
//
//            try {
//                serverSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    //TODO fazer isto depois para fechar a thread
//    /*public synchronized void closeServer(){
//        this.handlerDB.join(5000);
//        this.handlerDB.interrupt();
//    }*/
//
//}

public class Server {
    public static final int TIMEOUT = 10; // seconds

    public static void main(String[] args) {
        /*if (args.length != 1) {
            System.out.println("Sintaxe: java TcpConcurrentTimeServer listeningPort");
            return;
        }*/

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
    private int serverPort;
    TcpHandler tcpHandler;
    private AtomicReference<String> operationResult;


    private String presenceList;

    public Server(int port, String DBDirectory /*, Integer rmiPort*/) throws SQLException {
        this.serverPort = port;
        this.DBDirectory = DBDirectory;

        this.data = new Data(new ResourceManager());

        this.tcpHandler = new TcpHandler();
        tcpHandler.start();

        operationResult = new AtomicReference<>("");
    }

    class TcpHandler extends Thread{

        @Override
        public void run(){
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                System.out.println("Server initialized successfully. Port used is " + serverSocket.getLocalPort() + ".");

                while (true) {
                    try {
                        Socket toClientSocket = serverSocket.accept();
                        toClientSocket.setSoTimeout(TIMEOUT * 1000);

                        InputStream is = toClientSocket.getInputStream();
                        OutputStream os = toClientSocket.getOutputStream();

                        byte[] msg = new byte[1024];
                        int nBytes = is.read(msg);
                        String msgReceived = new String(msg, 0, nBytes, StandardCharsets.UTF_8);

                        if(msgReceived.equals("CLIENT")){
                            Thread clientThread = new Thread(
                                    new RunnableClientThread(toClientSocket),
                                    toClientSocket.getInetAddress().toString()
                            );
                            clientThread.start();
                        }

                    } catch (Exception e) {
                        System.out.println("a");
                        System.out.println("Error: " + e);
                    }
                }
            } catch (Exception e) {
                System.out.println("b");
                System.out.println("Error: " + e);
            }
        }
    }

     class RunnableClientThread implements Runnable {
        private Socket socket;
        private DBHelper dbHelper;

        RunnableClientThread(Socket socket) {
            this.socket = socket;
            this.dbHelper = new DBHelper();
        }

        @Override
        public void run() {

            /*PrintStream pso = null;
            try {
                pso = new PrintStream(socket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/

            /*PrintStream pso = null;
            try {
                pso = new PrintStream(socket.getOutputStream(), true);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            ObjectInputStream objectInputStream = null;
            try {
                objectInputStream = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/

            try(PrintStream pso = new PrintStream(socket.getOutputStream(), true);
                ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());){
                System.out.println("Client " + socket.getInetAddress() + ":" + socket.getPort());

                while (true) {

                /*String msgReceived = os

                if (msgReceived.equals("NEW REQUEST")) {
                        System.out.println("\nServer received a new request from Client with\n\tIP:" + clientSocket.getInetAddress().getHostAddress() + "\tPort: " + clientSocket.getPort());
                        dbHelper.setIsRequestAlreadyProcessed(false);
                        handleDB.set(true);
                        //operationResult.set("");
                }*/

                    dbHelper.setIsRequestAlreadyProcessed(false);
                    operationResult.set("");

                    if ((this.dbHelper = (DBHelper) objectInputStream.readObject()) != null) {
                        System.out.println("\nServer received a new request from Client with\n\tIP:" + socket.getInetAddress().getHostAddress() + "\tPort: " + socket.getPort());
                    }


                    if (!dbHelper.isRequestAlreadyProcessed()) {
                        switch (dbHelper.getOperation()) {
                            case "INSERT" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        int id = data.insertUser(dbHelper.getParams());
                                        if (id == 0) {  //id quando é 0 falha
                                            operationResult.set("insert user fail");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            operationResult.set("insert user done: " + id);
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "evento" -> {
                                        if (data.insertEvent(dbHelper.getParams()) == -1) {
                                            operationResult.set("insert event fail");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            operationResult.set("insert event done");

                                        }
                                    }
                                    case "presenca" -> {

                                    }
                                }
                            }
                            case "SELECT" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        int id = data.verifyLogin(dbHelper.getParams());
                                        if (id != 0) {
                                            operationResult.set("select user exist: " + id);
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            operationResult.set("select user doesnt exist");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "evento" -> {
                                        presenceList = data.listPresencas(dbHelper.getIdEvento(), dbHelper.getId());
                                        operationResult.set("select evento done");
                                        dbHelper.setIsRequestAlreadyProcessed(true);
                                    }
                                    case "presenca" -> {


                                    }
                                    default -> System.out.println("default");
                                }
                            }
                            case "UPDATE" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        if (data.editProfile(dbHelper.getParams(), dbHelper.getEmail())) {
                                            operationResult.set("update user done");
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            operationResult.set("update user fail");
                                            dbHelper.setIsRequestAlreadyProcessed(true);

                                        }
                                    }
                                }
                            }
                            default -> {
                                System.out.println("Erro!\n");
                            }
                        }
                    }

                    if (operationResult.get().equalsIgnoreCase("insert user fail")) {
                        String stringToSend = "EXISTS";
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().contains("insert user done")) {
                        int startIndex = operationResult.get().lastIndexOf(":") + 2;
                        String numberStr = operationResult.get().substring(startIndex);
                        int idClient = Integer.parseInt(numberStr);

                        String stringToSend = idClient + "NEW";
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().contains("select user exist: ")) {
                        int startIndex = operationResult.get().lastIndexOf(":") + 2;
                        String numberStr = operationResult.get().substring(startIndex);
                        int idClient = Integer.parseInt(numberStr);

                        String stringToSend = idClient + "USR FND";
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().equalsIgnoreCase("select user doesnt exist")) {
                        String stringToSend = "USER NOT FOUND";

                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().equalsIgnoreCase("select evento done")) {
                        String stringToSend = "PRESENCE LIST " + presenceList;
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().equalsIgnoreCase("insert event fail")) {
                        String stringToSend = "EVENT NOT CREATED";
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().equalsIgnoreCase("insert event done")) {
                        String stringToSend = "EVENT CREATED";
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().equalsIgnoreCase("update user done")) {
                        String stringToSend = "UPDATE DONE";
                        pso.println(stringToSend);
                        pso.flush();
                    } else if (operationResult.get().equalsIgnoreCase("update user fail")) {
                        String stringToSend = "UPDATE NOT DONE";
                        pso.println(stringToSend);
                        pso.flush();
                    }


                /*Calendar calendar = GregorianCalendar.getInstance();
                switch (request) {
                    case TIME_REQUEST -> {
                        objectOutputStream.writeObject(new Time(
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                calendar.get(Calendar.SECOND)
                        ));
                        objectOutputStream.flush();
                    }
                    default -> System.out.println("UNEXPECTED");
                */
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }

            /*try {
                socket.close();
            } catch (IOException e) {
                System.out.println("Error: " + e);
            }*/

        }
    }
}

