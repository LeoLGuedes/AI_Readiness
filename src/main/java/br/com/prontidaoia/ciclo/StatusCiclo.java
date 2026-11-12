package br.com.prontidaoia.ciclo;

/** Situacao de um ciclo de acompanhamento (secao 4). */
public enum StatusCiclo {

    /** Aceita check-ins e tentativas. Existe no maximo um por vez. */
    ABERTO,

    /** Ja teve os indicadores gravados; nao aceita mais respostas. */
    FECHADO
}
