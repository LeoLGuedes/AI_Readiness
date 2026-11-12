package br.com.prontidaoia.diagnostico.recomendacao;

/** Decisão da gestão sobre cada item recomendado (seção 5.4). */
public enum StatusRecomendacao {

    /** Gerada no fechamento; aguarda decisão. */
    SUGERIDA,

    /** "Aceitar e ajustar escopo". */
    ACEITA,

    /** Fluxo alternativo FA1: texto editado pela gestão. */
    AJUSTADA,

    /** "Adiar". */
    ADIADA
}
