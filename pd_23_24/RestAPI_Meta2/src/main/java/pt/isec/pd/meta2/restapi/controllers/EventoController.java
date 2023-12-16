package pt.isec.pd.meta2.restapi.controllers;

import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.parameters.P;
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

    @GetMapping("/")
    public String Index() {
        return "Bem-vindo à API Rest do Trabalho de Programação Distribuída 2023/2024! Está nos Eventos.";
    }

    @GetMapping("/admin/search")
    public ResponseEntity<String> getCreatedEventsFiltered(@RequestParam(required = false) String pesquisa) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de consulta de eventos criados").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        return ResponseEntity.ok().body("Resultado: \n" + dbManager.checkCreatedEvents(pesquisa));
    }

    @PostMapping("/admin/create")
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
            if (dbManager.insertEvent(paramsToInsert)) {
                return ResponseEntity.ok().body("Evento criado com sucesso!");
            }
        }
        catch (SQLException e) {
            return ResponseEntity.badRequest().header("CreateEvento", "Impossível realizar o pedido, reveja o que inseriu.").body("Não foi possível realizar o pedido à base de dados.");
        }

        return ResponseEntity.badRequest().body("Evento não pode ser criado. Verifique o que inseriu.\n");
    }

    @DeleteMapping("/admin/delete/{eventId}")
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

    @GetMapping("admin/presences/{eventId}")
    public ResponseEntity<String> getPresencasInEvent(@PathVariable(value = "eventId") Integer eventId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de criar evento").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        return ResponseEntity.ok().body(dbManager.checkAllRegisteredPresences(eventId));
    }

    @PutMapping("/admin/code")
    public ResponseEntity<String> addCodeToEvent(@RequestParam Integer eventId, @RequestParam Integer codeExpirationTime) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean admin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (!admin) {
            return ResponseEntity.badRequest().header("AdminAuth", "Tem de ser admin para realizar o pedido de criar evento").body("Pedido não realizado. Por favor, dê login como admin.");
        }

        if (dbManager.addCodeToEvent(eventId, codeExpirationTime) == 0 || dbManager.addCodeToEvent(eventId, codeExpirationTime) == -2) {
            return ResponseEntity.badRequest().body("Não foi possível adicionar código ao evento " + eventId);
        }

        else if (dbManager.addCodeToEvent(eventId, codeExpirationTime) == -3) {
            return ResponseEntity.badRequest().body("Não foi possível encontrar o evento com o ID especificado.");
        }
        return ResponseEntity.ok().body("Código adicionado com sucesso ao evento " + eventId + "\n");
    }

    @PutMapping("/submit")
    public ResponseEntity<String> submitEventCode(@RequestParam int userId, @RequestParam int eventCode) {
        boolean success = dbManager.checkEventCodeAndInsertUser(eventCode, userId);

        if (success) {
            return ResponseEntity.ok("Código inserido com sucesso. Está com a presença registada neste evento.\n");
        }

        return ResponseEntity.badRequest().body("Não foi possível registá-lo no evento. O código está errado/evento não existe.");
    }

    @GetMapping("/list")
    public ResponseEntity<String> listAllUserPresences(@RequestParam Integer userId, @RequestParam String pesquisa) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isUser = auth.getAuthorities().toString().equals("SCOPE_User");

        if (!isUser) {
            return ResponseEntity.badRequest().header("ListPresencas", "Não está autenticado como utilizador.").body("Autentique-se como User para ver as suas presenças");
        }
        String response = dbManager.listAllUserPresencas(userId, pesquisa);

        if (response.isEmpty()) {
            return ResponseEntity.badRequest().header("ListPresencas", "Erro ao encontrar presenças").body("Erro ao listar as presenças do utilizador");
        }

        return ResponseEntity.ok().body(response);
    }
}