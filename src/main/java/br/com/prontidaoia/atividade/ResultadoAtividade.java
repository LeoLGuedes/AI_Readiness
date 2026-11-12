package br.com.prontidaoia.atividade;

import java.time.Duration;
import java.util.List;

/** Tela de resultado: acertos, tempo e gabarito comentado (seção 6). */
public record ResultadoAtividade(
        Long liberadaId,
        String codigo,
        String titulo,
        int acertos,
        int total,
        Duration duracao,
        List<ItemCorrigido> itens) {

    /** Uma questão já corrigida, com o comentário do gabarito. */
    public record ItemCorrigido(
            int ordem,
            String area,
            String enunciado,
            List<String> alternativas,
            int escolhida,
            int correta,
            String comentario,
            int tempoSegundos) {

        public boolean acertou() {
            return escolhida == correta;
        }
    }

    /** "10 min 32 s". */
    public String duracaoFormatada() {
        return duracao.toMinutes() + " min " + duracao.toSecondsPart() + " s";
    }
}
