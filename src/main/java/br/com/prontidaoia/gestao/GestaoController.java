package br.com.prontidaoia.gestao;

import br.com.prontidaoia.gestao.consulta.DetalheTimeQuery;
import br.com.prontidaoia.gestao.consulta.VisaoGeralQuery;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Quadro 1 (visão geral dos times) e Quadro 2 (detalhe do time).
 *
 * <p>A gerente de RH vê todos os departamentos; o líder vê apenas o seu
 * ({@link AcessoDepartamento}).</p>
 */
@Controller
public class GestaoController {

    private final VisaoGeralQuery visaoGeral;
    private final DetalheTimeQuery detalhe;
    private final AcessoDepartamento acesso;

    public GestaoController(VisaoGeralQuery visaoGeral, DetalheTimeQuery detalhe, AcessoDepartamento acesso) {
        this.visaoGeral = visaoGeral;
        this.detalhe = detalhe;
        this.acesso = acesso;
    }

    @GetMapping("/gestao")
    public String visaoGeral(@AuthenticationPrincipal UsuarioAutenticado usuario,
                             @RequestParam(required = false) Integer ciclo, Model model) {
        model.addAttribute("menuAtivo", "visao-geral");
        model.addAttribute("visao", visaoGeral.doCiclo(ciclo,
                acesso.veTodos(usuario) ? null : acesso.idsVisiveis(usuario)));
        return "gestao/visao-geral";
    }

    @GetMapping("/gestao/times/{id}")
    public String detalheDoTime(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                                @RequestParam(required = false) Integer ate, Model model) {
        acesso.exigirTime(usuario, id);
        model.addAttribute("menuAtivo", "visao-geral");
        model.addAttribute("d", detalhe.doTime(id, ate));
        return "gestao/detalhe-time";
    }
}
