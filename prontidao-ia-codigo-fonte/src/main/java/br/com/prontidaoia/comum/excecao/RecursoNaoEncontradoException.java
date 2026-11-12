package br.com.prontidaoia.comum.excecao;

/**
 * O recurso não existe ou não pertence a quem pediu.
 *
 * <p>Os dois casos respondem 404 de propósito: dizer "existe, mas não é seu"
 * já revelaria algo sobre outra pessoa ou outro time.</p>
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
