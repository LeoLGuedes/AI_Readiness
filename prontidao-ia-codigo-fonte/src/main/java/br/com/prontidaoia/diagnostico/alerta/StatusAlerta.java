package br.com.prontidaoia.diagnostico.alerta;

/** Ciclo de vida do alerta (seções 5.2 e 5.4). */
public enum StatusAlerta {

    /** Gerado no fechamento; a gestão ainda não decidiu. */
    ABERTO,

    /** A gestão salvou o escopo; o sistema acompanha o efeito. */
    EM_ACOMPANHAMENTO,

    /** A regra de encerramento foi atendida. */
    RESOLVIDO
}
