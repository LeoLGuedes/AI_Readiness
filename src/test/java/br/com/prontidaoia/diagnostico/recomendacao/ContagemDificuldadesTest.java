package br.com.prontidaoia.diagnostico.recomendacao;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.prontidaoia.diagnostico.recomendacao.ContagemDificuldades.Marcacao;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("As duas dificuldades mais marcadas (seção 5.3)")
class ContagemDificuldadesTest {

    // Ids na ordem do catálogo da migration V2.
    private static final long RECEIO = 1;
    private static final long NAO_SEI_CONFIAR = 2;
    private static final long FALTA_TEMPO = 3;
    private static final long NAO_SE_ENCAIXA = 4;
    private static final long LIDERANCA_NAO_USA = 5;

    @Test
    @DisplayName("Atendimento, ciclo 4 (5, 3, 2, 1, 0): receio (5) e não sei confiar (3)")
    void cenarioDoCiclo4() {
        Map<Long, Long> contagem = Map.of(
                RECEIO, 5L, NAO_SEI_CONFIAR, 3L, FALTA_TEMPO, 2L, NAO_SE_ENCAIXA, 1L, LIDERANCA_NAO_USA, 0L);

        assertThat(ContagemDificuldades.maisMarcadas(contagem, 2))
                .containsExactly(new Marcacao(RECEIO, 5), new Marcacao(NAO_SEI_CONFIAR, 3));
    }

    @Test
    @DisplayName("empate é resolvido pela ordem do catálogo")
    void empatePelaOrdemDoCatalogo() {
        Map<Long, Long> contagem = Map.of(LIDERANCA_NAO_USA, 4L, FALTA_TEMPO, 4L, NAO_SEI_CONFIAR, 4L);

        assertThat(ContagemDificuldades.maisMarcadas(contagem, 2))
                .extracting(Marcacao::dificuldadeId)
                .containsExactly(NAO_SEI_CONFIAR, FALTA_TEMPO);
    }

    @Test
    @DisplayName("dificuldade que ninguém marcou não vira recomendação")
    void zeroNaoEntra() {
        Map<Long, Long> contagem = Map.of(NAO_SE_ENCAIXA, 3L, RECEIO, 0L);

        assertThat(ContagemDificuldades.maisMarcadas(contagem, 2))
                .containsExactly(new Marcacao(NAO_SE_ENCAIXA, 3));
    }
}
