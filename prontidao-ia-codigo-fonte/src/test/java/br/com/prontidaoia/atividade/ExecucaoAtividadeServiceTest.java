package br.com.prontidaoia.atividade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.prontidaoia.TesteComBanco;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.simulacao.PessoasDemo;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * Execução de atividade: abrir, finalizar e corrigir. Cada teste roda numa
 * transação desfeita no fim.
 */
@SpringBootTest
@ActiveProfiles("demo")
@Transactional
@DisplayName("Execução de atividade")
class ExecucaoAtividadeServiceTest extends TesteComBanco {

    @Autowired
    private ExecucaoAtividadeService service;

    @Autowired
    private UsuarioRepository usuarios;

    @Autowired
    private AtividadeLiberadaRepository liberadas;

    @Autowired
    private QuestaoRepository questoes;

    @Autowired
    private TentativaRepository tentativas;

    @Autowired
    private RespostaRepository respostas;

    @Autowired
    private CicloService ciclos;

    private UsuarioAutenticado carla;
    private AtividadeLiberada atividadeA;
    private List<Questao> questoesDaA;

    @BeforeEach
    void preparar() {
        carla = new UsuarioAutenticado(usuarios.findByEmail(PessoasDemo.email(PessoasDemo.CARLA)).orElseThrow());
        Long cicloAberto = ciclos.cicloAberto().orElseThrow().getId();
        atividadeA = liberadas.escopoDoTime(carla.getTimeId(), cicloAberto).getFirst();
        questoesDaA = questoes.daAtividade(atividadeA.getAtividade().getId());
    }

    /** Escolhas que acertam todas as questões, menos as das ordens indicadas. */
    private Map<Long, Integer> escolhasErrando(Integer... ordensErradas) {
        List<Integer> erradas = List.of(ordensErradas);
        Map<Long, Integer> escolhas = new HashMap<>();
        for (Questao q : questoesDaA) {
            int certa = q.getIndiceCorreto();
            escolhas.put(q.getId(), erradas.contains(q.getOrdem()) ? (certa + 1) % 4 : certa);
        }
        return escolhas;
    }

    private Tentativa tentativaDaCarla() {
        return tentativas.findByUsuarioIdAndAtividadeLiberadaId(carla.getId(), atividadeA.getId()).orElseThrow();
    }

    @Test
    @DisplayName("abrir cria a tentativa uma vez só; reabrir não reinicia o cronômetro")
    void abrirCriaTentativaUmaVez() {
        AtividadeParaResponder primeira = service.abrir(carla, atividadeA.getId());
        AtividadeParaResponder segunda = service.abrir(carla, atividadeA.getId());

        assertThat(primeira.questoes()).hasSize(10);
        assertThat(segunda.iniciadaEm()).isEqualTo(primeira.iniciadaEm());
        assertThat(tentativaDaCarla().estaFinalizada()).isFalse();
    }

    @Test
    @DisplayName("finalizar com questão em branco é recusado e não grava nada")
    void emBrancoNaoFinaliza() {
        service.abrir(carla, atividadeA.getId());
        Map<Long, Integer> escolhas = escolhasErrando();
        escolhas.remove(questoesDaA.get(4).getId());

        assertThatThrownBy(() -> service.finalizar(carla, atividadeA.getId(), escolhas, Map.of()))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessageContaining("Faltam: 5");

        assertThat(tentativaDaCarla().getFinalizadaEm()).isNull();
        assertThat(respostas.daTentativa(tentativaDaCarla().getId())).isEmpty();
    }

    @Test
    @DisplayName("a correção conta os acertos e grava o tempo de cada questão")
    void correcaoContaAcertosETempo() {
        service.abrir(carla, atividadeA.getId());
        Map<Long, Integer> tempos = new HashMap<>();
        questoesDaA.forEach(q -> tempos.put(q.getId(), 60 + q.getOrdem()));

        service.finalizar(carla, atividadeA.getId(), escolhasErrando(3, 8), tempos);

        List<Resposta> gravadas = respostas.daTentativa(tentativaDaCarla().getId());
        assertThat(gravadas).hasSize(10);
        assertThat(gravadas).filteredOn(Resposta::getCorreta).hasSize(8);
        assertThat(gravadas).filteredOn(r -> !r.getCorreta())
                .extracting(r -> r.getQuestao().getOrdem())
                .containsExactlyInAnyOrder(3, 8);
        assertThat(gravadas).allSatisfy(r ->
                assertThat(r.getTempoSegundos()).isEqualTo(60 + r.getQuestao().getOrdem()));

        ResultadoAtividade resultado = service.resultado(carla, atividadeA.getId());
        assertThat(resultado.acertos()).isEqualTo(8);
        assertThat(resultado.total()).isEqualTo(10);
    }

    @Test
    @DisplayName("finalizada_em é gravado uma única vez")
    void finalizaUmaVezSo() {
        service.abrir(carla, atividadeA.getId());
        service.finalizar(carla, atividadeA.getId(), escolhasErrando(1), Map.of());
        Instant primeiraFinalizacao = tentativaDaCarla().getFinalizadaEm();
        assertThat(primeiraFinalizacao).isNotNull();

        assertThatThrownBy(() ->
                service.finalizar(carla, atividadeA.getId(), escolhasErrando(), Map.of()))
                .isInstanceOf(RegraVioladaException.class)
                .hasMessage("Esta atividade já foi finalizada.");

        assertThat(tentativaDaCarla().getFinalizadaEm()).isEqualTo(primeiraFinalizacao);
        assertThat(respostas.daTentativa(tentativaDaCarla().getId()))
                .filteredOn(Resposta::getCorreta).hasSize(9);
    }

    @Test
    @DisplayName("finalizar sem ter aberto a atividade é recusado")
    void finalizarSemAbrir() {
        assertThatThrownBy(() ->
                service.finalizar(carla, atividadeA.getId(), escolhasErrando(), Map.of()))
                .isInstanceOf(RegraVioladaException.class);
    }

    @Test
    @DisplayName("ninguém abre a atividade liberada a outro time")
    void atividadeDeOutroTime() {
        UsuarioAutenticado financeiro = new UsuarioAutenticado(
                usuarios.findByEmail(PessoasDemo.email("Eduardo Salgado")).orElseThrow());

        assertThatThrownBy(() -> service.abrir(financeiro, atividadeA.getId()))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
