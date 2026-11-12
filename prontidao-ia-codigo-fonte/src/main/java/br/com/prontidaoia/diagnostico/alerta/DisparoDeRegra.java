package br.com.prontidaoia.diagnostico.alerta;

/** Uma regra de alerta que valeu: qual tipo e com que valores. */
public record DisparoDeRegra(TipoAlerta tipo, DetalheAlerta detalhe) {
}
