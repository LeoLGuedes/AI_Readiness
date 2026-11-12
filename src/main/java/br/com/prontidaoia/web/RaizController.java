package br.com.prontidaoia.web;

import br.com.prontidaoia.usuario.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Manda cada papel para a sua tela inicial depois do login. */
@Controller
public class RaizController {

    @GetMapping("/")
    public String raiz(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario.isAcessaGestao() ? "redirect:/gestao" : "redirect:/inicio";
    }
}
