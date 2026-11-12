package br.com.prontidaoia.gestao;

import br.com.prontidaoia.checkin.Dificuldade;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.config.LimiaresProperties;
import br.com.prontidaoia.diagnostico.indicador.AmostraMinima;
import br.com.prontidaoia.diagnostico.indicador.ColetaDoCiclo;
import br.com.prontidaoia.diagnostico.indicador.IndicadorTimeCiclo;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * <b>Regra de privacidade da seção 4</b>, no lado do gestor.
 *
 * <p>É a <b>única</b> porta pela qual uma tela de gestão chega a dados de
 * check-in que não estão na fotografia do indicador (hoje, a contagem de
 * dificuldades). Ela só devolve contagens do time inteiro, e só quando o time
 * teve a amostra mínima de check-ins no ciclo. Nenhum método recebe ou devolve
 * uma pessoa.</p>
 *
 * <p>Confiança e resistência já chegam protegidas: o fechamento grava as duas
 * como nulas quando falta amostra.</p>
 */
@Component
public class PrivacidadeGuard {

    private final ColetaDoCiclo coleta;
    private final DificuldadeRepository dificuldades;
    private final AmostraMinima amostra;

    public PrivacidadeGuard(ColetaDoCiclo coleta, DificuldadeRepository dificuldades, LimiaresProperties limiares) {
        this.coleta = coleta;
        this.dificuldades = dificuldades;
        this.amostra = new AmostraMinima(limiares.amostraMinimaCheckins());
    }

    /** Uma dificuldade e quantas respostas do time a marcaram. */
    public record ContagemDoTime(String dificuldade, long vezes, int respostas) {

        public int percentual() {
            return respostas == 0 ? 0 : (int) Math.round(vezes * 100.0 / respostas);
        }
    }

    public boolean temAmostra(IndicadorTimeCiclo indicador) {
        return amostra.suficiente(indicador.getNCheckins());
    }

    public int minimo() {
        return amostra.minimo();
    }

    /**
     * Dificuldades relatadas pelo time no ciclo, na ordem do catálogo. Vazio
     * quando falta amostra: nesse caso a tela mostra o aviso de amostra
     * insuficiente.
     */
    public Optional<List<ContagemDoTime>> dificuldadesDoTime(IndicadorTimeCiclo indicador) {
        if (!temAmostra(indicador)) {
            return Optional.empty();
        }
        Map<Long, Long> vezes = coleta.contagemDeDificuldades(
                indicador.getTime().getId(), indicador.getCiclo().getId());
        int respostas = indicador.getNCheckins();
        return Optional.of(dificuldades.findAllByOrderById().stream()
                .map((Dificuldade d) -> new ContagemDoTime(d.getDescricao(), vezes.getOrDefault(d.getId(), 0L),
                        respostas))
                .toList());
    }
}
