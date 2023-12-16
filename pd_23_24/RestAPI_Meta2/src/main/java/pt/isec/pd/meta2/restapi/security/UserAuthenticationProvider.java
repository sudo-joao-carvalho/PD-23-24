package pt.isec.pd.meta2.restapi.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import pt.isec.pd.meta2.restapi.database.DBManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
//@RequestMapping("utilizador")
public class UserAuthenticationProvider implements AuthenticationProvider
{
    private final DBManager dbManager;

    @Autowired
    public UserAuthenticationProvider(DBManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException
    {
        String email = authentication.getName();
        String password = authentication.getCredentials().toString();

        ArrayList<String> loginParams = new ArrayList<>();

        loginParams.add(email);
        loginParams.add(password);

        int[] returnValue = dbManager.verifyLogin(loginParams);

        if (returnValue == null) { // se ele devolve null, user não existe.
            return null;
        }
        // se ele não devolve null, user existe, prosseguir:

        System.out.println(Arrays.toString(returnValue));

        List<GrantedAuthority> authorities = new ArrayList<>();

        if (returnValue[1] == 1) { // significa que é admin
            authorities.add(new SimpleGrantedAuthority("Admin"));
        }

        else {
            authorities.add(new SimpleGrantedAuthority("User"));
        }

        return new UsernamePasswordAuthenticationToken(email, password, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication)
    {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
