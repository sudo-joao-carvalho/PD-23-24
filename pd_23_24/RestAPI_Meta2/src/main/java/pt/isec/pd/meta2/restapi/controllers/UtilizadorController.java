package pt.isec.pd.meta2.restapi.controllers;

import org.apache.el.parser.Token;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import pt.isec.pd.meta2.restapi.database.DBManager;
import pt.isec.pd.meta2.restapi.models.Utilizador;
import pt.isec.pd.meta2.restapi.security.TokenService;

import java.util.ArrayList;

@RestController
public class UtilizadorController {
    private final DBManager dbManager;
    private final TokenService tokenService;
    @Autowired
    public UtilizadorController(DBManager dbManager, TokenService tokenService) {
        this.dbManager = dbManager;
        this.tokenService = tokenService;
    }
    @PostMapping("/login")
    public String login(Authentication authentication)
    {
        return tokenService.generateToken(authentication);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Utilizador utilizador) {
        if (utilizador == null) {
            return ResponseEntity.badRequest().header("RegistarUtilizador", "Por favor, insira todos os detalhes.").body("Utilizador não inserido.\n");
        }

        ArrayList<String> params = new ArrayList<>();
        params.add(utilizador.getNome());
        params.add(utilizador.getEmail());
        params.add(String.valueOf(utilizador.getNif()));
        params.add(utilizador.getPassword());
        params.add("0");
        params.add("0");

        int returnValue = dbManager.insertUser(params);

        if (returnValue == 0) {
            return ResponseEntity.badRequest().header("RegistarUtilizador", "Não foi possível registar o utilizador.").body("Utilizador não inserido. Já existe um com esse email.\n");
        }

        utilizador.setId(returnValue);

        return ResponseEntity.ok().body("Utilizador " + utilizador.getId() + " registado com sucesso!");
    }

    @GetMapping("/isAdmin")
    public String getIsAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isAdmin = auth.getAuthorities().stream().anyMatch(r -> r.getAuthority().equals("SCOPE_Admin"));

        if (isAdmin) {
            return "Este utilizador possui privilégios de administrador.\n";
        }

        return "Este utilizador não possui privilégios de administrador.\n";
    }


}