package resources.db;
import java.io.*;
import java.sql.*;

public class DBManager {
    // aqui ficam as queries e a lógica toda das queries
    private Connection conn;

    public DBManager() throws SQLException {
        this.conn = DriverManager.getConnection("jdbc:sqlite:src/resources/db/PD-2023-24-TP.db");
    }

    public boolean connectToDB(String directory, int port) {
        if (new File(directory + "/PD-2023-24-TP-" + port + ".db").exists()) {
            try {
                this.conn = DriverManager.getConnection("jdbc:sqlite:" + directory + "/PD-2022-23-TP-" + port + ".db\"");
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
            fos = new FileOutputStream(directory + "/PD-2023-24-TP-" + port + ".db");
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
            this.conn = DriverManager.getConnection("jdbc:sqlite:" + directory + "/PD-2023-24-TP-" + port + ".db");
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

    public String listAllUsers(int id) throws SQLException {
        Statement statement = conn.createStatement();

        String sqlQuery = "SELECT Id, Nome, Admin, Autenticado, Email, Password FROM utilizador";

        if (id != -1)
            sqlQuery += " WHERE Id like '%" + id + "%'";

        ResultSet resultSet = statement.executeQuery(sqlQuery);

        StringBuilder str = new StringBuilder();
        str.append("ID\tUsername\tNome\tAdministrador\tAutenticado\n");

        while(resultSet.next()){
            int Id = resultSet.getInt("Id");
            String username = resultSet.getString("Nome");
            String nome = resultSet.getString("nome");
            int administrador = resultSet.getInt("Admin");
            int autenticado = resultSet.getInt("Autenticado");

            str.append(id).append("\t").append(username).append("\t").append(nome);
            str.append("\t\t").append(administrador).append("\t\t").append(autenticado).append("\n");
        }

        resultSet.close();
        statement.close();

        return str.toString();
    }

}


