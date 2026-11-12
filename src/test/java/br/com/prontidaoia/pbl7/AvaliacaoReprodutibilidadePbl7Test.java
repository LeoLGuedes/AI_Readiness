package br.com.prontidaoia.pbl7;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.StatusCiclo;
import br.com.prontidaoia.diagnostico.indicador.IndicadorRepository;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import br.com.prontidaoia.simulacao.CenarioInicial;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** PBL 7: repetição controlada do cenário para isolar a avaliação de flutuações de dados. */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("PBL7 - reprodutibilidade dos indicadores")
class AvaliacaoReprodutibilidadePbl7Test extends TesteComBanco {

    @Autowired private CenarioInicial cenario;
    @Autowired private IndicadorRepository indicadores;
    @Autowired private CicloRepository ciclos;

    private List<String> fotografia() {
        return indicadores.findAll().stream()
                .map(this::resumo)
                .sorted()
                .toList();
    }

    private String resumo(IndicadorTimeCiclo i) {
        // Desconsiderar IDs autogerados, timestamps e datas de execução.
        return String.join("|",
                i.getTime().getNome(),
                String.valueOf(i.getCiclo().getNumero()),
                String.valueOf(i.getTecnico()),
                String.valueOf(i.getConfianca()),
                String.valueOf(i.getResistencia()),
                String.valueOf(i.getEngajamento()),
                String.valueOf(i.getTempoMedioMin()),
                String.valueOf(i.getNCheckins()));
    }

    @Test
    @DisplayName("restaurar duas vezes resulta nos mesmos 9 conjuntos de indicadores e abre ciclo 4")
    void restauracoesProduzemMesmaFotografia() {
        cenario.restaurar();
        List<String> primeira = fotografia();
        assertThat(primeira).hasSize(9); // 3 times x 3 ciclos fechados
        assertThat(ciclos.findAllByOrderByNumero()).hasSize(4);
        assertThat(ciclos.findByStatus(StatusCiclo.ABERTO).orElseThrow().getNumero())
                .isEqualTo(4);

        cenario.restaurar();
        List<String> segunda = fotografia();
        assertThat(segunda).containsExactlyElementsOf(primeira);
        assertThat(ciclos.findAllByOrderByNumero()).hasSize(4);
    }
}
