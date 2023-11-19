package model.server;

import model.ModelManager;
import model.data.DBHelper;
import model.data.Data;
import model.server.hb.HeartBeat;
import resources.ResourceManager;
import ui.ServerUI;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

class SendHeartBeat extends Thread{

    private static final int HBTIMER = 5;
    private HeartBeat hearbeat;
    private MulticastSocket mcastSocket;
    private AtomicReference<Boolean> isRunning;

    public AtomicReference<Boolean> dbUpdated;

    public SendHeartBeat(HeartBeat hearbeat, MulticastSocket mcastSocket){
        this.hearbeat = hearbeat;
        this.mcastSocket = mcastSocket;

        this.isRunning = new AtomicReference<>(true);
        this.dbUpdated = new AtomicReference<>(false);
    }

    @Override
    public void run(){
        try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos)) {

            while(isRunning.get()){
                if(dbUpdated.get()){ //mandar hearbeat quando a versao da base de dados e updated
                    oos.writeObject(hearbeat);
                    byte[] buffer = baos.toByteArray();
                    DatagramPacket dp = new DatagramPacket(buffer, buffer.length,
                            InetAddress.getByName("230.44.44.44"), 4444);
                    mcastSocket.send(dp);

                    continue;
                }

                try{
                    Thread.sleep(HBTIMER * 1000);
                } catch (InterruptedException e) {
                    this.isRunning.set(false);
                    throw new RuntimeException(e);
                }

                oos.writeObject(hearbeat);
                byte[] buffer = baos.toByteArray();
                DatagramPacket dp = new DatagramPacket(buffer, buffer.length,
                        InetAddress.getByName("230.44.44.44"), 4444);
                mcastSocket.send(dp);
            }

        } catch (IOException e) {
            if (this.isRunning.get()) {
                e.printStackTrace();
            }
            if (!mcastSocket.isClosed()) {
                mcastSocket.close();
            }
        }
    }
}

public class Server {
    public static final int TIMEOUT = 20; // seconds TODO alterar para 10

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

    private String presenceList;

    public Server(int port, String dbDirectory /*, Integer rmiPort*/) throws SQLException, IOException {
        this.serverPort = port;
        this.dbDirectory = dbDirectory;

        // multicast
        this.mcastSocket = new MulticastSocket(Integer.parseInt(MULTICAST.getValue(1)));
        this.groupIp = InetAddress.getByName(MULTICAST.getValue(0));
        this.socketAddr = new InetSocketAddress(groupIp, Integer.parseInt(MULTICAST.getValue(1)));
        this.networkInterface = NetworkInterface.getByIndex(0);
        this.mcastSocket.joinGroup(socketAddr, networkInterface); // creates group for object comms
        // multicast

        this.heartBeat = new HeartBeat(port, true/*, 1, 1*/, dbDirectory);

        this.sendHeartBeat = new SendHeartBeat(this.heartBeat, mcastSocket);
        sendHeartBeat.start();

        this.data = new Data(new ResourceManager());

        this.tcpHandler = new TcpHandler();
        tcpHandler.start();

        operationResult = new AtomicReference<>("");
        handlerClient = new AtomicReference<>(true);
    }

    class TcpHandler extends Thread{

        @Override
        public void run(){
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                System.out.println("Server initialized successfully. Port used is " + serverSocket.getLocalPort() + ".");

                while (true) {
                    try {
                        Socket toClientSocket = serverSocket.accept();

                        //TODO fazer uma verificacao aqui para so fazer isto antes do utilizador estar logado ou algo do genero, pq senao se a pessoa nao escrever nada em 10 segundos da broken pipe pq o socket da timeout
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
                        throw new SocketTimeoutException("Too long to send request to server! Disconnecting...\n");
                        //System.out.println("Error: " + e);
                    }
                }
            } catch (Exception e) {
                //throw new Exception("Failed to create socket!\n");
                //System.out.println("Error: " + e);
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

            if (!data.connectToDB(dbDirectory, serverPort)) {
                System.out.println("Couldnt connect to database");
                return;
            } else
                System.out.println("Successfully connected to database");

            try(ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());){
                System.out.println("Client " + socket.getInetAddress() + ":" + socket.getPort());

                while (handlerClient.get()) {

                    operationResult.set("");

                    if ((this.dbHelper = (DBHelper) ois.readObject()) != null) {
                        System.out.println("\nServer received a new request from Client with\n\tIP:" + socket.getInetAddress().getHostAddress() + "\tPort: " + socket.getPort());
                    }

                    String requestResult = "";
                    while (!dbHelper.isRequestAlreadyProcessed()) {
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
                                            sendHeartBeat.dbUpdated.set(true);
                                        }
                                    }
                                    case "evento" -> {
                                        if (!data.insertEvent(dbHelper.getParams())) {
                                            requestResult = "Event not created";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        } else {
                                            requestResult = "Event created";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            sendHeartBeat.dbUpdated.set(true);
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
                                                sendHeartBeat.dbUpdated.set(true);
                                            }
                                        }else{
                                            if(data.checkEventCodeAndInsertUser(dbHelper.getEventCode(), dbHelper.getId())){
                                                requestResult = "User registered in the event";
                                                dbHelper.setIsRequestAlreadyProcessed(true);
                                                sendHeartBeat.dbUpdated.set(true);
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
                                        } else {
                                            requestResult = "User doesnt exist";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "evento" -> {
                                        if(!dbHelper.getIsAdmin()) {
                                            presenceList = data.listPresencas(dbHelper.getIdEvento(), dbHelper.getId());
                                            requestResult = "PRESENCE LIST\n" + presenceList;
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }else{
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
                                            sendHeartBeat.dbUpdated.set(true);
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
                                                    sendHeartBeat.dbUpdated.set(true);
                                                }
                                            }
                                            case "nome", "local", "data", "horainicio", "horafim" -> {
                                                System.out.println(dbHelper.getParams());
                                                if(data.editEventData(dbHelper.getIdEvento(), dbHelper.getParams())){
                                                    requestResult = "Update done";
                                                    dbHelper.setIsRequestAlreadyProcessed(true);
                                                    sendHeartBeat.dbUpdated.set(true);
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
                                            sendHeartBeat.dbUpdated.set(true);
                                        }else{
                                            requestResult = "Delete evento failed";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                        }
                                    }
                                    case "presenca" -> {
                                        if(data.deleteUserFromEvent(dbHelper.getParams())){
                                            requestResult = "User deleted from event";
                                            dbHelper.setIsRequestAlreadyProcessed(true);
                                            sendHeartBeat.dbUpdated.set(true);
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
                        if(!this.dbHelper.getRequestResult().equals("")){
                            oos.writeObject(this.dbHelper.getRequestResult());
                            this.dbHelper.setRequestResult("");
                            break;
                        }
                    }

                    sendHeartBeat.dbUpdated.set(false);

                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            } catch (SQLException e) {
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

