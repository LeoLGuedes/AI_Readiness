package br.com.prontidaoia.gestao;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoService;
import br.com.prontidaoia.gestao.consulta.RecomendacaoQuery;
import br.com.prontidaoia.gestao.consulta.RecomendacaoQuery.Item;
import br.com.prontidaoia.gestao.consulta.RecomendacaoQuery.TelaRecomendacao;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Quadro 3: a recomendação de um alerta e as três decisões da gestão
 * (seção 5.4): aceitar, ajustar (FA1) e adiar.
 *
 * <p>Toda rota confere o departamento do alerta ({@link AcessoDepartamento}).</p>
 */
@Controller
public class RecomendacaoController {

    static final String PREFIXO_TEXTO = "texto";

    private final RecomendacaoQuery consulta;
    private final RecomendacaoService decisoes;
    private final AcessoDepartamento acesso;

    public RecomendacaoController(RecomendacaoQuery consulta, RecomendacaoService decisoes,
                                  AcessoDepartamento acesso) {
        this.consulta = consulta;
        this.decisoes = decisoes;
        this.acesso = acesso;
    }

    @GetMapping("/gestao/alertas/{id}/recomendacao")
    public String recomendacao(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                               @RequestParam(required = false) String ajustar, Model model) {
        acesso.exigirAlerta(usuario, id);
        model.addAttribute("menuAtivo", "visao-geral");
        model.addAttribute("tela", consulta.doAlerta(id));
        model.addAttribute("ajustando", ajustar != null);
        return "gestao/recomendacao";
    }

    @PostMapping("/gestao/alertas/{id}/recomendacao/aceitar")
    public String aceitar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado gestor) {
        acesso.exigirAlerta(gestor, id);
        TelaRecomendacao tela = consulta.doAlerta(id);
        decisoes.aceitar(id, gestor.getId());
        return "redirect:/gestao/times/" + tela.timeId() + "/escopo";
    }

    /**
     * FA1: cada item com texto alterado passa a AJUSTADA; os demais são
     * aceitos como estão, e a gestão segue para o escopo (decisão D49).
     */
    @PostMapping("/gestao/alertas/{id}/recomendacao/ajustar")
    public String ajustar(@PathVariable Long id, @RequestParam Map<String, String> campos,
                          @AuthenticationPrincipal UsuarioAutenticado gestor, RedirectAttributes redirect) {
        acesso.exigirAlerta(gestor, id);
        TelaRecomendacao tela = consulta.doAlerta(id);
        try {
            for (Item item : tela.itens()) {
                String novo = campos.get(PREFIXO_TEXTO + item.id());
                if (novo != null && !novo.trim().equals(item.texto())) {
                    decisoes.ajustar(item.id(), novo, gestor.getId());
                }
            }
        } catch (RegraVioladaException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/gestao/alertas/" + id + "/recomendacao?ajustar";
        }
        decisoes.aceitar(id, gestor.getId());
        return "redirect:/gestao/times/" + tela.timeId() + "/escopo";
    }

    @PostMapping("/gestao/alertas/{id}/recomendacao/adiar")
    public String adiar(@PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado gestor,
                        RedirectAttributes redirect) {
        acesso.exigirAlerta(gestor, id);
        TelaRecomendacao tela = consulta.doAlerta(id);
        decisoes.adiar(id, gestor.getId());
        redirect.addFlashAttribute("sucesso", "Recomendação adiada.");
        return "redirect:/gestao/times/" + tela.timeId();
    }
}
