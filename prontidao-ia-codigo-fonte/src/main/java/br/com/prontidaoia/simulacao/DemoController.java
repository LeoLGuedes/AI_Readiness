package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.diagnostico.fechamento.FechamentoCicloService;
import br.com.prontidaoia.diagnostico.fechamento.ResumoDoFechamento;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Painel de demonstração (seção 6). Só existe com o perfil {@code demo}; fora
 * dele, {@code /demo} responde 404 (decisão D19).
 */
@Controller
@Profile("demo")
public class DemoController {

    private final PainelDemo painel;
    private final CenarioInicial cenario;
    private final PreenchimentoCarla carla;
    private final GeradorDeterministico gerador;
    private final FechamentoCicloService fechamento;
    private final RegistroAcoesDemo registro;

    public DemoController(PainelDemo painel, CenarioInicial cenario, PreenchimentoCarla carla,
                          GeradorDeterministico gerador, FechamentoCicloService fechamento,
                          RegistroAcoesDemo registro) {
        this.painel = painel;
        this.cenario = cenario;
        this.carla = carla;
        this.gerador = gerador;
        this.fechamento = fechamento;
        this.registro = registro;
    }

    @GetMapping("/demo")
    public String painel(Model model) {
        model.addAttribute("menuAtivo", "demo");
        model.addAttribute("situacao", painel.situacao());
        model.addAttribute("acoes", registro.recentes());
        return "demo/painel";
    }

    @PostMapping("/demo/restaurar")
    public String restaurar(RedirectAttributes redirect) {
        return executar("Restaurar cenário inicial", redirect, () -> {
            cenario.restaurar();
            return List.of("Ciclos 1 a 3 fechados; ciclo 4 aberto sem as respostas da Carla.");
        });
    }

    @PostMapping("/demo/carla")
    public String preencherCarla(RedirectAttributes redirect) {
        return executar("Preencher pendências da Carla conforme o roteiro", redirect, () -> {
            int n = carla.preencher();
            return List.of(n == 0 ? "A Carla não tinha pendências." : n + " pendência(s) preenchida(s).");
        });
    }

    @PostMapping("/demo/gerar-respostas")
    public String gerarRespostas(RedirectAttributes redirect) {
        return executar("Gerar respostas simuladas do ciclo aberto", redirect,
                () -> List.of(descrever(gerador.gerarCicloAberto())));
    }

    @PostMapping("/demo/fechar-ciclo")
    public String fecharCiclo(RedirectAttributes redirect) {
        return executar("Fechar ciclo aberto", redirect, () -> descrever(fechamento.fecharCicloAberto()));
    }

    @PostMapping("/demo/avancar-ciclo")
    public String avancarCiclo(RedirectAttributes redirect) {
        return executar("Avançar um ciclo", redirect, () -> {
            List<String> linhas = new ArrayList<>();
            linhas.add(descrever(gerador.gerarCicloAberto()));
            linhas.addAll(descrever(fechamento.fecharCicloAberto()));
            return linhas;
        });
    }

    private String executar(String titulo, RedirectAttributes redirect, Supplier<List<String>> acao) {
        try {
            List<String> detalhes = acao.get();
            registro.registrar(titulo, detalhes);
            redirect.addFlashAttribute("sucesso", titulo + ": feito.");
        } catch (RegraVioladaException e) {
            registro.registrar(titulo + " (não executado)", List.of(e.getMessage()));
            redirect.addFlashAttribute("erro", e.getMessage());
        }
        return "redirect:/demo";
    }

    private static String descrever(GeradorDeterministico.Geracao g) {
        return "Ciclo " + g.ciclo() + ": " + g.checkins() + " check-in(s) e " + g.atividades()
                + " atividade(s) geradas.";
    }

    private static List<String> descrever(ResumoDoFechamento r) {
        List<String> linhas = new ArrayList<>();
        linhas.add("Ciclo " + r.cicloFechado() + " fechado."
                + (r.cicloAberto() != null ? " Ciclo " + r.cicloAberto() + " aberto." : " Acompanhamento encerrado."));
        r.alertasNovos().forEach(a -> linhas.add("Alerta novo: " + a));
        r.alertasAtualizados().forEach(a -> linhas.add("Alerta mantido: " + a));
        r.alertasResolvidos().forEach(a -> linhas.add("Alerta resolvido: " + a));
        return linhas;
    }
}
