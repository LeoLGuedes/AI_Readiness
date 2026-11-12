package br.com.prontidaoia.diagnostico.indicador;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.prontidaoia.diagnostico.alerta.PontoDoCiclo;
import br.com.prontidaoia.diagnostico.alerta.RegraConfiancaSemVerificacao;
import br.com.prontidaoia.diagnostico.alerta.SerieDoTime;
import br.com.prontidaoia.diagnostico.indicador.CalculadoraIndicadores.Indicadores;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Melhoria M1 (PBL 8): o limite exato da amostra mínima, em código puro.
 *
 * <p>A regra de privacidade inteira depende de uma comparação só,
 * {@code checkins >= 5}. Se alguém trocar por {@code >} ou por {@code >= 4},
 * um destes testes quebra. O lado da tela está em
 * {@code gestao.LimiteDaAmostraNaGestaoTest}.</p>
 */
@DisplayName("Limite exato da amostra mínima: 4 ocultam, 5 liberam")
class AmostraMinimaTest {

    private static final int MINIMO = 5; // application.yml: amostra-minima-checkins

    private final AmostraMinima amostra = new AmostraMinima(MINIMO);
    private final CalculadoraIndicadores calculadora = new CalculadoraIndicadores(amostra);
    private final RegraConfiancaSemVerificacao regraB = new RegraConfiancaSemVerificacao(75, 50);

    @ParameterizedTest(name = "{0} check-ins ocultam")
    @ValueSource(ints = {0, 1, 4})
    @DisplayName("abaixo do mínimo, não há amostra")
    void abaixoDoMinimo(int checkins) {
        assertThat(amostra.suficiente(checkins)).isFalse();
    }

    @ParameterizedTest(name = "{0} check-ins liberam")
    @ValueSource(ints = {5, 6, 8})
    @DisplayName("a partir do mínimo, inclusive, há amostra")
    void aPartirDoMinimo(int checkins) {
        assertThat(amostra.suficiente(checkins)).isTrue();
    }

    /** n check-ins iguais, todos com confiança máxima e receio máximo. */
    private Indicadores comCheckins(int n) {
        return calculadora.calcular(new DadosDoTimeNoCiclo(
                List.of(), Collections.nCopies(n, 5), Collections.nCopies(n, 5),
                List.of(Duration.ofMinutes(10)), 8, 2));
    }

    @Test
    @DisplayName("4 check-ins: confiança e resistência nulas, mesmo com respostas unânimes")
    void quatroOcultamOsIndicadoresDePercepcao() {
        Indicadores quatro = comCheckins(4);

        assertThat(quatro.confianca()).isNull();
        assertThat(quatro.resistencia()).isNull();
        assertThat(quatro.checkins()).isEqualTo(4);
    }

    @Test
    @DisplayName("5 check-ins: confiança e resistência calculadas")
    void cincoLiberamOsIndicadoresDePercepcao() {
        Indicadores cinco = comCheckins(5);

        assertThat(cinco.confianca()).isEqualByComparingTo("100.0");
        assertThat(cinco.resistencia()).isEqualByComparingTo("100.0");
        assertThat(cinco.checkins()).isEqualTo(5);
    }

    /**
     * Do cálculo à regra B, sem atalho: a confiança que a calculadora grava é a
     * que o fechamento passa à regra. Verificação em 20% nos dois ciclos, bem
     * abaixo de 50%.
     */
    private SerieDoTime doisCiclosCom(int checkins) {
        BigDecimal confianca = comCheckins(checkins).confianca();
        BigDecimal verificacao = BigDecimal.valueOf(20);
        BigDecimal tecnico = BigDecimal.valueOf(60);
        BigDecimal engajamento = BigDecimal.valueOf(80);
        return SerieDoTime.de(List.of(
                new PontoDoCiclo(1, tecnico, confianca, engajamento, verificacao),
                new PontoDoCiclo(2, tecnico, confianca, engajamento, verificacao)));
    }

    @Test
    @DisplayName("4 check-ins: a regra B não é avaliada, mesmo com confiança 100 e verificação 20%")
    void quatroNaoAvaliamRegras() {
        assertThat(regraB.avaliar(doisCiclosCom(4), 2)).isEmpty();
    }

    @Test
    @DisplayName("5 check-ins: a regra B é avaliada e dispara com os mesmos números")
    void cincoAvaliamRegras() {
        assertThat(regraB.avaliar(doisCiclosCom(5), 2)).isPresent();
    }
}
