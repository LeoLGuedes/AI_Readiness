package br.com.prontidaoia.diagnostico.indicador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import br.com.prontidaoia.diagnostico.indicador.CalculadoraIndicadores.Indicadores;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Indicadores da seção 5.1. Código puro, sem banco. */
@DisplayName("Indicadores (seção 5.1)")
class CalculoIndicadoresTest {

    private static List<RespostaAvaliada> respostas(String area, int certas, int erradas) {
        List<RespostaAvaliada> lista = new ArrayList<>();
        lista.addAll(Collections.nCopies(certas, new RespostaAvaliada(area, true)));
        lista.addAll(Collections.nCopies(erradas, new RespostaAvaliada(area, false)));
        return lista;
    }

    @Test
    @DisplayName("técnico: % de respostas corretas (72 de 100 → 72)")
    void tecnico() {
        assertThat(CalculoTecnico.calcular(respostas("FORMULACAO", 72, 28))).isEqualTo(72.0);
        assertThat(CalculoTecnico.calcular(List.of())).isEqualTo(0.0);
    }

    @Test
    @DisplayName("confiança e resistência: média de (resposta − 1) / 4 × 100")
    void confiancaEResistencia() {
        // (1 + 2 + 3 + 3 + 4) → (0 + 25 + 50 + 50 + 75) / 5 = 40
        assertThat(CalculoConfianca.calcular(List.of(1, 2, 3, 3, 4))).isEqualTo(40.0);
        assertThat(CalculoResistencia.calcular(List.of(5, 5, 4, 4, 4))).isEqualTo(85.0);
    }

    @Test
    @DisplayName("engajamento do Atendimento no ciclo 4: 7 check-ins + 12 atividades de 24 → 79,2")
    void engajamentoDoCiclo4() {
        assertThat(CalculoEngajamento.esperadas(8, 2)).isEqualTo(24);
        assertThat(CalculoEngajamento.calcular(8, 2, 7, 12)).isCloseTo(79.17, within(0.01));
    }

    @Test
    @DisplayName("produtividade: tempo médio das tentativas finalizadas, em minutos")
    void produtividade() {
        List<Duration> duracoes = List.of(Duration.ofMinutes(11), Duration.ofMinutes(10), Duration.ofSeconds(690));
        // (11 + 10 + 11,5) / 3 = 10,83
        assertThat(CalculoProdutividade.minutosPorAtividade(duracoes)).isCloseTo(10.83, within(0.01));
    }

    @Test
    @DisplayName("erro por área: 100 − % de acerto em cada área")
    void erroPorArea() {
        List<RespostaAvaliada> todas = new ArrayList<>();
        todas.addAll(respostas("VERIFICACAO", 2, 1));
        todas.addAll(respostas("DADOS", 3, 1));

        Map<String, Double> erro = CalculoErroPorArea.erroPorArea(todas);

        assertThat(erro.get("VERIFICACAO")).isCloseTo(33.33, within(0.01));
        assertThat(erro.get("DADOS")).isEqualTo(25.0);
        assertThat(erro).doesNotContainKey("FORMULACAO");
    }

    @Test
    @DisplayName("com menos de 5 check-ins, confiança e resistência ficam nulas; o resto é calculado")
    void amostraInsuficiente() {
        CalculadoraIndicadores calculadora = new CalculadoraIndicadores(new AmostraMinima(5));
        DadosDoTimeNoCiclo quatroCheckins = new DadosDoTimeNoCiclo(
                respostas("FORMULACAO", 7, 3), List.of(2, 3, 4, 5), List.of(1, 1, 2, 2),
                List.of(Duration.ofMinutes(12)), 8, 2);

        Indicadores valores = calculadora.calcular(quatroCheckins);

        assertThat(valores.confianca()).isNull();
        assertThat(valores.resistencia()).isNull();
        assertThat(valores.tecnico()).isEqualByComparingTo("70.0");
        assertThat(valores.checkins()).isEqualTo(4);
    }

    @Test
    @DisplayName("com 5 check-ins há amostra, e os valores são gravados com uma casa decimal")
    void amostraSuficienteEUmaCasa() {
        CalculadoraIndicadores calculadora = new CalculadoraIndicadores(new AmostraMinima(5));
        DadosDoTimeNoCiclo cinco = new DadosDoTimeNoCiclo(
                respostas("FORMULACAO", 2, 1), List.of(2, 3, 3, 3, 3), List.of(4, 4, 4, 4, 5),
                List.of(Duration.ofSeconds(640), Duration.ofSeconds(660)), 8, 2);

        Indicadores valores = calculadora.calcular(cinco);

        assertThat(valores.tecnico()).isEqualByComparingTo("66.7");      // 2/3
        assertThat(valores.confianca()).isEqualByComparingTo("45.0");    // (25 + 4×50) / 5
        assertThat(valores.resistencia()).isEqualByComparingTo("80.0");  // (4×75 + 100) / 5
        assertThat(valores.engajamento()).isEqualByComparingTo("29.2");  // 7 / 24
        assertThat(valores.tempoMedioMin()).isEqualByComparingTo("10.8"); // 650 s
    }
}
