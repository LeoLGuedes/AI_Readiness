package br.com.prontidaoia.gestao;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.diagnostico.escopo.EscopoService;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoService;
import br.com.prontidaoia.gestao.consulta.EscopoQuery;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Tela de escopo (seção 5.4): salvar o escopo e agendar as intervenções
 * humanas. "Salvar escopo" gera a confirmação "Escopo salvo" (seção 7: a
 * mesma palavra para a mesma ação).
 *
 * <p>Toda rota confere o departamento ({@link AcessoDepartamento}): o líder
 * só decide sobre o escopo do próprio time.</p>
 */
@Controller
public class EscopoController {

    private final EscopoQuery consulta;
    private final EscopoService escopo;
    private final RecomendacaoService decisoes;
    private final AcessoDepartamento acesso;

    public EscopoController(EscopoQuery consulta, EscopoService escopo, RecomendacaoService decisoes,
                            AcessoDepartamento acesso) {
        this.consulta = consulta;
        this.escopo = escopo;
        this.decisoes = decisoes;
        this.acesso = acesso;
    }

    @GetMapping("/gestao/times/{id}/escopo")
    public String tela(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id, Model model) {
        acesso.exigirTime(usuario, id);
        model.addAttribute("menuAtivo", "visao-geral");
        model.addAttribute("tela", consulta.doTime(id));
        return "gestao/escopo";
    }

    @PostMapping("/gestao/times/{id}/escopo")
    public String salvar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                         @PathVariable Long id, @RequestParam Long alertaId,
                         @RequestParam(defaultValue = "false") boolean aplicarTroca,
                         RedirectAttributes redirect) {
        acesso.exigirTime(usuario, id);
        // O alerta enviado no formulário precisa ser do mesmo time da URL.
        if (!id.equals(acesso.exigirAlerta(usuario, alertaId))) {
            throw new AccessDeniedException("O alerta não pertence a este departamento.");
        }
        try {
            EscopoService.EscopoSalvo salvo = escopo.salvar(alertaId, aplicarTroca);
            redirect.addFlashAttribute("sucesso", salvo.noCicloAberto()
                    ? "Escopo salvo. O alerta está em acompanhamento."
                    : "Escopo salvo. A troca de atividade vale a partir do ciclo " + salvo.cicloDeAplicacao()
                            + ", porque o time já começou o ciclo atual. O alerta está em acompanhamento.");
        } catch (RegraVioladaException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
            return "redirect:/gestao/times/" + id + "/escopo";
        }
        return "redirect:/gestao/times/" + id;
    }

    @PostMapping("/gestao/recomendacoes/{id}/agendar")
    public String agendar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id,
                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                          RedirectAttributes redirect) {
        Long timeId = acesso.exigirRecomendacao(usuario, id);
        try {
            decisoes.agendar(id, data);
            redirect.addFlashAttribute("sucesso", "Intervenção agendada.");
        } catch (RegraVioladaException e) {
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/gestao/times/" + timeId + "/escopo";
    }
}
