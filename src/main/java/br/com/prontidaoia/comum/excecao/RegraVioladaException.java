package br.com.prontidaoia.comum.excecao;

/**
 * Uma regra de negócio impediu a operação: check-in repetido, atividade
 * finalizada com questão em branco, ciclo já fechado.
 *
 * <p>A mensagem é escrita para a pessoa que está usando o sistema, porque as
 * telas a exibem como está.</p>
 */
public class RegraVioladaException extends RuntimeException {

    public RegraVioladaException(String mensagem) {
        super(mensagem);
    }
}
