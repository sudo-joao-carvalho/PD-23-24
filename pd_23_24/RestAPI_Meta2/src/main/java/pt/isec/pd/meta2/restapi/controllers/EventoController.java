package pt.isec.pd.meta2.restapi.controllers;

import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pt.isec.pd.meta2.restapi.database.DBManager;
import pt.isec.pd.meta2.restapi.models.Evento;

import java.sql.SQLException;
import java.util.ArrayList;

@RestController
@RequestMapping("event")
public class EventoController {
    private final DBManager dbManager;

    @Autowired
    public EventoController(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @GetMapping("/") // event/
    public String Index() {
        return "Bem-vindo à API Rest do Trabalho de Programação Distribuída 2023/2024! Está nos Eventos.";
    }

    @GetMapping("/admin") // event/admin GET -> tem que ser admin // localhost:8080/event/admin?pesquisa=
    public ResponseEntity<String> getCreatedEventsFiltered(@RequestParam(required = false) String pesquisa) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de consulta de eventos criados").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        return ResponseEntity.ok().body("Resultado: \n" + dbManager.checkCreatedEvents(pesquisa));
    }

    @PostMapping("/admin") // /event/admin -> POST -> é para criar evento
    public ResponseEntity<String> createEvento(@RequestBody Evento evento) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de criar evento").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        if (evento == null) {
            return ResponseEntity.badRequest().header("CreateEvento", "Não foi possível criar").body("Por favor insira todos os detalhes necessários.");
        }

        ArrayList<String> paramsToInsert = new ArrayList<>();
        paramsToInsert.add(evento.getLocal());
        paramsToInsert.add(evento.getNome());
        paramsToInsert.add(evento.getData());
        paramsToInsert.add(evento.getHoraInicio());
        paramsToInsert.add(evento.getHoraFim());

        try {
            if (dbManager.insertEvent(paramsToInsert)) { // INSERT INTO Evento VALUES (valores)
                return ResponseEntity.ok().body("Evento criado com sucesso!");
            }
        }
        catch (SQLException e) {
            return ResponseEntity.badRequest().header("CreateEvento", "Impossível realizar o pedido, reveja o que inseriu.").body("Não foi possível realizar o pedido à base de dados.");
        }

        return ResponseEntity.badRequest().body("Evento não pode ser criado. Verifique o que inseriu.\n");
    }

    @DeleteMapping("/admin/{eventId}") // ?
    public ResponseEntity<String> deleteEvent(@PathVariable(value = "eventId") int eventId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de criar evento").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        try {
            if (dbManager.deleteEvent(eventId)) {
                return ResponseEntity.ok().body("Evento eliminado com sucesso.\n");
            }
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().header("DeleteEvento", "Inseriu um número inválido.").body("Número inválido. Tente outra vez.");
        }

        return ResponseEntity.badRequest().body("Evento não pode ser eliminado.\n");
    }

    @GetMapping("admin/{eventId}") // admin/10 -> GET
    public ResponseEntity<String> getPresencasInEvent(@PathVariable(value = "eventId") Integer eventId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de criar evento").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        return ResponseEntity.ok().body(dbManager.checkAllRegisteredPresences(eventId)); //
    }

    @PutMapping("/admin/{eventId}") ///event/admin/10 -> PUT EDITAR
    public ResponseEntity<String> addCodeToEvent(@PathVariable(value = "eventId") Integer eventId, @RequestBody int codeExpirationTime) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de criar evento").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        if (dbManager.addCodeToEvent(eventId, codeExpirationTime) == 0 || dbManager.addCodeToEvent(eventId, codeExpirationTime) == -2) {
            System.out.println("Estou aqui1");
            return ResponseEntity.badRequest().body("Não foi possível adicionar código ao evento " + eventId);
        }

        else if (dbManager.addCodeToEvent(eventId, codeExpirationTime) == -3) {
            System.out.println("estou aqui 2");
            return ResponseEntity.badRequest().body("Não foi possível encontrar o evento com o ID especificado.");
        }
        return ResponseEntity.ok().body("Código adicionado com sucesso ao evento " + eventId + "\n");
    }

    @PostMapping("/") // /event/ -> POST //evento/129820 -> INFO SENSIVEL
    public ResponseEntity<String> submitEventCode(Authentication authentication, @RequestBody int eventCode) {
        String subject = authentication.getName(); // vai buscar o email

        boolean success = dbManager.checkEventCodeAndInsertUser(eventCode, subject);

        if (success) {
            return ResponseEntity.ok().body("Código inserido com sucesso. Está com a presença registada neste evento.\n");
        }
        return ResponseEntity.badRequest().body("Não foi possível registá-lo no evento. O código está errado/evento não existe.");
    }

    @GetMapping("/presences") // @GetMapping("/") // event/presences?pesquisa=
    public ResponseEntity<String> listAllUserPresences(Authentication authentication, @RequestParam String pesquisa) {

        String subject = authentication.getName();

        String response = dbManager.listAllUserPresencas(subject, pesquisa);

        if (response.isEmpty()) {
            return ResponseEntity.badRequest().header("ListPresencas", "Erro ao encontrar presenças").body("Erro ao listar as presenças do utilizador. Tem a certeza que está em algum evento?");
        }

        return ResponseEntity.ok().body(response);
    }
}