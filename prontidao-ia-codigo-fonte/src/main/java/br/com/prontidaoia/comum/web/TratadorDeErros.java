package br.com.prontidaoia.comum.web;

import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Converte as exceções do domínio em telas de erro.
 *
 * <p>Os controllers tratam localmente as regras que viram mensagem no próprio
 * formulário; o que chega aqui é o caso que não tem tela melhor para mostrar.</p>
 */
@ControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String naoEncontrado(Model model) {
        model.addAttribute("status", 404);
        return "error";
    }

    /** Líder pedindo um time, alerta ou recomendação de outro departamento. */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String acessoNegado(AccessDeniedException e, Model model) {
        model.addAttribute("status", 403);
        model.addAttribute("mensagem", e.getMessage());
        return "error";
    }

    @ExceptionHandler(RegraVioladaException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String regraViolada(RegraVioladaException e, Model model) {
        model.addAttribute("status", 409);
        model.addAttribute("mensagem", e.getMessage());
        return "error";
    }
}
