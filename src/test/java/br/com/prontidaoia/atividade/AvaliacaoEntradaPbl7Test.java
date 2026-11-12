package br.com.prontidaoia.atividade;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** PBL 7: entradas de formulário não confiáveis são tratadas como ausência de escolha. */
@DisplayName("PBL7 - entradas inválidas no quiz")
class AvaliacaoEntradaPbl7Test {

    @Test
    @DisplayName("valores vazios, texto e IDs não numéricos são ignorados")
    void valoresInvalidosNaoViraramRespostas() {
        Map<String,String> parametros = Map.of(
                "q1", "", "q2", "abc", "qX", "2", "q3", " 2 ", "t3", "18");
        assertThat(AtividadeController.porQuestao(parametros, "q"))
                .containsExactlyInAnyOrderEntriesOf(Map.of(3L, 2));
    }

    @Test
    @DisplayName("campos de duração não são confundidos com alternativas")
    void prefixosNaoSeMisturam() {
        Map<String,String> parametros = Map.of("q10", "1", "t10", "21");
        assertThat(AtividadeController.porQuestao(parametros, "q")).containsOnlyKeys(10L);
        assertThat(AtividadeController.porQuestao(parametros, "t")).containsEntry(10L,21);
    }
}
