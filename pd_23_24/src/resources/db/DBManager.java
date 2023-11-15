package resources.db;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DBManager {
    // aqui ficam as queries e a lógica toda das queries
    private Connection conn;

    public DBManager() throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:src/resources/db/PD-2023-24-TP.db");
    }

    public boolean connectToDB(String directory, int port) {
        if (new File(directory + "/PD-2023-24-TP.db" /*"/PD-2023-24-TP-" + port + ".db"*/).exists()) {
            try {
                this.conn = DriverManager.getConnection("jdbc:sqlite:" + directory + "/PD-2023-24-TP.db"/*"/PD-2022-23-TP-" + port + ".db\""*/);
            } catch(SQLException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        File file = new File("src/resources/db/PD-2023-24-TP.db");

        FileInputStream fis;

        try {
            fis = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        FileOutputStream fos;
        try{
            fos = new FileOutputStream(directory + "/PD-2023-24-TP.db"/*"/PD-2023-24-TP-" + port + ".db"*/);
        }catch (FileNotFoundException e){
            return false;
        }

        byte[] buf = new byte[1024];

        int nBytes = 0;

        while (nBytes >= 0) {
            try {
                nBytes = fis.read(buf);
                fos.write(buf);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        try {
            fos.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + directory + "/PD-2023-24-TP.db"/*"/PD-2023-24-TP-" + port + ".db"*/);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void close() throws SQLException
    {
        if (conn != null)
            conn.close();
    }

    public String listAllUsers(Integer id) throws SQLException {
        Statement statement = conn.createStatement();

        String sqlQuery = "SELECT Id, Nome, Admin, Autenticado, Email, Password, NIF FROM utilizador";

        if (id != -1)
            sqlQuery += " WHERE Id like '%" + id + "%'";

        ResultSet resultSet = statement.executeQuery(sqlQuery);

        StringBuilder str = new StringBuilder();
        str.append("ID\tUsername\tNome\tAdministrador\tAutenticado\tPassword\tEmail\t\t\t\t\tNIF\t");

        while(resultSet.next()){
            int Id = resultSet.getInt("Id");
            String username = resultSet.getString("Nome");
            String nome = resultSet.getString("nome");
            int administrador = resultSet.getInt("Admin");
            int autenticado = resultSet.getInt("Autenticado");
            String email = resultSet.getString("Email");
            String password = resultSet.getString("Password");
            int nif = resultSet.getInt("NIF");


            str.append(id).append("\t").append(username).append("\t").append(nome);
            str.append("\t\t").append(administrador).append("\t\t").append(autenticado).append("\t\t").append(password).append("\t\t").append(email).append("\t\t").append(nif);
        }

        resultSet.close();
        statement.close();

        return str.toString();
    }

    public int insertEvent(ArrayList<String> params) { // precisa de devolver o id do evento criado, não apagar o return disto!
        Statement statement;

        try {
            statement = conn.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }

        int i = 0;

        String sqlQuery = "INSERT INTO Evento VALUES (NULL, 0, '" +
                params.get(i++) + "' , '" + params.get(i++) + "' , '" +
                params.get(i++) + "' , '" + params.get(i++) + "' , '" +
                params.get(i) + "')";

        try {
            statement.executeUpdate(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            ResultSet rs = statement.getGeneratedKeys();
            return rs.getInt(1); // devolve o id do novo evento
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public int insertCodeByAdmin(int eventID) throws SQLException {
        Statement stmt = null;

        try {
            stmt = conn.createStatement();

            Random rnd = new Random();

            int minVal = 100000;

            int maxVal = 999999;

            int eventCode = rnd.nextInt(maxVal - minVal + 1) + minVal; // get random code for event

            String sqlQuery = "UPDATE Evento SET Codigo='" + eventCode + "' WHERE Id=" + eventID;

            stmt.executeUpdate(sqlQuery);

        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public int insertUser(ArrayList<String> userParameters){

        Statement statement = null;
        try{
            statement = conn.createStatement();
        }catch (SQLException e){
            return 0;
        }


        boolean existeRegisto = false;
        int idRegisto = 0;

        // Verificar se há algum com nome ou utilizador igual
        String verificar = "SELECT 1 FROM utilizador WHERE lower(email) = lower('" + userParameters.get(1) + "') OR lower(password) = lower('" + userParameters.get(3) + "')";
        try {
            ResultSet resultSet = statement.executeQuery(verificar);

            // Se houver algum registro no ResultSe"t, definimos existeRegistro como true
            existeRegisto = resultSet.next();

        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                // Lidar com a exceção, se necessário.
            }
        }

        if (existeRegisto) {
            // Já existe um registro com nome ou utilizador igual, então retornamos false.
            return 0;
        }

        int i = 0;

        String sqlQuery = "INSERT INTO utilizador VALUES (NULL, '" + userParameters.get(i++) + "' , '" +
                userParameters.get(i++) + "' , '" + userParameters.get(i++) + "' , '" +
                userParameters.get(i++) + "' , '" + userParameters.get(i++) + "' , '" + userParameters.get(i++) + "')";

        try{
            statement.executeUpdate(sqlQuery);
            //saveQuery(sqlQuery);
        }catch (SQLException e){
            e.printStackTrace();
            return 0;
        }finally {
            try{
                statement.close();
            }catch (SQLException e){

            }
        }

        String getId = "SELECT id FROM utilizador WHERE lower(email) = lower('" + userParameters.get(1) + "') OR lower(password) = lower('" + userParameters.get(3) + "')";
        try {
            ResultSet resultSet = statement.executeQuery(getId);

            // Se houver algum registro no ResultSe"t, definimos existeRegistro como true
            idRegisto = resultSet.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                // Lidar com a exceção, se necessário.
            }
        }
        //updateVersion();
        return idRegisto;
    }

    public int[] verifyLogin(ArrayList<String> params){

        Statement statement = null;
        try{
            statement = conn.createStatement();
        }catch (SQLException e){
            return null;
        }

        int idRegisto = 0;
        int isAdmin = 0;

        String verificar = "SELECT id, admin FROM utilizador WHERE lower(email) = lower('" + params.get(0) + "') AND lower(password) = lower('" + params.get(1) + "')";
        try {
            ResultSet resultSet = statement.executeQuery(verificar);

            // Se houver algum registro no ResultSet, definimos existeRegistro como true
            idRegisto = resultSet.getInt("id");
            isAdmin = resultSet.getInt("admin");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                // Lidar com a exceção, se necessário.
            }
        }

        return new int[]{idRegisto, isAdmin};
    }

    public String listPresencas(Integer idEvento, Integer idClient) {

        Statement statement = null;
        try{
            statement = conn.createStatement();
        }catch (SQLException e){
            return "";
        }

        String sqlQuery = null;

        if(idEvento == -1){
            /*sqlQuery = "SELECT distinct evento.nome, evento.local, evento.data, evento.horaInicio FROM evento " +
                    "JOIN presenca ON evento.id = presenca.idEvento " +
                    "JOIN utilizador ON utilizador.id = Presenca.idUtilizador " +
                    "WHERE utilizador.id = '" + idClient + "'";*/
            sqlQuery = "SELECT evento.Nome, evento.Local, evento.Data, evento.HoraInicio " +
                    "FROM Evento evento " +
                    "JOIN Presenca presenca ON evento.Id = presenca.IdEvento " +
                    "JOIN Utilizador utilizador ON presenca.IdUtilizador = utilizador.Id " +
                    "WHERE utilizador.Id=" + idClient;

            //sqlQuery = "SELECT * FROM Evento";
        }else{
            sqlQuery = "SELECT distinct evento.Nome, evento.Local, evento.Data, evento.HoraInicio FROM evento " +
                    "JOIN presenca ON evento.Id = presenca.IdEvento " +
                    "JOIN utilizador ON utilizador.Id = presenca.IdUtilizador " +
                    "WHERE evento.Id=" + idEvento +
                    " AND utilizador.Id=" + idClient;
        }


        ResultSet resultSet = null;
        StringBuilder str = new StringBuilder();
        try{
            resultSet = statement.executeQuery(sqlQuery);

            str.append("ID\tID Evento\tID Utilizador\t");

            while(resultSet.next()){
                String nome = resultSet.getString("Nome");
                String local = resultSet.getString("Local");
                String data = resultSet.getString("Data");
                String horaInicio = resultSet.getString("HoraInicio");

                str.append(idEvento).append("\t").append(nome).append("\t").append(local);
                str.append("\t\t").append(data).append(horaInicio).append("\t\t");
            }

            //saveQuery(sqlQuery);
        }catch (SQLException e){
            e.printStackTrace();
            return "";
        }finally {
            try{
                if(resultSet != null) resultSet.close();
                statement.close();
            }catch (SQLException e){

            }
        }

        return str.toString();
    }

    public boolean editProfile(ArrayList<String> params, String email){

        Statement statement = null;
        try{
            statement = conn.createStatement();
        }catch (SQLException e){
            return false;
        }

        if(params.get(0).equalsIgnoreCase("name")){
            String newName = params.get(1);
            String sqlQuery = "UPDATE utilizador SET Nome = '" + newName + "' WHERE lower(email) = lower('" + email + "')";

            try{
                statement.executeUpdate(sqlQuery);
                //saveQuery(sqlQuery);
            }catch (SQLException e){
                e.printStackTrace();
                return false;
            }finally {
                try{
                    statement.close();
                }catch (SQLException e){

                }
            }

            return true;
        }else if(params.get(0).equalsIgnoreCase("email")){
            String newEmail = params.get(1);
            String sqlQuery = "UPDATE utilizador SET Email = '" + newEmail + "' WHERE lower(email) = lower('" + email + "')";

            try{
                statement.executeUpdate(sqlQuery);
                //saveQuery(sqlQuery);
            }catch (SQLException e){
                e.printStackTrace();
                return false;
            }finally {
                try{
                    statement.close();
                }catch (SQLException e){

                }
            }

            return true;
        }else if(params.get(0).equalsIgnoreCase("password")){
            String newPassword = params.get(1);
            String sqlQuery = "UPDATE utilizador SET password = '" + newPassword + "' WHERE lower(email) = lower('" + email + "')";

            try{
                statement.executeUpdate(sqlQuery);
                //saveQuery(sqlQuery);
            }catch (SQLException e){
                e.printStackTrace();
                return false;
            }finally {
                try{
                    statement.close();
                }catch (SQLException e){

                }
            }

            return true;
        }if(params.get(0).equalsIgnoreCase("nif")){
            int newNif = Integer.parseInt(params.get(1));
            String sqlQuery = "UPDATE utilizador SET nif = '" + newNif + "' WHERE lower(email) = lower('" + email + "')";

            try{
                statement.executeUpdate(sqlQuery);
                //saveQuery(sqlQuery);
            }catch (SQLException e){
                e.printStackTrace();
                return false;
            }finally {
                try{
                    statement.close();
                }catch (SQLException e){

                }
            }

            return true;
        }

        return false;
    }

    public boolean insertUserInEvent(ArrayList<String> params) {
        Statement statement = null;

        try {
            statement = conn.createStatement();

            String sqlQuery = "INSERT INTO presenca VALUES (NULL, (SELECT id FROM utilizador WHERE email='" + params.get(0) + "'), (SELECT id FROM EVENTO WHERE nome='" + params.get(1) + "'))";

            statement.executeQuery(sqlQuery);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public boolean deleteEvent(int eventId) throws SQLException {

        if (getTotalAttendanceForEventAsInt(eventId) >= 1) { //so pode ser eliminado se n tiver nenhuma presenca
            return false;
        }

        Statement statement = null;

        try {
            statement = conn.createStatement();

            String sqlQuery = "DELETE FROM evento WHERE id=" + eventId;

            //statement.executeQuery(sqlQuery); estava a lancar excecao

            int rowsAffected = statement.executeUpdate(sqlQuery);

            if (rowsAffected == 0) {
                // Se nenhum registro foi afetado, pode ser que o evento com o ID fornecido não exista.
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            }
            catch (SQLException e) {
                e.printStackTrace();
            }

        }

        return true;
    }

    public int getTotalAttendanceForEventAsInt(int eventId) throws SQLException {

        if (eventId <= 0) {
            throw new IndexOutOfBoundsException("Invalid Id!");
        }

        Statement statement = null;

        try {
            statement = conn.createStatement();

            String sqlQuery = "SELECT Count(*) AS totalPresencas FROM presenca WHERE Presenca.IdEvento=" + eventId;

            ResultSet rs = statement.executeQuery(sqlQuery);

            return rs.getInt("totalPresencas");

        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return 0;
    }

    public boolean editEventData(Integer eventId, HashMap<String, String> params) throws SQLException {

        if (getTotalAttendanceForEventAsInt(eventId) != 0) { // se o evento já tiver uma presença ou mais, então gg
            return false;
        }

        Statement statement = null;

        String sqlQuery = "";

        try {
            statement = conn.createStatement();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                switch (entry.getKey().toLowerCase()) {
                    case "codigo" -> {
                        // verificar depois qunado o evento está a correr e assim, o código mais recente é que prevalece tb
                        //Random rnd = new Random();

                        //int minVal = 100000;

                        //int maxVal = 999999;

                        //int eventCode = rnd.nextInt(maxVal - minVal + 1) + minVal; // get random code for event

                        // tirei porque não sei se o código é random ou é o admin que mete

                        // depois não esquecer do tempo em minutos da validade do código

                        sqlQuery = "UPDATE evento SET codigo='" + entry.getValue() + "' WHERE id=" + eventId;
                    }

                    case "nome" -> {
                        sqlQuery = "UPDATE evento SET nome='" + entry.getValue() + "' WHERE id=" + eventId;
                    }
                    case "local" -> {
                        sqlQuery = "UPDATE evento SET local='" + entry.getValue() + "' WHERE id=" + eventId;
                    }
                    case "data" -> {
                        sqlQuery = "UPDATE evento SET data='" + entry.getValue() + "' WHERE id=" + eventId;
                    }
                    case "horainicio" -> {
                        sqlQuery = "UPDATE evento SET horainicio='" + entry.getValue() + "' WHERE id=" + eventId;
                    }

                    case "horafim" -> {
                        sqlQuery = "UPDATE evento SET horafim='" + entry.getValue() + "' WHERE id=" + eventId;
                    }
                }
                statement.executeUpdate(sqlQuery);
            }
        } catch (SQLException e){
            return false;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return true;
    }

    public int addCodeToEvent(Integer eventId){ //TODO so se pode mudar codigo quando o evento ja esta a decorrer

        Statement statement = null;
        try{
            statement = conn.createStatement();
        }catch (SQLException e){
            System.out.println("1");
            return 0;
        }

        Random random = new Random();
        int eventCode = random.nextInt(900000) + 100000;
        System.out.println(eventCode);

        //String sqlQuery = "INSERT INTO evento VALUES (SELECT codigo FROM evento WHERE id = '" + eventId + "'")";

        //String sqlQuery = "INSERT INTO evento (codigo) SELECT codigo FROM evento WHERE id = " + eventId;

        String sqlQuery = "UPDATE evento SET codigo = '" + eventCode + "' WHERE id = " + eventId;

        try {
            //ResultSet resultSet = statement.executeQuery(verificar);

            int rowsAffected = statement.executeUpdate(sqlQuery);

            if (rowsAffected == 0) {
                System.out.println("2");
                // Se nenhum registro foi afetado, pode ser que o evento com o ID fornecido não exista.
                return 0;
            }else{
                return eventCode;
            }

            // Se houver algum registro no ResultSet, definimos existeRegistro como true
        } catch (SQLException e) {
            System.out.println("3");
            e.printStackTrace();
            return 0;
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                // Lidar com a exceção, se necessário.
            }
        }

    }

}


