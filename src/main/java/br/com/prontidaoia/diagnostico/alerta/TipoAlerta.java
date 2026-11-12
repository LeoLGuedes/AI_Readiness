package br.com.prontidaoia.diagnostico.alerta;

/** Os dois alertas da seção 5.2, com o nome exibido nas telas. */
public enum TipoAlerta {

    /** Regra A: o técnico sobe e a confiança cai. */
    DIVERGENCIA_TECNICO_CONFIANCA("Sabe usar, mas não se sente pronto"),

    /** Regra B: confiança alta e pouco acerto em verificação. */
    CONFIANCA_SEM_VERIFICACAO("Confiança alta sem senso crítico");

    private final String nomeExibido;

    TipoAlerta(String nomeExibido) {
        this.nomeExibido = nomeExibido;
    }

    public String getNomeExibido() {
        return nomeExibido;
    }
}
