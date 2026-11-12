package br.com.prontidaoia.config;

import br.com.prontidaoia.usuario.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Coloca em todas as telas o que o layout base precisa: quem esta logado e se
 * a faixa "Ambiente de demonstracao: dados simulados" deve aparecer (secao 6).
 */
@ControllerAdvice
public class FaixaDemoAdvice {

    private final DemoProperties demo;
    private final CiclosProperties ciclos;

    public FaixaDemoAdvice(DemoProperties demo, CiclosProperties ciclos) {
        this.demo = demo;
        this.ciclos = ciclos;
    }

    @ModelAttribute("ambienteDemo")
    public boolean ambienteDemo() {
        return demo.ativo();
    }

    @ModelAttribute("totalDeCiclos")
    public int totalDeCiclos() {
        return ciclos.total();
    }

    @ModelAttribute("usuarioLogado")
    public UsuarioAutenticado usuarioLogado(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario;
    }
}
