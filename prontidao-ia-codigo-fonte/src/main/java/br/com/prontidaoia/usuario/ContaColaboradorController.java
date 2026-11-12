package br.com.prontidaoia.usuario;

import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ContaColaboradorController {
    private final ContaColaboradorService contas;

    public ContaColaboradorController(ContaColaboradorService contas) {
        this.contas = contas;
    }

    @GetMapping("/cadastro")
    public String formularioCadastro(Model model) {
        model.addAttribute("cadastro", new CadastroForm());
        return prepararCadastro(model);
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("cadastro") CadastroForm cadastro,
                            BindingResult erros, Model model) {
        if (!erros.hasErrors()) {
            try {
                contas.cadastrar(cadastro);
                return "redirect:/login?criado";
            } catch (RegraVioladaException ex) {
                model.addAttribute("erroCadastro", ex.getMessage());
            }
        }
        return prepararCadastro(model);
    }

    @GetMapping("/perfil")
    public String perfil(@AuthenticationPrincipal UsuarioAutenticado logado, Model model) {
        ContaColaboradorService.DadosPerfil dados = contas.perfil(logado.getId());
        PerfilForm formulario = new PerfilForm();
        formulario.setNome(dados.nome());
        formulario.setEmail(dados.email());
        formulario.setDepartamentoId(dados.departamentoId());
        model.addAttribute("perfil", formulario);
        return prepararPerfil(model, dados);
    }

    @PostMapping("/perfil")
    public String atualizar(@AuthenticationPrincipal UsuarioAutenticado logado,
                            @Valid @ModelAttribute("perfil") PerfilForm perfil,
                            BindingResult erros, Model model,
                            HttpServletRequest request, HttpServletResponse response,
                            Authentication authentication) {
        ContaColaboradorService.DadosPerfil dados = contas.perfil(logado.getId());
        if (!erros.hasErrors()) {
            try {
                contas.atualizar(logado.getId(), perfil);
                encerrarSessao(request, response, authentication);
                return "redirect:/login?atualizado";
            } catch (RegraVioladaException ex) {
                model.addAttribute("erroPerfil", ex.getMessage());
            }
        }
        return prepararPerfil(model, dados);
    }

    @PostMapping("/perfil/excluir")
    public String excluir(@AuthenticationPrincipal UsuarioAutenticado logado,
                          @RequestParam(required = false) String senhaExclusao,
                          @RequestParam(required = false) String confirmacaoExclusao,
                          Model model, HttpServletRequest request,
                          HttpServletResponse response, Authentication authentication) {
        try {
            contas.excluir(logado.getId(), senhaExclusao, confirmacaoExclusao);
            encerrarSessao(request, response, authentication);
            return "redirect:/login?excluido";
        } catch (RegraVioladaException ex) {
            ContaColaboradorService.DadosPerfil dados = contas.perfil(logado.getId());
            PerfilForm formulario = new PerfilForm();
            formulario.setNome(dados.nome());
            formulario.setEmail(dados.email());
            formulario.setDepartamentoId(dados.departamentoId());
            model.addAttribute("perfil", formulario);
            model.addAttribute("erroExclusao", ex.getMessage());
            return prepararPerfil(model, dados);
        }
    }

    private String prepararCadastro(Model model) {
        model.addAttribute("departamentos", contas.departamentos());
        return "cadastro";
    }

    private String prepararPerfil(Model model, ContaColaboradorService.DadosPerfil dados) {
        model.addAttribute("dados", dados);
        model.addAttribute("departamentos", contas.departamentos());
        model.addAttribute("menuAtivo", "perfil");
        return "colaborador/perfil";
    }

    private void encerrarSessao(HttpServletRequest request, HttpServletResponse response,
                               Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }
}
