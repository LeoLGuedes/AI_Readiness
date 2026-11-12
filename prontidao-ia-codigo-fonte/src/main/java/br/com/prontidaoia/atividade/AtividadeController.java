package br.com.prontidaoia.atividade;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Execução e resultado de uma atividade (seção 6).
 *
 * <p>O formulário envia {@code q<idQuestao>} com a alternativa escolhida e
 * {@code t<idQuestao>} com os segundos gastos na questão.</p>
 */
@Controller
public class AtividadeController {

    static final String PREFIXO_ESCOLHA = "q";
    static final String PREFIXO_TEMPO = "t";

    private final ExecucaoAtividadeService execucao;

    public AtividadeController(ExecucaoAtividadeService execucao) {
        this.execucao = execucao;
    }

    @GetMapping("/atividades/{id}")
    public String abrir(@AuthenticationPrincipal UsuarioAutenticado usuario,
                        @PathVariable Long id,
                        Model model) {
        if (execucao.jaFinalizou(usuario, id)) {
            return "redirect:/atividades/" + id + "/resultado";
        }
        return tela(usuario, id, Map.of(), model);
    }

    @PostMapping("/atividades/{id}/finalizar")
    public String finalizar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                            @PathVariable Long id,
                            @RequestParam Map<String, String> campos,
                            Model model) {
        Map<Long, Integer> escolhas = porQuestao(campos, PREFIXO_ESCOLHA);
        try {
            execucao.finalizar(usuario, id, escolhas, porQuestao(campos, PREFIXO_TEMPO));
        } catch (RegraVioladaException e) {
            if (execucao.jaFinalizou(usuario, id)) {
                return "redirect:/atividades/" + id + "/resultado";
            }
            model.addAttribute("erro", e.getMessage());
            return tela(usuario, id, escolhas, model);
        }
        return "redirect:/atividades/" + id + "/resultado";
    }

    @GetMapping("/atividades/{id}/resultado")
    public String resultado(@AuthenticationPrincipal UsuarioAutenticado usuario,
                            @PathVariable Long id,
                            Model model) {
        if (!execucao.jaFinalizou(usuario, id)) {
            return "redirect:/atividades/" + id;
        }
        model.addAttribute("menuAtivo", "inicio");
        model.addAttribute("resultado", execucao.resultado(usuario, id));
        return "colaborador/resultado";
    }

    private String tela(UsuarioAutenticado usuario, Long id, Map<Long, Integer> escolhas, Model model) {
        model.addAttribute("menuAtivo", "inicio");
        model.addAttribute("atividade", execucao.abrir(usuario, id));
        model.addAttribute("escolhas", escolhas);
        return "colaborador/atividade";
    }

    /** Lê os campos {@code <prefixo><idQuestao>} que vieram com número válido. */
    static Map<Long, Integer> porQuestao(Map<String, String> campos, String prefixo) {
        Map<Long, Integer> valores = new HashMap<>();
        campos.forEach((nome, valor) -> {
            if (nome.startsWith(prefixo) && nome.length() > prefixo.length()) {
                try {
                    valores.put(Long.valueOf(nome.substring(prefixo.length())), Integer.valueOf(valor.trim()));
                } catch (NumberFormatException ignorado) {
                    // campo vazio ou alterado à mão: tratado como questão em branco
                }
            }
        });
        return valores;
    }
}
