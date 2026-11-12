package br.com.prontidaoia.colaborador;

import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.RespostaRepository;
import br.com.prontidaoia.atividade.Tentativa;
import br.com.prontidaoia.atividade.TentativaRepository;
import br.com.prontidaoia.checkin.CheckinRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.colaborador.SemanaDoColaborador.PendenciaAtividade;
import br.com.prontidaoia.colaborador.SemanaDoColaborador.Situacao;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta a semana da pessoa no ciclo aberto: check-in e atividades do time. */
@Service
public class SemanaDoColaboradorService {

    private final CicloService ciclos;
    private final CheckinRepository checkins;
    private final AtividadeLiberadaRepository liberadas;
    private final TentativaRepository tentativas;
    private final RespostaRepository respostas;

    public SemanaDoColaboradorService(CicloService ciclos,
                                      CheckinRepository checkins,
                                      AtividadeLiberadaRepository liberadas,
                                      TentativaRepository tentativas,
                                      RespostaRepository respostas) {
        this.ciclos = ciclos;
        this.checkins = checkins;
        this.liberadas = liberadas;
        this.tentativas = tentativas;
        this.respostas = respostas;
    }

    @Transactional(readOnly = true)
    public SemanaDoColaborador daPessoa(UsuarioAutenticado usuario) {
        Ciclo ciclo = ciclos.cicloAberto().orElse(null);
        if (ciclo == null) {
            return SemanaDoColaborador.semCicloAberto();
        }

        Map<Long, Tentativa> tentativaPorLiberada = tentativas.daPessoaNoCiclo(usuario.getId(), ciclo.getId())
                .stream()
                .collect(Collectors.toMap(t -> t.getAtividadeLiberada().getId(), Function.identity()));

        List<PendenciaAtividade> atividades = liberadas.escopoDoTime(usuario.getTimeId(), ciclo.getId()).stream()
                .map(liberada -> pendencia(liberada, tentativaPorLiberada.get(liberada.getId())))
                .toList();

        return new SemanaDoColaborador(
                ciclo,
                checkins.existsByUsuarioIdAndCicloId(usuario.getId(), ciclo.getId()),
                atividades);
    }

    private PendenciaAtividade pendencia(AtividadeLiberada liberada, Tentativa tentativa) {
        Situacao situacao;
        Long acertos = null;
        if (tentativa == null) {
            situacao = Situacao.NAO_INICIADA;
        } else if (!tentativa.estaFinalizada()) {
            situacao = Situacao.EM_ANDAMENTO;
        } else {
            situacao = Situacao.CONCLUIDA;
            acertos = respostas.acertosDaTentativa(tentativa.getId());
        }
        return new PendenciaAtividade(liberada.getId(), liberada.getAtividade().getCodigo(),
                liberada.getAtividade().getTitulo(), situacao, acertos);
    }
}
