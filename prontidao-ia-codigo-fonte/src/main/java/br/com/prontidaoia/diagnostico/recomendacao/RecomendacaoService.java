package br.com.prontidaoia.diagnostico.recomendacao;

import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * As três decisões da gestão sobre as recomendações de um alerta (seção 5.4)
 * e o agendamento das intervenções humanas.
 *
 * <p>O sistema recomenda e a gestão decide: nenhuma recomendação muda de
 * status sem uma dessas chamadas, e cada uma registra quem decidiu e quando.</p>
 */
@Service
public class RecomendacaoService {

    private final RecomendacaoRepository recomendacoes;
    private final UsuarioRepository usuarios;
    private final Clock clock;

    public RecomendacaoService(RecomendacaoRepository recomendacoes, UsuarioRepository usuarios, Clock clock) {
        this.recomendacoes = recomendacoes;
        this.usuarios = usuarios;
        this.clock = clock;
    }

    /**
     * "Aceitar e ajustar escopo": os itens passam a ACEITA. Um item já
     * AJUSTADO continua AJUSTADO, porque também foi aceito, só que com o texto
     * da gestão (decisão D45).
     */
    @Transactional
    public List<Recomendacao> aceitar(Long alertaId, Long gestorId) {
        List<Recomendacao> itens = doAlerta(alertaId);
        itens.stream()
                .filter(r -> r.getStatus() != StatusRecomendacao.AJUSTADA)
                .forEach(r -> r.decidir(StatusRecomendacao.ACEITA, usuarios.getReferenceById(gestorId),
                        Instant.now(clock)));
        return itens;
    }

    /** Fluxo alternativo FA1: a gestão reescreve o texto de um item. */
    @Transactional
    public Recomendacao ajustar(Long recomendacaoId, String novoTexto, Long gestorId) {
        if (novoTexto == null || novoTexto.isBlank()) {
            throw new RegraVioladaException("Escreva o texto ajustado da recomendação.");
        }
        Recomendacao item = porId(recomendacaoId);
        item.ajustar(novoTexto.trim(), usuarios.getReferenceById(gestorId), Instant.now(clock));
        return item;
    }

    /** "Adiar": todos os itens do alerta passam a ADIADA. */
    @Transactional
    public List<Recomendacao> adiar(Long alertaId, Long gestorId) {
        List<Recomendacao> itens = doAlerta(alertaId);
        itens.forEach(r -> r.decidir(StatusRecomendacao.ADIADA, usuarios.getReferenceById(gestorId),
                Instant.now(clock)));
        return itens;
    }

    /**
     * Marca uma intervenção humana como agendada. O sistema registra a data,
     * mas não executa a intervenção (seção 5.4).
     */
    @Transactional
    public Recomendacao agendar(Long recomendacaoId, LocalDate dataPrevista) {
        Recomendacao item = porId(recomendacaoId);
        if (item.getIntervencao().getTipo() != TipoIntervencao.HUMANA) {
            throw new RegraVioladaException("Só intervenções humanas são agendadas.");
        }
        if (dataPrevista == null) {
            throw new RegraVioladaException("Informe a data prevista.");
        }
        item.agendar(dataPrevista);
        return item;
    }

    /** Time do alerta a que a recomendação pertence, para voltar à tela certa. */
    @Transactional(readOnly = true)
    public Long timeDaRecomendacao(Long recomendacaoId) {
        return porId(recomendacaoId).getAlerta().getTime().getId();
    }

    private List<Recomendacao> doAlerta(Long alertaId) {
        List<Recomendacao> itens = recomendacoes.doAlerta(alertaId);
        if (itens.isEmpty()) {
            throw new RecursoNaoEncontradoException("Alerta sem recomendações.");
        }
        return itens;
    }

    private Recomendacao porId(Long id) {
        return recomendacoes.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendação não encontrada."));
    }
}
