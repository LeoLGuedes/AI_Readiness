package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.QuestaoRepository;
import br.com.prontidaoia.atividade.Tentativa;
import br.com.prontidaoia.atividade.TentativaRepository;
import br.com.prontidaoia.checkin.Checkin;
import br.com.prontidaoia.checkin.CheckinRepository;
import br.com.prontidaoia.checkin.DificuldadeRepository;
import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.config.DemoProperties;
import br.com.prontidaoia.simulacao.RoteiroCarla.AtividadeFixa;
import br.com.prontidaoia.usuario.Usuario;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "Preencher pendências da Carla conforme o roteiro" (seção 6, painel
 * {@code /demo}): a alternativa ao preenchimento pela tela. Só preenche o que
 * ainda está pendente, então pode ser usado depois de a Carla ter feito parte
 * do roteiro ao vivo.
 */
@Component
public class PreenchimentoCarla {

    private final CicloService ciclos;
    private final UsuarioRepository usuarios;
    private final CheckinRepository checkins;
    private final DificuldadeRepository dificuldades;
    private final AtividadeLiberadaRepository liberadas;
    private final QuestaoRepository questoes;
    private final TentativaRepository tentativas;
    private final GeradorDeterministico gerador;
    private final DemoProperties demo;
    private final Clock clock;

    public PreenchimentoCarla(CicloService ciclos, UsuarioRepository usuarios, CheckinRepository checkins,
                              DificuldadeRepository dificuldades, AtividadeLiberadaRepository liberadas,
                              QuestaoRepository questoes, TentativaRepository tentativas,
                              GeradorDeterministico gerador, DemoProperties demo, Clock clock) {
        this.ciclos = ciclos;
        this.usuarios = usuarios;
        this.checkins = checkins;
        this.dificuldades = dificuldades;
        this.liberadas = liberadas;
        this.questoes = questoes;
        this.tentativas = tentativas;
        this.gerador = gerador;
        this.demo = demo;
        this.clock = clock;
    }

    /** @return quantas pendências foram preenchidas */
    @Transactional
    public int preencher() {
        Ciclo ciclo = ciclos.cicloAberto()
                .orElseThrow(() -> new RegraVioladaException("Não há ciclo aberto."));
        if (ciclo.getNumero() != RoteiroCarla.CICLO_AO_VIVO) {
            throw new RegraVioladaException("O roteiro da Carla é do ciclo " + RoteiroCarla.CICLO_AO_VIVO
                    + ", e o ciclo aberto é o " + ciclo.getNumero() + ".");
        }
        Usuario carla = usuarios.findByEmail(PessoasDemo.email(PessoasDemo.CARLA)).orElseThrow();
        Instant agora = Instant.now(clock);
        int preenchidas = 0;

        if (checkins.findByUsuarioIdAndCicloId(carla.getId(), ciclo.getId()).isEmpty()) {
            var marcadas = new HashSet<>(dificuldades.findAllByOrderById().stream()
                    .filter(d -> RoteiroCarla.DIFICULDADES.contains(d.getCodigo()))
                    .toList());
            checkins.save(new Checkin(carla, ciclo, RoteiroCarla.CONFIANCA, RoteiroCarla.RECEIO, marcadas,
                    agora.minusSeconds(40 * 60)));
            preenchidas++;
        }

        List<AtividadeLiberada> escopo = liberadas.escopoDoTime(carla.getTime().getId(), ciclo.getId());
        Random rnd = new Random(demo.semente());
        Instant inicio = agora.minusSeconds(35 * 60);
        for (AtividadeFixa fixa : RoteiroCarla.ATIVIDADES_CICLO_4) {
            AtividadeLiberada liberada = GeradorDeterministico.noSlot(escopo, fixa.slot());
            boolean finalizada = tentativas.findByUsuarioIdAndAtividadeLiberadaId(carla.getId(), liberada.getId())
                    .map(Tentativa::estaFinalizada)
                    .orElse(false);
            if (!finalizada) {
                gerador.registrarAtividadeFixa(carla, liberada, questoes.daAtividade(liberada.getAtividade().getId()),
                        fixa, inicio, rnd);
                preenchidas++;
            }
            inicio = inicio.plusSeconds(fixa.segundos() + 60);
        }
        return preenchidas;
    }
}
