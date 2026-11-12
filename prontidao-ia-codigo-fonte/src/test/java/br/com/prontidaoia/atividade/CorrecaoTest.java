package br.com.prontidaoia.atividade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.prontidaoia.atividade.Correcao.ItemGabarito;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Correção de atividade")
class CorrecaoTest {

    /** Dez questões de quatro alternativas; a certa é sempre a de índice 1. */
    private static final List<ItemGabarito> GABARITO = IntStream.rangeClosed(1, 10)
            .mapToObj(i -> new ItemGabarito(100L + i, i, 1, 4))
            .toList();

    private static Map<Long, Integer> todasCom(int alternativa) {
        Map<Long, Integer> escolhas = new HashMap<>();
        GABARITO.forEach(item -> escolhas.put(item.questaoId(), alternativa));
        return escolhas;
    }

    @Test
    @DisplayName("conta os acertos: 8 certas e 2 erradas dão 8 de 10")
    void contaAcertos() {
        Map<Long, Integer> escolhas = todasCom(1);
        escolhas.put(103L, 0);
        escolhas.put(107L, 2);

        Correcao.Resultado resultado = Correcao.corrigir(GABARITO, escolhas);

        assertThat(resultado.acertos()).isEqualTo(8);
        assertThat(resultado.total()).isEqualTo(10);
        assertThat(resultado.acertouPorQuestao().get(103L)).isFalse();
        assertThat(resultado.acertouPorQuestao().get(104L)).isTrue();
    }

    @Test
    @DisplayName("questão em branco impede a finalização e diz quais faltam")
    void emBrancoERecusado() {
        Map<Long, Integer> escolhas = todasCom(1);
        escolhas.remove(102L);
        escolhas.remove(109L);

        assertThatThrownBy(() -> Correcao.corrigir(GABARITO, escolhas))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessage("Responda todas as questões antes de finalizar. Faltam: 2, 9.");
    }

    @Test
    @DisplayName("alternativa que não existe é recusada")
    void alternativaInexistente() {
        Map<Long, Integer> escolhas = todasCom(1);
        escolhas.put(105L, 7);

        assertThatThrownBy(() -> Correcao.corrigir(GABARITO, escolhas))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("questão 5");
    }
}
