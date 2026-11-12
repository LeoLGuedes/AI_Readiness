package br.com.prontidaoia.diagnostico.indicador;

import br.com.prontidaoia.comum.Arredondamento;
import java.math.BigDecimal;

/**
 * Junta as classes de cálculo da seção 5.1 e produz a fotografia do time no
 * ciclo, já arredondada para uma casa decimal, como vai para o banco.
 *
 * <p>Código puro: recebe {@link DadosDoTimeNoCiclo} e devolve números.</p>
 */
public final class CalculadoraIndicadores {

    private final AmostraMinima amostra;

    public CalculadoraIndicadores(AmostraMinima amostra) {
        this.amostra = amostra;
    }

    /** Valores de um time num ciclo. Confiança e resistência podem ser nulas. */
    public record Indicadores(
            BigDecimal tecnico,
            BigDecimal confianca,
            BigDecimal resistencia,
            BigDecimal engajamento,
            BigDecimal tempoMedioMin,
            int checkins,
            int membros) {
    }

    public Indicadores calcular(DadosDoTimeNoCiclo dados) {
        boolean temAmostra = amostra.suficiente(dados.checkins());

        return new Indicadores(
                Arredondamento.umaCasa(CalculoTecnico.calcular(dados.respostas())),
                temAmostra ? Arredondamento.umaCasa(CalculoConfianca.calcular(dados.confiancas())) : null,
                temAmostra ? Arredondamento.umaCasa(CalculoResistencia.calcular(dados.receios())) : null,
                Arredondamento.umaCasa(CalculoEngajamento.calcular(
                        dados.membros(), dados.atividadesLiberadas(),
                        dados.checkins(), dados.tentativasFinalizadas())),
                Arredondamento.umaCasa(CalculoProdutividade.minutosPorAtividade(dados.duracoes())),
                dados.checkins(),
                dados.membros());
    }
}
