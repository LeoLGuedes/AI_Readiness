package br.com.prontidaoia.colaborador;

import br.com.prontidaoia.atividade.AcertoNoCiclo;
import br.com.prontidaoia.atividade.RespostaRepository;
import br.com.prontidaoia.checkin.Checkin;
import br.com.prontidaoia.checkin.CheckinRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloRepository;
import br.com.prontidaoia.ciclo.StatusCiclo;
import br.com.prontidaoia.comum.Arredondamento;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Histórico pessoal (seção 6, {@code /meu-historico}).
 *
 * <p>É o único lugar do sistema com percepção individual, e só para a
 * própria pessoa (decisão D20). O método recebe apenas o id de quem está
 * logado; não existe variante que aceite o id de outra pessoa.</p>
 */
@Service
public class HistoricoPessoalService {

    private final CicloRepository ciclos;
    private final CheckinRepository checkins;
    private final RespostaRepository respostas;

    public HistoricoPessoalService(CicloRepository ciclos, CheckinRepository checkins, RespostaRepository respostas) {
        this.ciclos = ciclos;
        this.checkins = checkins;
        this.respostas = respostas;
    }

    /** Uma linha da tabela e um ponto de cada série do gráfico. */
    public record LinhaHistorico(
            int ciclo,
            LocalDate inicio,
            LocalDate fim,
            StatusCiclo status,
            Integer acertoPercentual,
            long acertos,
            long respostas,
            Integer confianca,
            Integer confiancaPercentual,
            Integer receio) {
    }

    @Transactional(readOnly = true)
    public List<LinhaHistorico> daPessoa(Long usuarioId) {
        Map<Long, Checkin> checkinPorCiclo = checkins.findByUsuarioId(usuarioId).stream()
                .collect(Collectors.toMap(c -> c.getCiclo().getId(), Function.identity()));
        Map<Long, AcertoNoCiclo> acertoPorCiclo = respostas.acertoDaPessoaPorCiclo(usuarioId).stream()
                .collect(Collectors.toMap(AcertoNoCiclo::cicloId, Function.identity()));

        return ciclos.findAllByOrderByNumero().stream()
                .map(ciclo -> linha(ciclo, checkinPorCiclo.get(ciclo.getId()), acertoPorCiclo.get(ciclo.getId())))
                .toList();
    }

    private static LinhaHistorico linha(Ciclo ciclo, Checkin checkin, AcertoNoCiclo acerto) {
        return new LinhaHistorico(
                ciclo.getNumero(),
                ciclo.getInicio(),
                ciclo.getFim(),
                ciclo.getStatus(),
                acerto == null ? null : Arredondamento.paraTela(acerto.percentual()),
                acerto == null ? 0 : acerto.acertos(),
                acerto == null ? 0 : acerto.respostas(),
                checkin == null ? null : checkin.getConfianca(),
                checkin == null ? null : Arredondamento.paraTela(Arredondamento.escalaParaCem(checkin.getConfianca())),
                checkin == null ? null : checkin.getReceioSubstituicao());
    }
}
