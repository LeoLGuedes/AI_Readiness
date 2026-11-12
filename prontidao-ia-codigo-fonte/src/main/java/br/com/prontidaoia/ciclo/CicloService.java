package br.com.prontidaoia.ciclo;

import br.com.prontidaoia.atividade.Atividade;
import br.com.prontidaoia.atividade.AtividadeLiberada;
import br.com.prontidaoia.atividade.AtividadeLiberadaRepository;
import br.com.prontidaoia.atividade.AtividadeRepository;
import br.com.prontidaoia.comum.excecao.RegraVioladaException;
import br.com.prontidaoia.config.CiclosProperties;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ciclo aberto e abertura de ciclos.
 *
 * <p>O fechamento, com o cálculo dos indicadores, fica em
 * {@code FechamentoCicloService}. Aqui só se abre ciclo e se libera o escopo.</p>
 */
@Service
public class CicloService {

    /** "4A", "4B": atividade do escopo padrão, presa ao número do ciclo. */
    private static final Pattern ATIVIDADE_DO_CICLO = Pattern.compile("^(\\d+)([A-Z])$");

    private final CicloRepository ciclos;
    private final TimeRepository times;
    private final AtividadeRepository atividades;
    private final AtividadeLiberadaRepository liberadas;
    private final CiclosProperties config;

    public CicloService(CicloRepository ciclos,
                        TimeRepository times,
                        AtividadeRepository atividades,
                        AtividadeLiberadaRepository liberadas,
                        CiclosProperties config) {
        this.ciclos = ciclos;
        this.times = times;
        this.atividades = atividades;
        this.liberadas = liberadas;
        this.config = config;
    }

    /** O ciclo aberto. Existe no máximo um (índice único parcial da seção 4). */
    @Transactional(readOnly = true)
    public Optional<Ciclo> cicloAberto() {
        return ciclos.findByStatus(StatusCiclo.ABERTO);
    }

    /**
     * Escopo padrão de um ciclo: as atividades "NA" e "NB" (seção 8.1).
     * A troca de escopo da seção 5.4 substitui uma delas pela trilha.
     */
    public static List<String> escopoPadrao(int numeroDoCiclo) {
        return List.of(numeroDoCiclo + "A", numeroDoCiclo + "B");
    }

    /**
     * Abre o ciclo 1 e libera o escopo padrão a todos os times. Usado quando o
     * sistema ainda não tem ciclo nenhum.
     */
    @Transactional
    public Ciclo abrirPrimeiroCiclo(LocalDate inicio) {
        if (ciclos.count() > 0) {
            throw new RegraVioladaException("O acompanhamento já começou.");
        }
        Ciclo ciclo = ciclos.save(new Ciclo(1, inicio, fimAPartirDe(inicio)));

        List<Atividade> escopo = atividades.findByCodigoIn(escopoPadrao(1));
        if (escopo.size() != 2) {
            throw new IllegalStateException("Atividades 1A e 1B não encontradas: o catálogo foi semeado?");
        }
        for (TimeEntity time : times.findAllByOrderByNome()) {
            for (Atividade atividade : escopo) {
                liberadas.save(new AtividadeLiberada(time, ciclo, atividade));
            }
        }
        return ciclo;
    }

    /**
     * Etapa 6 do fechamento (seção 5.5): abre o ciclo seguinte e cria as
     * atividades liberadas <b>copiando o escopo vigente</b> de cada time.
     *
     * <p>Copiar o escopo quer dizer (decisão D35): as atividades do escopo
     * padrão avançam de número ("4A" vira "5A"); uma atividade vinda de troca
     * de escopo, como a trilha, continua a mesma e leva a recomendação junto.</p>
     */
    @Transactional
    public Ciclo abrirProximo(Ciclo anterior) {
        if (anterior.estaAberto()) {
            throw new IllegalStateException("O ciclo " + anterior.getNumero() + " ainda está aberto.");
        }
        LocalDate inicio = anterior.getFim().plusDays(1);
        Ciclo proximo = ciclos.save(new Ciclo(anterior.getNumero() + 1, inicio, fimAPartirDe(inicio)));

        for (AtividadeLiberada liberada : liberadas.doCiclo(anterior.getId())) {
            Atividade atividade = atividadeNoProximo(liberada.getAtividade(), proximo.getNumero());
            liberadas.save(new AtividadeLiberada(liberada.getTime(), proximo, atividade, liberada.getRecomendacaoId()));
        }
        return proximo;
    }

    private Atividade atividadeNoProximo(Atividade atual, int numeroDoProximo) {
        Matcher m = ATIVIDADE_DO_CICLO.matcher(atual.getCodigo());
        if (!m.matches()) {
            return atual;
        }
        String codigo = numeroDoProximo + m.group(2);
        return atividades.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Atividade " + codigo + " não existe no catálogo."));
    }

    private LocalDate fimAPartirDe(LocalDate inicio) {
        return inicio.plusDays(config.diasPorCiclo() - 1L);
    }
}
