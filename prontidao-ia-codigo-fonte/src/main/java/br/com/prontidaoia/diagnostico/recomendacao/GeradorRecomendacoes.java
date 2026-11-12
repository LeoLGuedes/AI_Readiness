package br.com.prontidaoia.diagnostico.recomendacao;

import br.com.prontidaoia.checkin.Dificuldade;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.config.LimiaresProperties;
import br.com.prontidaoia.diagnostico.alerta.Alerta;
import br.com.prontidaoia.diagnostico.recomendacao.ContagemDificuldades.Marcacao;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gera as recomendações de um alerta novo (seção 5.3).
 *
 * <ul>
 *   <li><b>Alerta A:</b> uma recomendação para cada uma das duas
 *       dificuldades mais marcadas, com a intervenção ligada a ela;</li>
 *   <li><b>Alerta B:</b> uma recomendação com
 *       {@code TRILHA_VERIFICACAO_CHECKLIST}.</li>
 * </ul>
 * Todas nascem SUGERIDA: quem decide é a gestão.
 */
@Component
public class GeradorRecomendacoes {

    static final String INTERVENCAO_ALERTA_B = "TRILHA_VERIFICACAO_CHECKLIST";

    private final RecomendacaoRepository recomendacoes;
    private final DificuldadeRepository dificuldades;
    private final IntervencaoRepository intervencoes;
    private final LimiaresProperties limiares;

    public GeradorRecomendacoes(RecomendacaoRepository recomendacoes,
                                DificuldadeRepository dificuldades,
                                IntervencaoRepository intervencoes,
                                LimiaresProperties limiares) {
        this.recomendacoes = recomendacoes;
        this.dificuldades = dificuldades;
        this.intervencoes = intervencoes;
        this.limiares = limiares;
    }

    /**
     * @param vezesPorDificuldade marcações de cada dificuldade no ciclo
     * @param totalRespostas      check-ins do time no ciclo
     */
    @Transactional
    public List<Recomendacao> paraAlertaA(Alerta alerta, Map<Long, Long> vezesPorDificuldade, int totalRespostas) {
        List<Marcacao> escolhidas = ContagemDificuldades.maisMarcadas(
                vezesPorDificuldade, limiares.dificuldadesPorRecomendacao());

        return escolhidas.stream()
                .map(m -> {
                    Dificuldade dificuldade = dificuldades.findById(m.dificuldadeId()).orElseThrow();
                    return recomendacoes.save(new Recomendacao(alerta, dificuldade.getIntervencao(), dificuldade,
                            (int) m.vezes(), totalRespostas));
                })
                .toList();
    }

    /** A recomendação do alerta B não vem de check-in: contagem 0 de 0 (decisão D38). */
    @Transactional
    public List<Recomendacao> paraAlertaB(Alerta alerta) {
        Intervencao trilha = intervencoes.findByCodigo(INTERVENCAO_ALERTA_B).orElseThrow();
        return List.of(recomendacoes.save(new Recomendacao(alerta, trilha, null, 0, 0)));
    }
}
