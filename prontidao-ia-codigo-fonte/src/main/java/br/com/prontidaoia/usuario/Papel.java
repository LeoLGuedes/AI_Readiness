package br.com.prontidaoia.usuario;

/**
 * Papeis do sistema (secao 2 da especificacao).
 *
 * <p>O prefixo {@code ROLE_} exigido pelo Spring Security e adicionado em
 * {@link UsuarioAutenticado}, para que o banco guarde apenas o nome do papel.</p>
 */
public enum Papel {

    /** Faz check-in e atividades; enxerga apenas os proprios dados. */
    COLABORADOR,

    /** Acompanha os times; nunca enxerga check-in de uma pessoa. */
    GESTOR;

    public String authority() {
        return "ROLE_" + name();
    }
}
