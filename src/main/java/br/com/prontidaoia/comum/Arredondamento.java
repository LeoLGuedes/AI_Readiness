package br.com.prontidaoia.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de arredondamento da seção 5.1.
 *
 * <p>O banco guarda uma casa decimal, e as regras comparam esse valor
 * guardado. As telas mostram inteiros. Nos dois casos, a metade sobe:
 * 47,5 vira 48; 47,45 vira 47,5.</p>
 */
public final class Arredondamento {

    private Arredondamento() {
    }

    /** Valor como é gravado no banco: uma casa decimal, metade para cima. */
    public static BigDecimal umaCasa(double valor) {
        return BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP);
    }

    /** Valor como é mostrado na tela: inteiro, metade para cima. */
    public static int paraTela(double valor) {
        return BigDecimal.valueOf(valor).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /** Mesmo arredondamento de tela, para um valor já gravado. */
    public static int paraTela(BigDecimal valor) {
        return valor.setScale(0, RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * Converte uma resposta da escala de 1 a 5 para 0 a 100, como na seção 5.1:
     * {@code (resposta − 1) / 4 × 100}. 1 vira 0, 3 vira 50, 5 vira 100.
     */
    public static double escalaParaCem(int resposta) {
        return (resposta - 1) / 4.0 * 100.0;
    }
}
