package br.com.prontidaoia.gestao;

import br.com.prontidaoia.gestao.consulta.DetalheTimeQuery;
import br.com.prontidaoia.gestao.consulta.VisaoGeralQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Quadro 1 (visão geral dos times) e Quadro 2 (detalhe do time). */
@Controller
public class GestaoController {

    private final VisaoGeralQuery visaoGeral;
    private final DetalheTimeQuery detalhe;

    public GestaoController(VisaoGeralQuery visaoGeral, DetalheTimeQuery detalhe) {
        this.visaoGeral = visaoGeral;
        this.detalhe = detalhe;
    }

    @GetMapping("/gestao")
    public String visaoGeral(@RequestParam(required = false) Integer ciclo, Model model) {
        model.addAttribute("menuAtivo", "visao-geral");
        model.addAttribute("visao", visaoGeral.doCiclo(ciclo));
        return "gestao/visao-geral";
    }

    @GetMapping("/gestao/times/{id}")
    public String detalheDoTime(@PathVariable Long id, @RequestParam(required = false) Integer ate, Model model) {
        model.addAttribute("menuAtivo", "visao-geral");
        model.addAttribute("d", detalhe.doTime(id, ate));
        return "gestao/detalhe-time";
    }
}
