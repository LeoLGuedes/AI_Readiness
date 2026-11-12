package br.com.prontidaoia.gestao.consulta;

import br.com.prontidaoia.comum.Arredondamento;
import java.math.BigDecimal;

/**
 * Um indicador como a tela mostra: inteiro, com a variação em relação ao
 * ciclo anterior (↑ ↓ =).
 *
 * @param subirEhBom falso para a resistência, em que subir é ruim; define a cor
 */
public record Kpi(String nome, Integer valor, Integer delta, boolean subirEhBom) {

    public static Kpi de(String nome, BigDecimal atual, BigDecimal anterior, boolean subirEhBom) {
        Integer valor = atual == null ? null : Arredondamento.paraTela(atual);
        Integer delta = (valor == null || anterior == null) ? null : valor - Arredondamento.paraTela(anterior);
        return new Kpi(nome, valor, delta, subirEhBom);
    }

    /** "↑ +5", "↓ −6" ou "= 0". */
    public String variacao() {
        if (delta == null) {
            return "";
        }
        if (delta > 0) {
            return "↑ +" + delta;
        }
        if (delta < 0) {
            return "↓ −" + Math.abs(delta);
        }
        return "= 0";
    }

    /** Classe de cor da variação: verde quando melhora, vermelho quando piora. */
    public String classe() {
        if (delta == null || delta == 0) {
            return "";
        }
        return (delta > 0) == subirEhBom ? "up" : "down";
    }
}
