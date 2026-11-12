package br.com.prontidaoia.atividade;

import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Execução de uma atividade pelo colaborador: abrir, finalizar e ver o
 * resultado (seção 6).
 *
 * <p>Regras que moram aqui:</p>
 * <ul>
 *   <li>a pessoa só abre atividades liberadas ao <b>próprio time</b>;</li>
 *   <li>abrir a atividade cria a tentativa e marca {@code iniciada_em};</li>
 *   <li>finalizar exige todas as questões respondidas ({@link Correcao});</li>
 *   <li>{@code finalizada_em} é gravado <b>uma única vez</b>
 *       ({@link Tentativa#finalizar});</li>
 *   <li>só se inicia ou finaliza no ciclo aberto.</li>
 * </ul>
 */
@Service
public class ExecucaoAtividadeService {

    private final AtividadeLiberadaRepository liberadas;
    private final QuestaoRepository questoes;
    private final TentativaRepository tentativas;
    private final RespostaRepository respostas;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public ExecucaoAtividadeService(AtividadeLiberadaRepository liberadas,
                                    QuestaoRepository questoes,
                                    TentativaRepository tentativas,
                                    RespostaRepository respostas,
                                    UsuarioRepository usuarios,
                                    Clock clock) {
        this.liberadas = liberadas;
        this.questoes = questoes;
        this.tentativas = tentativas;
        this.respostas = respostas;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    /** Verdadeiro quando a pessoa já finalizou esta atividade. */
    @Transactional(readOnly = true)
    public boolean jaFinalizou(UsuarioAutenticado usuario, Long liberadaId) {
        AtividadeLiberada liberada = liberadaDoTime(usuario, liberadaId);
        return tentativas.findByUsuarioIdAndAtividadeLiberadaId(usuario.getId(), liberada.getId())
                .map(Tentativa::estaFinalizada)
                .orElse(false);
    }

    /**
     * Abre a atividade. Na primeira vez, cria a tentativa; nas seguintes,
     * devolve a mesma tentativa, para o cronômetro não recomeçar.
     */
    @Transactional
    public AtividadeParaResponder abrir(UsuarioAutenticado usuario, Long liberadaId) {
        AtividadeLiberada liberada = liberadaDoTime(usuario, liberadaId);

        Tentativa tentativa = tentativas
                .findByUsuarioIdAndAtividadeLiberadaId(usuario.getId(), liberada.getId())
                .orElseGet(() -> {
                    exigirCicloAberto(liberada);
                    return tentativas.save(new Tentativa(
                            usuarios.getReferenceById(usuario.getId()), liberada, Instant.now(clock)));
                });

        List<AtividadeParaResponder.QuestaoParaResponder> itens = questoes
                .daAtividade(liberada.getAtividade().getId()).stream()
                .map(q -> new AtividadeParaResponder.QuestaoParaResponder(
                        q.getId(), q.getOrdem(), q.getArea().getNome(), q.getEnunciado(), q.getAlternativas()))
                .toList();

        Atividade atividade = liberada.getAtividade();
        return new AtividadeParaResponder(liberada.getId(), atividade.getCodigo(), atividade.getTitulo(),
                atividade.getDescricao(), tentativa.getIniciadaEm(), itens);
    }

    /**
     * Finaliza a tentativa: corrige, grava as respostas e marca
     * {@code finalizada_em}. Se qualquer regra falhar, nada é gravado.
     *
     * @param escolhas índice da alternativa escolhida, por id de questão
     * @param tempos   segundos gastos em cada questão, por id de questão
     */
    @Transactional
    public void finalizar(UsuarioAutenticado usuario, Long liberadaId,
                          Map<Long, Integer> escolhas, Map<Long, Integer> tempos) {
        AtividadeLiberada liberada = liberadaDoTime(usuario, liberadaId);
        Tentativa tentativa = tentativas
                .findByUsuarioIdAndAtividadeLiberadaId(usuario.getId(), liberada.getId())
                .orElseThrow(() -> new RegraVioladaException("Abra a atividade antes de finalizá-la."));

        if (tentativa.estaFinalizada()) {
            throw new RegraVioladaException("Esta atividade já foi finalizada.");
        }
        exigirCicloAberto(liberada);

        List<Questao> daAtividade = questoes.daAtividade(liberada.getAtividade().getId());
        Correcao.Resultado resultado = Correcao.corrigir(
                daAtividade.stream()
                        .map(q -> new Correcao.ItemGabarito(
                                q.getId(), q.getOrdem(), q.getIndiceCorreto(), q.getAlternativas().size()))
                        .toList(),
                escolhas);

        for (Questao questao : daAtividade) {
            respostas.save(new Resposta(
                    tentativa,
                    questao,
                    escolhas.get(questao.getId()),
                    resultado.acertouPorQuestao().get(questao.getId()),
                    Math.max(0, tempos.getOrDefault(questao.getId(), 0))));
        }
        tentativa.finalizar(Instant.now(clock));
    }

    /** Resultado e gabarito comentado. Só existe depois de finalizar. */
    @Transactional(readOnly = true)
    public ResultadoAtividade resultado(UsuarioAutenticado usuario, Long liberadaId) {
        AtividadeLiberada liberada = liberadaDoTime(usuario, liberadaId);
        Tentativa tentativa = tentativas
                .findByUsuarioIdAndAtividadeLiberadaId(usuario.getId(), liberada.getId())
                .filter(Tentativa::estaFinalizada)
                .orElseThrow(() -> new RegraVioladaException("Finalize a atividade para ver o resultado."));

        List<ResultadoAtividade.ItemCorrigido> itens = respostas.daTentativa(tentativa.getId()).stream()
                .map(r -> {
                    Questao q = r.getQuestao();
                    return new ResultadoAtividade.ItemCorrigido(
                            q.getOrdem(), q.getArea().getNome(), q.getEnunciado(), q.getAlternativas(),
                            r.getAlternativaEscolhida(), q.getIndiceCorreto(), q.getComentario(),
                            r.getTempoSegundos());
                })
                .toList();

        int acertos = (int) itens.stream().filter(ResultadoAtividade.ItemCorrigido::acertou).count();
        Atividade atividade = liberada.getAtividade();
        return new ResultadoAtividade(liberada.getId(), atividade.getCodigo(), atividade.getTitulo(),
                acertos, itens.size(), tentativa.duracao(), itens);
    }

    /**
     * Carrega a atividade liberada, conferindo que ela é do time da pessoa. O
     * time vem da sessão; de outro time, a resposta é "não encontrada".
     */
    private AtividadeLiberada liberadaDoTime(UsuarioAutenticado usuario, Long liberadaId) {
        return liberadas.comAtividadeECiclo(liberadaId)
                .filter(l -> l.getTime().getId().equals(usuario.getTimeId()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
    }

    private static void exigirCicloAberto(AtividadeLiberada liberada) {
        if (!liberada.getCiclo().estaAberto()) {
            throw new RegraVioladaException("Esta atividade era do ciclo "
                    + liberada.getCiclo().getNumero() + ", que já foi fechado.");
        }
    }
}
