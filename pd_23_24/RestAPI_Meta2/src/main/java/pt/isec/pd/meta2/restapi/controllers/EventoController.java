package pt.isec.pd.meta2.restapi.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.isec.pd.meta2.restapi.models.DBManager;

import java.sql.SQLException;
import java.util.ArrayList;

@RestController
@RequestMapping("Evento")
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

    @GetMapping("/searchEvento")
    public ResponseEntity<String> getCreatedEventsFiltered(@RequestParam(required = false) String pesquisa) {
        DBManager dbManager = new DBManager();
        return ResponseEntity.ok().body("Resultado: \n" + dbManager.checkCreatedEvents(pesquisa));
    }

    @PostMapping("/createEvento")
    public ResponseEntity<String> createEvento(String local, String nome, String data, String horaInicio, String horaFim) {
        DBManager dbManager = new DBManager();
        ArrayList<String> paramsToInsert = new ArrayList<>();
        paramsToInsert.add(local);
        paramsToInsert.add(nome);
        paramsToInsert.add(data);
        paramsToInsert.add(horaInicio);
        paramsToInsert.add(horaFim);
        if (dbManager.insertEvent(paramsToInsert)) {
            return ResponseEntity.ok().body("Evento criado com sucesso!");
        }
        return ResponseEntity.badRequest().body("Evento não pode ser criado. Verifique o que inseriu.\n");
    }

    // mudar se isto não der para não ter o id no url
    @DeleteMapping("/deleteEvento/{eventId}")
    public ResponseEntity<String> deleteEvent(@PathVariable(value = "eventId") int eventId) throws SQLException {
        if (dbManager.deleteEvent(eventId)) {
            return ResponseEntity.ok().body("Evento eliminado com sucesso.\n");
        }
        return ResponseEntity.badRequest().body("Evento não pode ser eliminado.\n");
    }

    @PutMapping("/addCodeEvento/")
    public ResponseEntity<String> addCodeToEvent(int eventId, int codeExpirationTime) {
        if (dbManager.addCodeToEvent(eventId, codeExpirationTime) == 0 || dbManager.addCodeToEvent(eventId, codeExpirationTime) == -2) {
            return ResponseEntity.badRequest().body("Não foi possível adicionar código ao evento " + eventId);
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
}