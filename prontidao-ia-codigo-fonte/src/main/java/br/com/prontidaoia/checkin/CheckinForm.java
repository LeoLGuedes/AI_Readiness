package br.com.prontidaoia.checkin;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/** Campos do formulário de check-in, com a validação inline da seção 6. */
public class CheckinForm {

    @NotNull(message = "Escolha uma opção de 1 a 5.")
    @Min(1)
    @Max(5)
    private Integer confianca;

    @NotNull(message = "Escolha uma opção de 1 a 5.")
    @Min(1)
    @Max(5)
    private Integer receio;

    private List<Long> dificuldades = new ArrayList<>();

    private boolean nenhuma;

    @AssertTrue(message = "Marque as dificuldades que você teve ou “nenhuma”.")
    public boolean isPergunta3Respondida() {
        return nenhuma || !dificuldades.isEmpty();
    }

    @AssertTrue(message = "“Nenhuma” não pode ser marcada junto com outras dificuldades.")
    public boolean isNenhumaExclusiva() {
        return !(nenhuma && !dificuldades.isEmpty());
    }

    public RespostaCheckin paraResposta() {
        return new RespostaCheckin(confianca, receio, dificuldades, nenhuma);
    }

    public Integer getConfianca() {
        return confianca;
    }

    public void setConfianca(Integer confianca) {
        this.confianca = confianca;
    }

    public Integer getReceio() {
        return receio;
    }

    public void setReceio(Integer receio) {
        this.receio = receio;
    }

    public List<Long> getDificuldades() {
        return dificuldades;
    }

    public void setDificuldades(List<Long> dificuldades) {
        this.dificuldades = dificuldades == null ? new ArrayList<>() : dificuldades;
    }

    public boolean isNenhuma() {
        return nenhuma;
    }

    public void setNenhuma(boolean nenhuma) {
        this.nenhuma = nenhuma;
    }
}
