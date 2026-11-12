package br.com.prontidaoia.comum;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Arredondamento (seção 5.1)")
class ArredondamentoTest {

    @Test
    @DisplayName("na tela, a metade sobe: 47,5 vira 48 e 47,4 vira 47")
    void telaArredondaMetadeParaCima() {
        assertThat(Arredondamento.paraTela(47.5)).isEqualTo(48);
        assertThat(Arredondamento.paraTela(47.4)).isEqualTo(47);
        assertThat(Arredondamento.paraTela(new BigDecimal("62.5"))).isEqualTo(63);
    }

    @Test
    @DisplayName("no banco, uma casa decimal com a metade subindo")
    void bancoGuardaUmaCasa() {
        assertThat(Arredondamento.umaCasa(46.875)).isEqualByComparingTo("46.9");
        assertThat(Arredondamento.umaCasa(53.125)).isEqualByComparingTo("53.1");
        assertThat(Arredondamento.umaCasa(47.25)).isEqualByComparingTo("47.3");
    }

    @Test
    @DisplayName("a escala de 1 a 5 vira 0 a 100: 1 → 0, 2 → 25, 3 → 50, 5 → 100")
    void escalaDeUmACincoViraZeroACem() {
        assertThat(Arredondamento.escalaParaCem(1)).isEqualTo(0.0);
        assertThat(Arredondamento.escalaParaCem(2)).isEqualTo(25.0);
        assertThat(Arredondamento.escalaParaCem(3)).isEqualTo(50.0);
        assertThat(Arredondamento.escalaParaCem(5)).isEqualTo(100.0);
    }
}
