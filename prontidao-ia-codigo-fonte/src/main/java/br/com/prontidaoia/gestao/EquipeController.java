package br.com.prontidaoia.gestao;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.gestao.consulta.EquipeQuery;
import br.com.prontidaoia.usuario.CadastroForm;
import br.com.prontidaoia.usuario.ContaColaboradorService;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Equipe do departamento: o líder cria colaboradores para o próprio
 * departamento; a gerente de RH, para qualquer um.
 *
 * <p>A regra fica no serviço ({@link ContaColaboradorService#cadastrarPelaLideranca}):
 * um líder que altere o departamento no formulário recebe a mensagem de erro e
 * nenhuma conta é criada.</p>
 */
@Controller
public class EquipeController {

    private final EquipeQuery consulta;
    private final ContaColaboradorService contas;

    public EquipeController(EquipeQuery consulta, ContaColaboradorService contas) {
        this.consulta = consulta;
        this.contas = contas;
    }

    @GetMapping("/gestao/equipe")
    public String tela(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        CadastroForm novo = new CadastroForm();
        novo.setDepartamentoId(usuario.getDepartamentoLideradoId());
        model.addAttribute("novo", novo);
        return preparar(usuario, model);
    }

    @PostMapping("/gestao/equipe")
    public String criar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                        @Valid @ModelAttribute("novo") CadastroForm novo, BindingResult erros,
                        Model model, RedirectAttributes redirect) {
        if (!erros.hasErrors()) {
            try {
                Usuario criado = contas.cadastrarPelaLideranca(usuario.getId(), novo);
                redirect.addFlashAttribute("sucesso", "Colaborador " + criado.getNome()
                        + " criado no departamento " + criado.getTime().getNome()
                        + ". A pessoa já pode entrar com o e-mail e a senha informados.");
                return "redirect:/gestao/equipe";
            } catch (RegraVioladaException ex) {
                model.addAttribute("erroCriacao", ex.getMessage());
            }
        }
        return preparar(usuario, model);
    }

    private String preparar(UsuarioAutenticado usuario, Model model) {
        model.addAttribute("menuAtivo", "equipe");
        model.addAttribute("tela", consulta.de(usuario));
        return "gestao/equipe";
    }
}
