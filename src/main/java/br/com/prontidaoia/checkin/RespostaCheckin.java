package br.com.prontidaoia.checkin;

import java.util.List;

/**
 * O que a pessoa respondeu no formulário de check-in.
 *
 * @param confianca    pergunta 1, de 1 a 5
 * @param receio       pergunta 2, de 1 a 5
 * @param dificuldades ids do catálogo marcados na pergunta 3
 * @param nenhuma      verdadeiro quando marcou "nenhuma" na pergunta 3
 */
public record RespostaCheckin(Integer confianca, Integer receio, List<Long> dificuldades, boolean nenhuma) {

    public RespostaCheckin {
        dificuldades = dificuldades == null ? List.of() : List.copyOf(dificuldades);
    }
}
