package br.com.prontidaoia.colaborador;

import br.com.prontidaoia.usuario.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Histórico pessoal. A rota não aceita parâmetro de pessoa: o id vem sempre
 * da sessão, então ninguém consegue ver o histórico de outra pessoa mudando a
 * URL.
 */
@Controller
public class HistoricoController {

    private final HistoricoPessoalService historico;

    public HistoricoController(HistoricoPessoalService historico) {
        this.historico = historico;
    }

    @GetMapping("/meu-historico")
    public String meuHistorico(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("menuAtivo", "historico");
        model.addAttribute("linhas", historico.daPessoa(usuario.getId()));
        return "colaborador/historico";
    }
}
