package pt.isec.pd.meta2.restapi.ui;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import pt.isec.pd.meta2.restapi.consumer.Consumer;
import pt.isec.pd.meta2.restapi.models.Evento;
import pt.isec.pd.meta2.restapi.models.Utilizador;
import pt.isec.pd.meta2.restapi.utils.InputProtection;
import com.google.gson.Gson;

public class ClientUI {
    private boolean isAdmin = false;

    private int currentUserId = 0;

    private String email;

    private String password;

    private String token;

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

        this.email = InputProtection.readString("\tEmail: ", true);
        this.password = InputProtection.readString("\tPassword: ", true);

        String loginUri = "http://localhost:8080/login";

        String credentials = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());

        if (email.equals("admin") && password.equals("admin")) {
            this.isAdmin = true;
        }

        String token = Consumer.sendRequestAndShowResponse(loginUri, "POST", "basic " + credentials, null);

        if (token == null) {
            return false;
        }

        this.token = token;

        return true;
    }

    public boolean register() throws IOException {
        String nome = InputProtection.readString("\tNome: ", false);
        String email = InputProtection.readString("\tEmail: ", true);
        int nif = InputProtection.readInt("\tNIF: ");
        String password = InputProtection.readString("\tPassword: ", true);

        Utilizador newUser = new Utilizador();

        newUser.setAdmin(0);

        newUser.setEmail(email);

        newUser.setPassword(password);

        newUser.setNif(nif);

        newUser.setNome(nome);

        String registerUri = "http://localhost:8080/register";

        Gson gson = new Gson();

        String requestBody = gson.toJson(newUser);

        String response = Consumer.sendRequestAndShowResponse(registerUri, "POST", null, requestBody);

        if (response != null) {
            if (response.contains("sucesso!")) {
                String[] splitString = response.split("\\s+"); // dá split por " " (espaço em branco)

                this.currentUserId = Integer.parseInt(splitString[1]); // dá assign ao id do utilizador atual na segunda string (pq o return é "Utilizador %d registado com sucesso) segunda parte é o id
                System.out.println("Registo bem-sucedido!");
                this.email = email;
                this.password = password;
                this.isAdmin = false;
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

        String response = Consumer.sendRequestAndShowResponse(submitEventCodeUri, "PUT", "bearer " + this.token, null);

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

    public void isAdmin() throws IOException{
        String isAdminUri = "http://localhost:8080/isAdmin";

        String response = Consumer.sendRequestAndShowResponse(isAdminUri, "GET", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void listPresencas() throws IOException {
        String listPresencasUri = "http://localhost:8080/list";

        String credentials = Base64.getEncoder().encodeToString((this.email + ":" + this.password).getBytes());

        String response = Consumer.sendRequestAndShowResponse(listPresencasUri, "GET", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void userMenu() throws IOException {
        while(true){
            System.out.print("\nMenu de User");

            int input = InputProtection.chooseOption("Escolher acao: ",  "Submeter codigo de evento", "Verificar se e admin",  "Consultar as suas presencas registadas", "Sair");

            switch (input){
                case 1 -> {
                    submitEventCode();
                }
                case 2 -> {
                    isAdmin();
                }
                case 3 -> {
                    listPresencas();
                }
                case 4 -> {
                    System.exit(1);
                }
                default -> {
                    System.out.println("Input inválido!");
                }
            }
        }
    }

    public boolean createEvent() throws IOException {

        String local = InputProtection.readString("\tLocal: ", false);
        String nome = InputProtection.readString("\tNome: ", false);
        String dia = InputProtection.readString("\tDia: ", true);
        String mes = InputProtection.readString("\tMês: ", true);
        String ano = InputProtection.readString("\tAno: ", true);
        String horaInicio = InputProtection.readString("Hora início (apenas hora): ", true);
        String minutoInicio = InputProtection.readString("Minuto da hora início (apenas minutos): ", true);
        String horaFim = InputProtection.readString("Hora fim (apenas hora): ", true);
        String minutoFim = InputProtection.readString("Minuto da hora fim (apenas minutos): ", true);

        Evento eventoNovo = new Evento();

        eventoNovo.setLocal(local);
        eventoNovo.setNome(nome);
        eventoNovo.setData(dia + '/' + mes + '/' + ano);
        eventoNovo.setHoraInicio(horaInicio + ':' + minutoInicio);
        eventoNovo.setHoraFim(horaFim + ':' + minutoFim);

        Gson gson = new Gson();

        String requestBody = gson.toJson(eventoNovo);

        System.out.println(requestBody);

        String createEventUri = "http://localhost:8080/event/admin/create";

        String credentials = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());

        String response = Consumer.sendRequestAndShowResponse(createEventUri, "POST", "bearer " + this.token, requestBody);

        if (response != null && response.contains("Evento criado com sucesso")) {
            System.out.println("Evento criado com sucesso!");
            return true;
        } else {
            System.out.println("Erro ao criar o evento. Verifique os detalhes e tente novamente.");
            return false;
        }
    }

    public void deleteEvent() throws IOException {

        int idEvento = InputProtection.readInt("Inserir ID do evento que deseja eliminar: ");

        String deleteEventUri = "http://localhost:8080/event/admin/delete/" + idEvento;

        String credentials = Base64.getEncoder().encodeToString((email + ":" + password).getBytes());

        String response = Consumer.sendRequestAndShowResponse(deleteEventUri, "DELETE", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void checkCreatedEvents() throws IOException {
        String searchFilter = InputProtection.readString("Search: ", false);

        if (searchFilter != null) {
            String searchEventsAdminUri = "http://localhost:8080/event/admin/search?pesquisa=" + searchFilter;

            String response = Consumer.sendRequestAndShowResponse(searchEventsAdminUri, "GET", "bearer " + this.token, null);

            System.out.println(response);
        }
        else
        {
            String searchEventsAdminUri = "http://localhost:8080/event/admin/search/";

            String response = Consumer.sendRequestAndShowResponse(searchEventsAdminUri, "GET", "bearer " + this.token, null);

            System.out.println(response);
        }
    }

    private void generateEventCode() throws IOException {
        int idEvento = InputProtection.readInt("Evento Id: ");

        int codeExpirationTime = InputProtection.readInt("Tempo de validade do codigo (em minutos) : ");
        System.out.println("A inserir codigo de 6 digitos aleatorio na base de dados...");

        String generateEventCodeUri = "http://localhost:8080/admin/code?eventId=" + idEvento + "&codeExpirationTime=" + codeExpirationTime;

        String response = Consumer.sendRequestAndShowResponse(generateEventCodeUri, "PUT", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void checkAllPresencasInEvent() throws IOException {

        int eventId = InputProtection.readInt("Id Evento: ");

        String checkAllPresencasInEventUri = "http://localhost:8080/event/admin/presences/" + eventId;

        String response = Consumer.sendRequestAndShowResponse(checkAllPresencasInEventUri, "GET", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void adminUI() throws IOException {
        while(true){
            System.out.print("\nMenu de Administrador");

            int input = InputProtection.chooseOption("Escolher acao: ",  "Criar um evento", "Apagar um evento",  "Consultar os eventos criados", "Gerar codigo para evento", "Consultar presencas registadas num evento", "Sair");

            switch (input){
                case 1 -> {
                    createEvent();
                }
                case 2 -> {
                    deleteEvent();
                }
                case 3 -> {
                    checkCreatedEvents();
                }
                case 4 -> {
                    generateEventCode();
                }
                case 5 -> {
                    checkAllPresencasInEvent();
                }
                case 6 -> {
                    System.exit(400);
                }
                default -> {
                    System.out.println("Input inválido!");
                }
            }
        }
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