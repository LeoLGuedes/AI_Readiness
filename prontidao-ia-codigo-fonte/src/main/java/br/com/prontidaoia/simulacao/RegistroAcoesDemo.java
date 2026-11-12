package br.com.prontidaoia.simulacao;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Registro das ações executadas no painel de demonstração (seção 6). Fica em
 * memória: é um apoio para quem conduz a demonstração, não um dado do domínio.
 */
@Component
@Profile("demo")
public class RegistroAcoesDemo {

    private static final int LIMITE = 50;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Deque<Acao> acoes = new ArrayDeque<>();
    private final Clock clock;

    public RegistroAcoesDemo(Clock clock) {
        this.clock = clock;
    }

    /** Uma linha do registro. */
    public record Acao(String hora, String titulo, List<String> detalhes) {
    }

    public synchronized void registrar(String titulo, List<String> detalhes) {
        String hora = LocalTime.now(clock.withZone(ZoneId.of("America/Sao_Paulo"))).format(HORA);
        acoes.addFirst(new Acao(hora, titulo, List.copyOf(detalhes)));
        while (acoes.size() > LIMITE) {
            acoes.removeLast();
        }
    }

    /** Da mais recente para a mais antiga. */
    public synchronized List<Acao> recentes() {
        return List.copyOf(acoes);
    }
}
