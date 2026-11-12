package br.com.prontidaoia.usuario;

/**
 * Papeis do sistema (secao 2 da especificacao), com a hierarquia de
 * departamentos:
 *
 * <pre>
 *   GESTOR (Gerente de RH)  ->  ve todos os departamentos
 *     LIDER (um por departamento)  ->  ve e cria colaboradores so no seu
 *       COLABORADOR  ->  ve apenas os proprios dados
 * </pre>
 *
 * <p>O prefixo {@code ROLE_} exigido pelo Spring Security e adicionado em
 * {@link UsuarioAutenticado}, para que o banco guarde apenas o nome do papel.</p>
 */
public enum Papel {

    /** Faz check-in e atividades; enxerga apenas os proprios dados. */
    COLABORADOR,

    /**
     * Lider de um departamento: acompanha somente o proprio time e cria
     * colaboradores apenas para ele. Nunca enxerga check-in de uma pessoa.
     */
    LIDER,

    /**
     * Gerente de RH: acompanha todos os departamentos (analise geral). O nome
     * do enum foi mantido para nao alterar os dados ja gravados.
     */
    GESTOR;

    public String authority() {
        return "ROLE_" + name();
    }

    /** Papeis que entram nas telas de {@code /gestao}. */
    public boolean acessaGestao() {
        return this == LIDER || this == GESTOR;
    }
}
