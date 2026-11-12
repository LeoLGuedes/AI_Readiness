package br.com.prontidaoia.diagnostico.alerta;

import static br.com.prontidaoia.diagnostico.alerta.MetasSecao84.ATENDIMENTO;
import static br.com.prontidaoia.diagnostico.alerta.MetasSecao84.FINANCEIRO;
import static br.com.prontidaoia.diagnostico.alerta.MetasSecao84.MARKETING;
import static br.com.prontidaoia.diagnostico.alerta.MetasSecao84.ponto;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Regras da seção 5.2 com os números da seção 8.4. Código puro, sem banco:
 * cada teste é uma frase da especificação.
 */
@DisplayName("Regras de alerta (seção 5.2) com as metas da seção 8.4")
class RegrasDeAlertaTest {

    // Limiares de application.yml.
    private final RegraDivergenciaTecnicoConfianca regraA = new RegraDivergenciaTecnicoConfianca(5);
    private final RegraConfiancaSemVerificacao regraB = new RegraConfiancaSemVerificacao(75, 50);
    private final RegraPrioridade prioridade = new RegraPrioridade(10);
    private final RegraEncerramentoDivergencia encerramentoA = new RegraEncerramentoDivergencia(2);
    private final RegraEncerramentoConfianca encerramentoB = new RegraEncerramentoConfianca(2, regraB);

    private static List<Integer> ciclosEmQue(java.util.function.IntPredicate dispara) {
        return IntStream.rangeClosed(1, 6).filter(dispara).boxed().toList();
    }

    @Nested
    @DisplayName("Regra A: sabe usar, mas não se sente pronto")
    class RegraA {

        @Test
        @DisplayName("dispara para o Atendimento no ciclo 4 e não no 3 (queda de 63 para 59 é só 4)")
        void atendimentoSoNoCiclo4() {
            assertThat(regraA.avaliar(ATENDIMENTO, 3)).isEmpty();
            assertThat(regraA.avaliar(ATENDIMENTO, 4)).isPresent();
            assertThat(ciclosEmQue(c -> regraA.avaliar(ATENDIMENTO, c).isPresent())).containsExactly(4);
        }

        @Test
        @DisplayName("nunca dispara para o Financeiro nem para o Marketing")
        void nuncaParaOsOutros() {
            assertThat(ciclosEmQue(c -> regraA.avaliar(FINANCEIRO, c).isPresent())).isEmpty();
            assertThat(ciclosEmQue(c -> regraA.avaliar(MARKETING, c).isPresent())).isEmpty();
        }

        @Test
        @DisplayName("o detalhe traz a frase da seção 6 com os valores que dispararam")
        void fraseDoDetalhe() {
            DetalheAlerta detalhe = regraA.avaliar(ATENDIMENTO, 4).orElseThrow().detalhe();

            assertThat(detalhe.regra()).isEqualTo("A");
            assertThat(detalhe.ciclo()).isEqualTo(4);
            assertThat(detalhe.frase())
                    .isEqualTo("Técnico subiu de 61 para 67 e 72; confiança caiu de 59 para 53 e 47.");
        }

        @Test
        @DisplayName("a queda exigida é 5 inclusive: 5 dispara, 4,9 não")
        void limiteDaQueda() {
            SerieDoTime cincoExatos = SerieDoTime.de(List.of(
                    ponto(1, 50, 60.0, 90, 60.0), ponto(2, 55, 55.0, 90, 60.0), ponto(3, 60, 50.0, 90, 60.0)));
            SerieDoTime quaseCinco = SerieDoTime.de(List.of(
                    ponto(1, 50, 60.0, 90, 60.0), ponto(2, 55, 55.1, 90, 60.0), ponto(3, 60, 50.0, 90, 60.0)));

            assertThat(regraA.avaliar(cincoExatos, 3)).isPresent();
            assertThat(regraA.avaliar(quaseCinco, 3)).isEmpty();
        }

        @Test
        @DisplayName("sem amostra (confiança nula) em qualquer dos três ciclos, não dispara")
        void semAmostraNaoAvalia() {
            SerieDoTime semAmostraNoMeio = SerieDoTime.de(List.of(
                    ponto(1, 50, 70.0, 90, 60.0), ponto(2, 55, null, 90, 60.0), ponto(3, 60, 50.0, 90, 60.0)));

            assertThat(regraA.avaliar(semAmostraNoMeio, 3)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Regra B: confiança alta sem senso crítico")
    class RegraB {

        @Test
        @DisplayName("dispara para o Marketing a partir do ciclo 2, em todos os ciclos seguintes")
        void marketingDesdeOCiclo2() {
            assertThat(ciclosEmQue(c -> regraB.avaliar(MARKETING, c).isPresent()))
                    .containsExactly(2, 3, 4, 5, 6);
        }

        @Test
        @DisplayName("nunca dispara para o Atendimento nem para o Financeiro")
        void nuncaParaOsOutros() {
            assertThat(ciclosEmQue(c -> regraB.avaliar(ATENDIMENTO, c).isPresent())).isEmpty();
            assertThat(ciclosEmQue(c -> regraB.avaliar(FINANCEIRO, c).isPresent())).isEmpty();
        }

        @Test
        @DisplayName("confiança 75 conta como alta; acerto de 50% em verificação já não dispara")
        void limites() {
            SerieDoTime noLimite = SerieDoTime.de(List.of(
                    ponto(1, 60, 75.0, 90, 49.9), ponto(2, 60, 75.0, 90, 49.9)));
            SerieDoTime verificacaoCinquenta = SerieDoTime.de(List.of(
                    ponto(1, 60, 80.0, 90, 50.0), ponto(2, 60, 80.0, 90, 50.0)));

            assertThat(regraB.avaliar(noLimite, 2)).isPresent();
            assertThat(regraB.avaliar(verificacaoCinquenta, 2)).isEmpty();
        }

        @Test
        @DisplayName("o detalhe explica a regra com os valores dos dois ciclos")
        void fraseDoDetalhe() {
            DetalheAlerta detalhe = regraB.avaliar(MARKETING, 2).orElseThrow().detalhe();

            assertThat(detalhe.frase()).isEqualTo(
                    "Confiança de 78 e 81 nos ciclos 1 e 2, com acerto em verificação de 38% e 40%.");
        }
    }

    @Nested
    @DisplayName("Prioridade")
    class Prioridade {

        @Test
        @DisplayName("Atendimento no ciclo 4: engajamento cai 13 pontos (92 → 79), prioridade ALTA")
        void altaNoAtendimento() {
            assertThat(prioridade.prioridade(ATENDIMENTO, 4)).isEqualTo(PrioridadeAlerta.ALTA);
        }

        @Test
        @DisplayName("Marketing no ciclo 2: engajamento cai 4 pontos, prioridade NORMAL")
        void normalNoMarketing() {
            assertThat(prioridade.prioridade(MARKETING, 2)).isEqualTo(PrioridadeAlerta.NORMAL);
        }

        @Test
        @DisplayName("queda de exatamente 10 já é ALTA")
        void limite() {
            SerieDoTime dez = SerieDoTime.de(List.of(ponto(1, 60, 60.0, 90, 60.0), ponto(2, 60, 60.0, 80, 60.0)));
            assertThat(prioridade.prioridade(dez, 2)).isEqualTo(PrioridadeAlerta.ALTA);
        }
    }

    @Nested
    @DisplayName("Encerramento")
    class Encerramento {

        @Test
        @DisplayName("alerta A com intervenção no ciclo 5: não encerra no ciclo 5, encerra no 6 (47 → 50 → 56)")
        void alertaAResolveNoCiclo6() {
            assertThat(encerramentoA.deveEncerrar(ATENDIMENTO, 5, 5)).isFalse();
            assertThat(encerramentoA.deveEncerrar(ATENDIMENTO, 6, 5)).isTrue();
        }

        @Test
        @DisplayName("subidas antes do ciclo de aplicação não contam")
        void subidasAntesDaIntervencaoNaoContam() {
            // O Financeiro sobe todo ciclo, mas com intervenção no ciclo 5 só o 5 e o 6 contam.
            assertThat(encerramentoA.deveEncerrar(FINANCEIRO, 5, 5)).isFalse();
            assertThat(encerramentoA.deveEncerrar(FINANCEIRO, 6, 5)).isTrue();
        }

        @Test
        @DisplayName("alerta B do Marketing nunca encerra: a condição continua valendo")
        void alertaBDoMarketingContinua() {
            assertThat(ciclosEmQue(c -> encerramentoB.deveEncerrar(MARKETING, c))).isEmpty();
        }

        @Test
        @DisplayName("alerta B encerra quando a condição deixa de valer por dois ciclos seguidos")
        void alertaBEncerraDepoisDeDoisCiclos() {
            SerieDoTime melhora = SerieDoTime.de(List.of(
                    ponto(1, 60, 80.0, 90, 40.0),
                    ponto(2, 60, 80.0, 90, 40.0),
                    ponto(3, 60, 80.0, 90, 55.0),
                    ponto(4, 60, 80.0, 90, 60.0)));

            assertThat(encerramentoB.deveEncerrar(melhora, 3)).isFalse();
            assertThat(encerramentoB.deveEncerrar(melhora, 4)).isTrue();
        }
    }
}
