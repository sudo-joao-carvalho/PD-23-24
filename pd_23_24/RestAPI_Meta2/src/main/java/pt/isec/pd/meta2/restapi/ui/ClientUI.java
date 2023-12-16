package pt.isec.pd.meta2.restapi.ui;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import pt.isec.pd.meta2.restapi.consumer.Consumer;
import pt.isec.pd.meta2.restapi.utils.InputProtection;

public class ClientUI {
    private boolean isAdmin = false;

    private int currentUserId = 0;

    public boolean loginRegister() throws IOException {
        int option = InputProtection.chooseOption("Choose a menu: " , "Login","Register","Exit");

        switch (option){
            case 1 -> {
                return login();
            }
            case 2 -> {
                return register();
            }
            default -> System.exit(0);

        }

        return false;
    }

    public boolean login() throws IOException {

        String email = InputProtection.readString("\tEmail: ", true);
        String password = InputProtection.readString("\tPassword: ", true);

        String loginUri = "http://localhost:8080/login";

        String credentials = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());

        String token = Consumer.sendRequestAndShowResponse(loginUri, "POST", "basic " + credentials, null);

        if (token == null) {
            return false;
        }

        return true;
    }

    public boolean register() throws IOException {
        String nome = InputProtection.readString("\tNome: ", false);
        String email = InputProtection.readString("\tEmail: ", true);
        int nif = InputProtection.readInt("\tNIF: ");
        String password = InputProtection.readString("\tPassword: ", true);

        String registerUri = "http://localhost:8080/register";

        String requestBody = String.format("{\"nome\":\"%s\",\"email\":\"%s\",\"nif\":%d,\"password\":\"%s\"}", nome, email, nif, password);

        String response = Consumer.sendRequestAndShowResponse(registerUri, "POST", null, requestBody);

        if (response != null) {
            if (response.contains("Utilizador registado com sucesso!")) {
                String[] splitString = response.split("\\s+"); // dá split por " " (espaço em branco)

                this.currentUserId = Integer.parseInt(splitString[1]); // dá assign ao id do utilizador atual na segunda string (pq o return é "Utilizador %d registado com sucesso) segunda parte é o id

                System.out.println("Registo bem-sucedido!");
                System.out.println(this.currentUserId);
                return true;
            } else if (response.contains("Não foi possível registar o utilizador")) {
                System.out.println("Erro no registo. Já existe um utilizador com o mesmo email.");
            } else {
                System.out.println("Erro no registo. Verifique os detalhes e tente novamente.");
            }
        } else {
            System.out.println("Erro de comunicação. Tente novamente.");
        }
        return false;
    }

    public boolean submitEventCode() throws IOException {

        int userId = this.currentUserId;

        int eventCode = InputProtection.readInt("\tInsira o código do evento: ");

        String submitEventCodeUri = "http://localhost:8080/submit?userId=" + userId + "&eventCode=" + eventCode;

        String response = Consumer.sendRequestAndShowResponse(submitEventCodeUri, "PUT", null, null);

        if (response != null) {
            if (response.contains("Código inserido com sucesso")) {
                System.out.println("Código inserido com sucesso. Presença registada no evento.");
                return true;
            } else if (response.contains("Não foi possível registá-lo no evento")) {
                System.out.println("Erro: Não foi possível registar o código. Verifique se o código está correto ou se o evento existe.");
            } else {
                System.out.println("Erro desconhecido ao registar o código do evento.");
            }
        } else {
            System.out.println("Erro de comunicação. Tente novamente mais tarde.");
        }

        return false;
    }

    public void userMenu() {

    }

    public void adminUI() {

    }
    public void start() throws IOException {
        while (true) {
            if (loginRegister()) {
                if (!isAdmin) {
                    userMenu();
                } else if (isAdmin) {
                    adminUI();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        ClientUI clientUI = new ClientUI();
        clientUI.start();
    }

}