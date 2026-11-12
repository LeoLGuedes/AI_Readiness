package br.com.prontidaoia.colaborador;

import br.com.prontidaoia.ciclo.Ciclo;
import br.com.prontidaoia.comum.Arredondamento;
import java.util.List;

/**
 * O que a tela {@code /inicio} mostra: o ciclo atual, as pendências e o
 * progresso.
 *
 * <p>As pendências são as mesmas que o engajamento da seção 5.1 conta: um
 * check-in e uma tentativa finalizada por atividade liberada.</p>
 */
public record SemanaDoColaborador(Ciclo ciclo, boolean checkinFeito, List<PendenciaAtividade> atividades) {

    public enum Situacao {
        NAO_INICIADA("Não iniciada"),
        EM_ANDAMENTO("Em andamento"),
        CONCLUIDA("Concluída");

        private final String rotulo;

        Situacao(String rotulo) {
            this.rotulo = rotulo;
        }

        public String getRotulo() {
            return rotulo;
        }
    }

    /** Uma atividade do escopo do time, com a situação da própria pessoa. */
    public record PendenciaAtividade(Long liberadaId, String codigo, String titulo,
                                     Situacao situacao, Long acertos) {

        public boolean concluida() {
            return situacao == Situacao.CONCLUIDA;
        }
    }

    public boolean temCicloAberto() {
        return ciclo != null;
    }

    public int total() {
        return 1 + atividades.size();
    }

    public int concluidas() {
        return (checkinFeito ? 1 : 0) + (int) atividades.stream().filter(PendenciaAtividade::concluida).count();
    }

    public int percentual() {
        return Arredondamento.paraTela(concluidas() * 100.0 / total());
    }

    public static SemanaDoColaborador semCicloAberto() {
        return new SemanaDoColaborador(null, false, List.of());
    }
}
