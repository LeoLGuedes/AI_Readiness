package br.com.prontidaoia.colaborador;

import br.com.prontidaoia.usuario.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tela inicial da colaboradora: ciclo atual, pendências e progresso (seção 6). */
@Controller
public class InicioController {

    private final SemanaDoColaboradorService semanas;

    public InicioController(SemanaDoColaboradorService semanas) {
        this.semanas = semanas;
    }

    @GetMapping("/inicio")
    public String inicio(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("menuAtivo", "inicio");
        model.addAttribute("semana", semanas.daPessoa(usuario));
        return "colaborador/inicio";
    }
}
