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
                    } /*else {
                        // Envia um heartbeat a cada 5 segundos
                        sendHeartbeat(hearbeat);
                    }*/

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

public class Server {
    public static final int TIMEOUT = 20; // seconds TODO alterar para 10
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

    public Server(int port, String dbDirectory /*, Integer rmiPort*/) throws SQLException, IOException {
        this.serverPort = port;
        this.dbDirectory = dbDirectory;

        this.data = new Data(/*new ResourceManager()*/);

        //System.setProperty("java.net.preferIPv4Stack", "true");
        // multicast
        this.mcastSocket = new MulticastSocket(Integer.parseInt(MULTICAST.getValue(1)));
        this.groupIp = InetAddress.getByName(MULTICAST.getValue(0));
        this.socketAddr = new InetSocketAddress(groupIp, Integer.parseInt(MULTICAST.getValue(1)));
        this.networkInterface = NetworkInterface.getByIndex(0);
        this.mcastSocket.joinGroup(socketAddr, networkInterface); // creates group for object comms
        // multicast

        this.heartBeat = new HeartBeat(1099, true, data.getDBVersion()/*, 1*/, dbDirectory, SERVICE_NAME);

        this.sendHeartBeat = new SendHeartBeat(this.heartBeat, mcastSocket, this.dbDirectory);
        sendHeartBeat.start();

        this.tcpHandler = new TcpHandler();
        tcpHandler.start();

        operationResult = new AtomicReference<>("");
        handlerClient = new AtomicReference<>(true);

        this.remoteService = new RemoteService();
        startRemoteService("src/");
    }

    public void startRemoteService(String arg) {
        File localDirectory;

        /*
         * Se existirem varias interfaces de rede activas na maquina onde corre esta aplicacao,
         * convem definir de forma explicita o endereco que deve ser incluido na referencia remota do servico
         * RMI criado. Para o efeito, o endereco deve ser atribuido 'a propriedade java.rmi.server.hostname.
         *
         * Pode ser no codigo atraves do metodo System.setProperty():
         *      - System.setProperty("java.rmi.server.hostname", "10.65.129.232"); //O endereco usado e' apenas um exemplo
         *      - System.setProperty("java.rmi.server.hostname", args[3]); //Neste caso, assume-se que o endereco e' passado como quarto argumento na linha de comando
         *
         * Tambem pode ser como opcao passada 'a maquina virtual Java:
         *      - java -Djava.rmi.server.hostname=10.202.128.22 GetRemoteFileClient ... //O endereco usado e' apenas um exemplo
         *      - No Netbeans: Properties -> Run -> VM Options -> -Djava.rmi.server.hostname=10.202.128.22 //O endereco usado e' apenas um exemplo
         */

        /*
         * Trata os argumentos da linha de comando
         */

        localDirectory = new File(arg.trim());

        if (!localDirectory.exists()) {
            System.out.println("A directoria " + localDirectory + " nao existe!");
            return;
        }

        if (!localDirectory.isDirectory()) {
            System.out.println("O caminho " + localDirectory + " nao se refere a uma diretoria!");
            return;
        }

        if (!localDirectory.canRead()) {
            System.out.println("Sem permissoes de leitura na diretoria " + localDirectory + "!");
            return;
        }

        /*
         * Lanca o rmiregistry localmente no porto TCP por omissao (1099).
         */
        try {

            try {

                System.out.println("Tentativa de lancamento do registry no porto " +
                        Registry.REGISTRY_PORT + "...");

                LocateRegistry.createRegistry(Registry.REGISTRY_PORT);

                System.out.println("Registry lancado!");

            } catch (RemoteException e) {
                System.out.println("Registry provavelmente ja' em execucao!");
            }

            /*
             * Cria o servico.
             */

            //RemoteService service = new RemoteService();

            System.out.println("Servico RemoteService criado e em execucao (" + this.remoteService.getRef().remoteToString() + "...");

            /*
             * Regista o servico no rmiregistry local para que os clientes possam localiza'-lo, ou seja,
             * obter a sua referencia remota (endereco IP, porto de escuta, etc.).
             */

            Naming.bind("rmi://localhost/" + SERVICE_NAME, remoteService);

            System.out.println("Servico " + SERVICE_NAME + " registado no registry...");

            /*
             * Para terminar um servico RMI do tipo UnicastRemoteObject:
             *
             *  UnicastRemoteObject.unexportObject(fileService, true).
             */
            //UnicastRemoteObject.unexportObject(service, true);

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

         private AtomicReference<Boolean> isUserAuth;

        RunnableClientThread(Socket socket) {
            this.socket = socket;
            this.dbHelper = new DBHelper();

            isUserAuth = new AtomicReference<>(false);
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

                    if(!isUserAuth.get()){ //
                        /*try {
                            System.out.println("1");
                            socket.setSoTimeout(TIMEOUT * 1000);
                            if ((this.dbHelper = (DBHelper) ois.readObject()) != null) {
                                System.out.println("\nServer received a new request from Client with\n\tIP:" +
                                        socket.getInetAddress().getHostAddress() + "\tPort: " + socket.getPort());
                            }
                        } catch (SocketTimeoutException e) {
                            // Timeout ocorreu, encerrar a conexão com o cliente
                            System.out.println("Timeout occurred. Closing connection with client.");
                            oos.writeObject("QUIT");
                            socket.close();
                            break;
                        }*/
                        socket.setSoTimeout(TIMEOUT * 1000);
                    }else{
                        socket.setSoTimeout(0);
                        //oos.writeObject("SUCCESS");
                    }

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
                        if(!this.dbHelper.getRequestResult().equals("")){
                            oos.writeObject(this.dbHelper.getRequestResult());
                            this.dbHelper.setRequestResult("");
                            break;
                        }
                    }
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

