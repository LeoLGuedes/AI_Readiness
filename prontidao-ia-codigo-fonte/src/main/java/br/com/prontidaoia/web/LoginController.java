package br.com.prontidaoia.web;

import br.com.prontidaoia.config.DemoProperties;
import br.com.prontidaoia.simulacao.PessoasDemo;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Tela única de login para os dois papéis (seção 6).
 *
 * <p>Com o perfil {@code demo} ativo, a tela lista os usuários de demonstração
 * e a senha padrão — é o que permite a quem assiste à defesa entrar como Carla
 * ou como Renata sem consultar o código.</p>
 */
@Controller
public class LoginController {

    private final DemoProperties demo;

    public LoginController(DemoProperties demo) {
        this.demo = demo;
    }

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String erro,
                        @RequestParam(required = false) String saiu,
                        @RequestParam(required = false) String criado,
                        @RequestParam(required = false) String atualizado,
                        @RequestParam(required = false) String excluido,
                        Model model) {

        model.addAttribute("erro", erro != null);
        model.addAttribute("saiu", saiu != null);
        model.addAttribute("criado", criado != null);
        model.addAttribute("atualizado", atualizado != null);
        model.addAttribute("excluido", excluido != null);

        if (demo.ativo()) {
            model.addAttribute("senhaDemo", demo.senhaPadrao());
            model.addAttribute("usuariosDemo", List.of(
                    new UsuarioDemo(PessoasDemo.GESTORA, "Gerente de RH · vê todos os departamentos",
                            PessoasDemo.email(PessoasDemo.GESTORA)),
                    new UsuarioDemo(PessoasDemo.LIDER_ATENDIMENTO, "Líder · Atendimento",
                            PessoasDemo.email(PessoasDemo.LIDER_ATENDIMENTO)),
                    new UsuarioDemo(PessoasDemo.LIDER_FINANCEIRO, "Líder · Financeiro",
                            PessoasDemo.email(PessoasDemo.LIDER_FINANCEIRO)),
                    new UsuarioDemo(PessoasDemo.LIDER_MARKETING, "Líder · Marketing",
                            PessoasDemo.email(PessoasDemo.LIDER_MARKETING)),
                    new UsuarioDemo("Carla Menezes", "Atendente · time Atendimento",
                            PessoasDemo.email(PessoasDemo.CARLA))));
        }

        return "login";
    }

    /** Uma linha da caixa "usuários de demonstração" da tela de login. */
    public record UsuarioDemo(String nome, String papel, String email) {
    }
}
