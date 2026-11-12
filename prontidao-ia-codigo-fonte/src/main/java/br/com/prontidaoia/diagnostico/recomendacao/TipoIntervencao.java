package br.com.prontidaoia.diagnostico.recomendacao;

/** Quem executa a intervenção (seção 5.4). */
public enum TipoIntervencao {

    /** Executada por pessoas; o sistema só registra a data prevista. */
    HUMANA,

    /** Executada pela plataforma, trocando a atividade liberada ao time. */
    PLATAFORMA
}
