package model.server;

import model.ModelManager;
import model.data.DBHelper;
import model.data.Data;
import model.server.hb.HeartBeat;
import model.server.rmi.RemoteService;
import model.server.rmi.RemoteServiceInterface;
import ui.ServerUI;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.SQLException;
import java.sql.SQLOutput;
import java.util.concurrent.atomic.AtomicReference;

class SendHeartBeat extends Thread{

    private static final int HBTIMER = 5;
    private HeartBeat hearbeat;
    private MulticastSocket mcastSocket;
    private AtomicReference<Boolean> isRunning;

    public AtomicReference<Boolean> dbUpdated;
    String dbDirectory;

    public SendHeartBeat(HeartBeat hearbeat, MulticastSocket mcastSocket, String dbDirectory){
        this.hearbeat = hearbeat;
        this.mcastSocket = mcastSocket;

        this.isRunning = new AtomicReference<>(true);
        this.dbUpdated = new AtomicReference<>(false);

        this.dbDirectory = dbDirectory;

    }

    private Object dbUpdatedLock = new Object();

    public void notifyDbUpdated() {
        synchronized (dbUpdatedLock) {
            dbUpdatedLock.notify();
            dbUpdated.set(true);
        }
    }

    @Override
    public void run() {
        while (isRunning.get()) {
            try {
                synchronized (dbUpdatedLock) {
                    // Aguarda por HBTIMER segundos ou notificação de atualização da base de dados
                    dbUpdatedLock.wait(HBTIMER * 1000);

                    // Verifica se dbUpdated é true e envia um heartbeat
                    if (dbUpdated.get()) {
                        dbUpdated.set(false);
                    }

                    sendHeartbeat(hearbeat);
                    hearbeat.setUpdateDB(false);
                }
            } catch (InterruptedException | IOException e) {
                if (isRunning.get()) {
                    e.printStackTrace();
                }
                if (!mcastSocket.isClosed()) {
                    mcastSocket.close();
                }
            }
        }
    }

    private void sendHeartbeat(HeartBeat heartbeat) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            oos.writeObject(heartbeat);
            byte[] buffer = baos.toByteArray();
            DatagramPacket dp = new DatagramPacket(buffer, buffer.length,
                    InetAddress.getByName("230.44.44.44"), 4444);
            mcastSocket.send(dp);
        }
    }
}

class RemoveUsersFromEvent extends Thread{

    Data data;

    public RemoveUsersFromEvent(Data data){
        this.data = data;
    }
    @Override
    public void run(){
        while(true){
            if(data.removeUsersOnEventEnd()){
                System.out.println("Users removidos do evento");
            }
        }
    }
}

public class Server {
    public static final int TIMEOUT = 10; // seconds
    public static final String SERVICE_NAME = "TP-PD-2324";

    private final String dbDirectory;
    private final Data data;
    private int serverPort;
    TcpHandler tcpHandler;
    private AtomicReference<String> operationResult;
    private AtomicReference<Boolean> handlerClient;

    // multicast
    private MulticastSocket mcastSocket = null;
    private NetworkInterface networkInterface;
    private InetAddress groupIp;
    private SocketAddress socketAddr;
    // multicast

    private HeartBeat heartBeat;

    SendHeartBeat sendHeartBeat;

    RemoveUsersFromEvent removeUsersFromEvent;

    private String presenceList;

    RemoteService remoteService;

    public static void main(String[] args) {

        if (args.length != 3) {
            System.out.println("Too few arguments to run. Shutting down SERVER.\n");
            return;
        }

        ServerUI serverUI = null;
        try {
            ModelManager modelManager = new ModelManager(Integer.parseInt(args[0]), args[1]/*, Integer.parseInt(args[2])*/);
            serverUI = new ServerUI(modelManager);


        } catch (SQLException | IOException | InterruptedException e) {
            e.printStackTrace();
        }
        serverUI.start();
    }

    public Server(int port, String dbDirectory) throws SQLException, IOException {
        this.serverPort = port;
        this.dbDirectory = dbDirectory;

        this.data = new Data();

        // multicast
        this.mcastSocket = new MulticastSocket(Integer.parseInt(MULTICAST.getValue(1)));
        this.groupIp = InetAddress.getByName(MULTICAST.getValue(0));
        this.socketAddr = new InetSocketAddress(groupIp, Integer.parseInt(MULTICAST.getValue(1)));
        this.networkInterface = NetworkInterface.getByIndex(0);
        this.mcastSocket.joinGroup(socketAddr, networkInterface); // creates group for object comms
        // multicast

        this.heartBeat = new HeartBeat(1099, true, data.getDBVersion(), dbDirectory, SERVICE_NAME);

        this.sendHeartBeat = new SendHeartBeat(this.heartBeat, mcastSocket, this.dbDirectory);
        sendHeartBeat.start();

        this.tcpHandler = new TcpHandler();
        tcpHandler.start();

        operationResult = new AtomicReference<>("");
        handlerClient = new AtomicReference<>(true);

        this.remoteService = new RemoteService();
        startRemoteService();

        this.removeUsersFromEvent = new RemoveUsersFromEvent(this.data);
        removeUsersFromEvent.start();
    }

    public void startRemoteService() {
        try {

            try {

                System.out.println("Tentativa de lancamento do registry no porto " +
                        Registry.REGISTRY_PORT + "...");

                LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

                System.out.println("Registry lancado!");

            } catch (RemoteException e) {
                System.out.println("Registry provavelmente ja' em execucao!");
            }

            System.out.println("Servico RemoteService criado e em execucao (" + this.remoteService.getRef().remoteToString() + "...");

            Naming.bind("rmi://localhost/" + SERVICE_NAME, remoteService);

            System.out.println("Servico " + SERVICE_NAME + " registado no registry...");

        } catch (RemoteException e) {
            System.out.println("Erro remoto - " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Erro - " + e);
            System.exit(1);
        }
    }

    class TcpHandler extends Thread{

        @Override
        public void run(){
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                System.out.println("Server initialized successfully. Port used is " + serverSocket.getLocalPort() + ".");

                while (true) {
                    try {
                        Socket toClientSocket = serverSocket.accept();

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

                    } catch (SocketException e) {
                        throw new SocketTimeoutException("Too long to send request to server! Disconnecting...\n");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

     class RunnableClientThread implements Runnable {
        private Socket socket;
        private DBHelper dbHelper;

         private AtomicReference<Boolean> isUserAuth;

        RunnableClientThread(Socket socket) {
            this.socket = socket;
            this.dbHelper = new DBHelper();

            isUserAuth = new AtomicReference<>(false);
        }

        @Override
        public void run() {

            if (!data.connectToDB(dbDirectory)) {
                System.out.println("Couldnt connect to database");
                return;
            } else
                System.out.println("Successfully connected to database");

            try(ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());){
                System.out.println("Client " + socket.getInetAddress() + ":" + socket.getPort());

                while (handlerClient.get()) {

                    operationResult.set("");

                    if(!isUserAuth.get()){ //
                        socket.setSoTimeout(TIMEOUT * 1000);
                    }else{
                        socket.setSoTimeout(0);
                    }

                    if ((this.dbHelper = (DBHelper) ois.readObject()) != null) {
                        System.out.println("\nServer received a new request from Client with\n\tIP:" + socket.getInetAddress().getHostAddress() + "\tPort: " + socket.getPort());
                    }

                    String requestResult = "";
                    while (true) {
                        assert dbHelper != null;
                        if (dbHelper.isRequestAlreadyProcessed()) break;

                        switch (dbHelper.getOperation()) {
                            case "INSERT" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        int id = data.insertUser(dbHelper.getParams());
                                        if (id == 0) {  //id quando é 0 falha
                                            requestResult = "false";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            requestResult = id + "true";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            heartBeat.setDbVersion(data.getDBVersion());
                                            heartBeat.setQuery(data.getExecutedQuery());
                                            heartBeat.setUpdateDB(true);
                                            sendHeartBeat.notifyDbUpdated();
                                            isUserAuth.set(true);
                                        }
                                    }
                                    case "evento" -> {
                                        if (!data.insertEvent(dbHelper.getParams())) {
                                            requestResult = "Event not created";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            requestResult = "Event created";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            heartBeat.setDbVersion(data.getDBVersion());
                                            heartBeat.setQuery(data.getExecutedQuery());
                                            heartBeat.setUpdateDB(true);
                                            sendHeartBeat.notifyDbUpdated();
                                        }
                                    }
                                    case "presenca" -> {
                                        if(dbHelper.getIsAdmin()){
                                            if(!data.insertUserInEvent(dbHelper.getParams())){
                                                requestResult = "User not inserted in the event";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                            }else{
                                                requestResult = "User successfully inserted in the event";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                heartBeat.setDbVersion(data.getDBVersion());
                                                heartBeat.setQuery(data.getExecutedQuery());
                                                heartBeat.setUpdateDB(true);
                                                sendHeartBeat.notifyDbUpdated();
                                            }
                                        }else{
                                            if(data.checkEventCodeAndInsertUser(dbHelper.getEventCode(), dbHelper.getId())){
                                                requestResult = "User registered in the event";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                heartBeat.setDbVersion(data.getDBVersion());
                                                heartBeat.setQuery(data.getExecutedQuery());
                                                heartBeat.setUpdateDB(true);
                                                sendHeartBeat.notifyDbUpdated();
                                            }else{
                                                requestResult = "Failed registering user in the event";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                            }
                                        }
                                    }
                                }
                            }
                            case "SELECT" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        int[] result = data.verifyLogin(dbHelper.getParams());
                                        int id = result[0];
                                        int isAdmin = result[1];

                                        if (id != 0) {
                                            requestResult = id + "User logged in: " + isAdmin;
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            isUserAuth.set(true);
                                        } else {
                                            requestResult = "User doesnt exist";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "evento" -> {
                                        if(!dbHelper.getIsAdmin()) {
                                            if(dbHelper.isGetCSV()){
                                                data.getCSV(dbHelper.getId());
                                                requestResult = "File obtained";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                break;
                                            }

                                            presenceList = data.listPresencas(dbHelper.getIdEvento(), dbHelper.getId());
                                            requestResult = "PRESENCE LIST\n" + presenceList;
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }else{
                                            if(dbHelper.isGetCSV()){
                                                if(!dbHelper.getEmail().equals("")){
                                                    data.getCSVAdminListUserAttendanceByEmail(dbHelper.getEmail());
                                                    requestResult = "File obtained";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    break;
                                                }

                                                data.getCSVAdmin(dbHelper.getIdEvento());
                                                requestResult = "File obtained";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                break;
                                            }

                                            if(!dbHelper.getSearchFilter().equals("")){
                                                requestResult = data.checkCreatedEvents(dbHelper.getSearchFilter());
                                                if(requestResult.equals("")){
                                                    requestResult = "Search Error";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    break;
                                                }

                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                break;
                                            }

                                            if(dbHelper.getIdEvento() != -1){
                                                requestResult = data.checkAllRegisteredPresences(dbHelper.getIdEvento());
                                                if(requestResult.equals("")){
                                                    requestResult = "Checking presences at event failed";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    break;
                                                }

                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                break;
                                            }

                                            presenceList = data.listPresencasFromUserEmail(dbHelper.getParams().get(0));
                                            requestResult = "LIST ALL PRESENCAS FROM USER\n" + presenceList;
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "presenca" -> {

                                    }
                                    default -> System.out.println("default");
                                }
                            }
                            case "UPDATE" -> {
                                switch (dbHelper.getTable()) {
                                    case "utilizador" -> {
                                        if (data.editProfile(dbHelper.getParams(), dbHelper.getId())) {
                                            requestResult = "Update done";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            heartBeat.setDbVersion(data.getDBVersion());
                                            heartBeat.setQuery(data.getExecutedQuery());
                                            heartBeat.setUpdateDB(true);
                                            sendHeartBeat.notifyDbUpdated();
                                        } else {
                                            requestResult = "Update failed";
                                            dbHelper.setIsRequestAlreadyProcessed(true);

                                        }
                                    }
                                    case "evento" -> {
                                        switch (dbHelper.getColumn()) {
                                            case "codigo" -> {
                                                int codigo = data.addCodeToEvent(dbHelper.getIdEvento(), dbHelper.getCodeExpirationTime());
                                                System.out.println(codigo);
                                                if(codigo == -2){
                                                    requestResult = "Event not happening right now";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                }else if(codigo == 0){
                                                    requestResult = "Couldnt insert the generated code";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                }else{
                                                    requestResult = "Code " + codigo + " inserted successfully";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    heartBeat.setDbVersion(data.getDBVersion());
                                                    heartBeat.setQuery(data.getExecutedQuery());
                                                    heartBeat.setUpdateDB(true);
                                                    sendHeartBeat.notifyDbUpdated();
                                                }
                                            }
                                            case "nome", "local", "data", "horainicio", "horafim" -> {
                                                if(data.editEventData(dbHelper.getIdEvento(), dbHelper.getParams())){
                                                    requestResult = "Update done";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    heartBeat.setDbVersion(data.getDBVersion());
                                                    heartBeat.setQuery(data.getExecutedQuery());
                                                    heartBeat.setUpdateDB(true);
                                                    sendHeartBeat.notifyDbUpdated();
                                                }else{
                                                    requestResult = "Update failed";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            case "DELETE" -> {
                                switch (dbHelper.getTable()){
                                    case "evento" -> {
                                        if(data.deleteEvent(dbHelper.getIdEvento())){
                                            requestResult = "Delete evento done";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            heartBeat.setDbVersion(data.getDBVersion());
                                            heartBeat.setQuery(data.getExecutedQuery());
                                            heartBeat.setUpdateDB(true);
                                            sendHeartBeat.notifyDbUpdated();
                                        }else{
                                            requestResult = "Delete evento failed";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "presenca" -> {
                                        if(data.deleteUserFromEvent(dbHelper.getParams())){
                                            requestResult = "User deleted from event";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            heartBeat.setDbVersion(data.getDBVersion());
                                            heartBeat.setQuery(data.getExecutedQuery());
                                            heartBeat.setUpdateDB(true);
                                            sendHeartBeat.notifyDbUpdated();
                                        }else{
                                            requestResult = "Couldnt delete user from event";
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

                    dbHelper.setRequestResult(requestResult);

                    while(true){
                        if(!this.dbHelper.getRequestResult().toString().equals("")){
                            oos.writeObject(this.dbHelper.getRequestResult());
                            this.dbHelper.setRequestResult("");
                            break;
                        }
                    }
                }

            } catch (SocketTimeoutException e) {
                System.out.println("\nCan't read from client, took too long to login. Not accepting any more requests from that user.\n\n");
            }catch(EOFException e){
                System.out.println("\nUser unexpectedly disconnected");
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

