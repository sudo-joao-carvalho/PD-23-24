package pt.isec.pd.meta2.restapi.ui;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import pt.isec.pd.meta2.restapi.consumer.Consumer;
import pt.isec.pd.meta2.restapi.models.Evento;
import pt.isec.pd.meta2.restapi.models.Utilizador;
import pt.isec.pd.meta2.restapi.utils.InputProtection;
import com.google.gson.Gson;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class ClientUI {
    private boolean isAdmin = false;
    private String email;
    private String password;
    private String token;

    public boolean loginRegister() throws IOException {
        int option = InputProtection.chooseOption("Escolher: " , "Login","Registar","Sair");

        switch (option){
            case 1 -> {
                return login();
            }
            case 2 -> {
                if (register()) {
                   return false;
                }
            }
            default -> System.exit(0);
        }

        return false;
    }

    public boolean login() throws IOException {

        this.email = InputProtection.readString("\tEmail: ", true, false);
        this.password = InputProtection.readString("\tPassword: ", true, false);

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

        //String[] split = token.split("\\s+");

        System.out.println(token);

        //this.currentUserId = Integer.parseInt(split[1]);

        return true;
    }

    public boolean register() throws IOException {
        String nome = InputProtection.readString("\tNome: ", false, false);
        String email = InputProtection.readString("\tEmail: ", true, false);
        int nif = InputProtection.readInt("\tNIF: ");
        String password = InputProtection.readString("\tPassword: ", true, false);

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
                //this.currentUserId = Integer.parseInt(response.split("\\s+")[1]);
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

        int eventCode = InputProtection.readInt("\tInsira o código do evento: ");

        // insere o código de evento num json para depois ir para o body

        String submitEventCodeUri = "http://localhost:8080/event/";

        String eventCodeAsString = String.valueOf(eventCode);

        // insere o json no request body
        String response = Consumer.sendRequestAndShowResponse(submitEventCodeUri, "POST", "bearer " + this.token, eventCodeAsString);

        System.out.println(response);

        return true;
    }

    public void isAdmin() throws IOException{
        String isAdminUri = "http://localhost:8080/isAdmin";

        String response = Consumer.sendRequestAndShowResponse(isAdminUri, "GET", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void listPresencas() throws IOException { // CONSULTAR PRESENÇAS DO USER NÃO ADMIN

        String pesquisa = InputProtection.readString("Pesquisa: ", false, true);

        String listPresencasUri = "http://localhost:8080/event/presences?pesquisa=" + pesquisa;

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

        String local = InputProtection.readString("\tLocal: ", false, false);
        String nome = InputProtection.readString("\tNome: ", false, false);
        String dia = InputProtection.readString("\tDia: ", true, false);
        String mes = InputProtection.readString("\tMês: ", true, false);
        String ano = InputProtection.readString("\tAno: ", true, false);
        String horaInicio = InputProtection.readString("Hora início (apenas hora): ", true, false);
        String minutoInicio = InputProtection.readString("Minuto da hora início (apenas minutos): ", true, false);
        String horaFim = InputProtection.readString("Hora fim (apenas hora): ", true, false);
        String minutoFim = InputProtection.readString("Minuto da hora fim (apenas minutos): ", true, false);

        Evento eventoNovo = new Evento();

        eventoNovo.setLocal(local);
        eventoNovo.setNome(nome);
        eventoNovo.setData(dia + '/' + mes + '/' + ano);
        eventoNovo.setHoraInicio(horaInicio + ':' + minutoInicio);
        eventoNovo.setHoraFim(horaFim + ':' + minutoFim);

        Gson gson = new Gson();

        String requestBody = gson.toJson(eventoNovo);

        System.out.println(requestBody);

        String createEventUri = "http://localhost:8080/event/admin";

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

        String deleteEventUri = "http://localhost:8080/event/admin/" + idEvento;

        String response = Consumer.sendRequestAndShowResponse(deleteEventUri, "DELETE", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void checkCreatedEvents() throws IOException {
        String searchFilter = InputProtection.readString("Search: ", false, true);

        if (searchFilter != null && !searchFilter.trim().isEmpty()) {
            String searchEventsAdminUri = "http://localhost:8080/event/admin?pesquisa=" + searchFilter;

            String response = Consumer.sendRequestAndShowResponse(searchEventsAdminUri, "GET", "bearer " + this.token, null);

            System.out.println(response);
        }
        else
        {
            String searchEventsAdminUri = "http://localhost:8080/event/admin?pesquisa=";

            String response = Consumer.sendRequestAndShowResponse(searchEventsAdminUri, "GET", "bearer " + this.token, null);

            System.out.println(response);
        }
    }

    private void generateEventCode() throws IOException {
        int idEvento = InputProtection.readInt("Evento Id: ");

        int codeExpirationTime = InputProtection.readInt("Tempo de validade do codigo (em minutos) : ");

        System.out.println("A inserir codigo de 6 digitos aleatorio na base de dados...");

        String generateEventCodeUri = "http://localhost:8080/event/admin/" + idEvento;

        String codeTimeInString = String.valueOf(codeExpirationTime);

        String response = Consumer.sendRequestAndShowResponse(generateEventCodeUri, "PUT", "bearer " + this.token, codeTimeInString);

        System.out.println(response);
    }

    public void checkAllPresencasInEvent() throws IOException { // CONSULTAR PRESENÇAS NUM EVENTO ADMIN

        int eventId = InputProtection.readInt("Id Evento: ");

        String checkAllPresencasInEventUri = "http://localhost:8080/event/admin/" + eventId;

        String response = Consumer.sendRequestAndShowResponse(checkAllPresencasInEventUri, "GET", "bearer " + this.token, null);

        System.out.println(response);
    }

    public void adminUI() throws IOException {
        while(true){
            System.out.print("\nMenu de Administrador");

            int input = InputProtection.chooseOption("Escolher acao: ",  "Criar um evento", "Apagar um evento",  "Consultar os eventos criados", "Gerar codigo para evento", "Consultar presencas registadas num evento", "Verificar se e admin","Sair");

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
                    isAdmin();
                }
                case 7 -> {
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
                } else {
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