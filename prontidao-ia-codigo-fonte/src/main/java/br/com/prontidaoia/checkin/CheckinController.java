package br.com.prontidaoia.checkin;

import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
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
 * Formulário de check-in (seção 6, {@code /checkin}).
 *
 * <p>O id de quem responde vem sempre da sessão, nunca do formulário.</p>
 */
@Controller
public class CheckinController {

    private final CheckinService checkins;
    private final DificuldadeRepository dificuldades;
    private final CicloService ciclos;

    public CheckinController(CheckinService checkins, DificuldadeRepository dificuldades, CicloService ciclos) {
        this.checkins = checkins;
        this.dificuldades = dificuldades;
        this.ciclos = ciclos;
    }

    @GetMapping("/checkin")
    public String formulario(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("form", new CheckinForm());
        return tela(usuario, model);
    }

    @PostMapping("/checkin")
    public String enviar(@AuthenticationPrincipal UsuarioAutenticado usuario,
                         @Valid @ModelAttribute("form") CheckinForm form,
                         BindingResult erros,
                         Model model,
                         RedirectAttributes redirect) {
        if (erros.hasErrors()) {
            return tela(usuario, model);
        }
        try {
            checkins.registrar(usuario.getId(), form.paraResposta());
        } catch (RegraVioladaException e) {
            model.addAttribute("erro", e.getMessage());
            return tela(usuario, model);
        }
        redirect.addFlashAttribute("sucesso", "Check-in enviado. Obrigado por responder!");
        return "redirect:/inicio";
    }

    private String tela(UsuarioAutenticado usuario, Model model) {
        model.addAttribute("menuAtivo", "checkin");
        model.addAttribute("ciclo", ciclos.cicloAberto().orElse(null));
        model.addAttribute("jaRespondido", checkins.doCicloAberto(usuario.getId()).isPresent());
        model.addAttribute("catalogo", dificuldades.findAllByOrderById());
        return "colaborador/checkin";
    }
}
