package br.com.prontidaoia.checkin;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.ciclo.CicloService;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.usuario.UsuarioRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro do check-in semanal (seções 4 e 6).
 *
 * <p>Regras que moram aqui:</p>
 * <ul>
 *   <li><b>um check-in por pessoa e por ciclo</b>: o segundo envio é recusado;</li>
 *   <li>só se responde no ciclo aberto;</li>
 *   <li>as duas escalas vão de 1 a 5;</li>
 *   <li>a pergunta 3 é obrigatória, e "nenhuma" não se combina com outras
 *       dificuldades (decisão D18).</li>
 * </ul>
 */
@Service
public class CheckinService {

    static final String JA_RESPONDIDO = "Você já fez o check-in deste ciclo.";

    private final CheckinRepository checkins;
    private final DificuldadeRepository dificuldades;
    private final UsuarioRepository usuarios;
    private final CicloService ciclos;
    private final Clock clock;

    public CheckinService(CheckinRepository checkins,
                          DificuldadeRepository dificuldades,
                          UsuarioRepository usuarios,
                          CicloService ciclos,
                          Clock clock) {
        this.checkins = checkins;
        this.dificuldades = dificuldades;
        this.usuarios = usuarios;
        this.ciclos = ciclos;
        this.clock = clock;
    }

    /** O check-in da pessoa no ciclo aberto, se já foi feito. */
    @Transactional(readOnly = true)
    public Optional<Checkin> doCicloAberto(Long usuarioId) {
        return ciclos.cicloAberto()
                .flatMap(ciclo -> checkins.findByUsuarioIdAndCicloId(usuarioId, ciclo.getId()));
    }

    @Transactional
    public Checkin registrar(Long usuarioId, RespostaCheckin resposta) {
        Ciclo ciclo = ciclos.cicloAberto()
                .orElseThrow(() -> new RegraVioladaException("Não há ciclo aberto para responder."));

        if (checkins.existsByUsuarioIdAndCicloId(usuarioId, ciclo.getId())) {
            throw new RegraVioladaException(JA_RESPONDIDO);
        }

        validarEscala(resposta.confianca(), "Responda à pergunta 1.");
        validarEscala(resposta.receio(), "Responda à pergunta 2.");
        Set<Dificuldade> marcadas = dificuldadesMarcadas(resposta);

        Checkin checkin = new Checkin(
                usuarios.getReferenceById(usuarioId),
                ciclo,
                resposta.confianca(),
                resposta.receio(),
                marcadas,
                Instant.now(clock));

        try {
            return checkins.saveAndFlush(checkin);
        } catch (DataIntegrityViolationException e) {
            // Dois envios simultâneos: o índice único do banco segura o segundo.
            throw new RegraVioladaException(JA_RESPONDIDO);
        }
    }

    private static void validarEscala(Integer valor, String mensagem) {
        if (valor == null || valor < 1 || valor > 5) {
            throw new RegraVioladaException(mensagem);
        }
    }

    private Set<Dificuldade> dificuldadesMarcadas(RespostaCheckin resposta) {
        if (resposta.nenhuma() && !resposta.dificuldades().isEmpty()) {
            throw new RegraVioladaException("“Nenhuma” não pode ser marcada junto com outras dificuldades.");
        }
        if (!resposta.nenhuma() && resposta.dificuldades().isEmpty()) {
            throw new RegraVioladaException("Responda à pergunta 3: marque as dificuldades ou “nenhuma”.");
        }

        Set<Dificuldade> encontradas = new HashSet<>(dificuldades.findAllById(resposta.dificuldades()));
        if (encontradas.size() != new HashSet<>(resposta.dificuldades()).size()) {
            throw new RegraVioladaException("Dificuldade desconhecida.");
        }
        return encontradas;
    }
}
