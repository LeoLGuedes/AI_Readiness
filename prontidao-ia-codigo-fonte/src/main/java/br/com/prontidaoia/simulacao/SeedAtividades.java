package br.com.prontidaoia.simulacao;

import br.com.prontidaoia.atividade.Area;
import br.com.prontidaoia.atividade.AreaRepository;
import br.com.prontidaoia.atividade.Atividade;
import br.com.prontidaoia.atividade.AtividadeRepository;
import br.com.prontidaoia.atividade.Questao;
import br.com.prontidaoia.atividade.QuestaoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Semeia as atividades "1A" … "6B" e a trilha de verificação (seção 8.1).
 *
 * <p>O conteúdo das questões fica em {@code catalogo/atividades.json}, gerado
 * por {@code ferramentas/gerar_atividades.py}. Para mudar uma questão, edite o
 * script e gere o JSON de novo: assim o grupo revisa o texto num lugar só.</p>
 */
@Component
public class SeedAtividades {

    static final String ARQUIVO = "catalogo/atividades.json";

    private final AtividadeRepository atividades;
    private final QuestaoRepository questoes;
    private final AreaRepository areas;
    private final ObjectMapper json;

    public SeedAtividades(AtividadeRepository atividades,
                          QuestaoRepository questoes,
                          AreaRepository areas,
                          ObjectMapper json) {
        this.atividades = atividades;
        this.questoes = questoes;
        this.areas = areas;
        this.json = json;
    }

    /** Semeia, se ainda não houver atividade nenhuma. */
    @Transactional
    public void semear() {
        if (atividades.count() > 0) {
            return;
        }
        Map<String, Area> porCodigo = areas.findAll().stream()
                .collect(Collectors.toMap(Area::getCodigo, Function.identity()));

        for (AtividadeJson a : lerArquivo().atividades()) {
            Atividade atividade = atividades.save(new Atividade(a.codigo(), a.titulo(), a.nivel(), a.descricao()));
            for (QuestaoJson q : a.questoes()) {
                Area area = porCodigo.get(q.area());
                if (area == null) {
                    throw new IllegalStateException("Área desconhecida em " + a.codigo() + ": " + q.area());
                }
                questoes.save(new Questao(atividade, area, q.ordem(), q.enunciado(),
                        q.alternativas(), q.indiceCorreto(), q.comentario()));
            }
        }
    }

    private CatalogoJson lerArquivo() {
        try (InputStream entrada = new ClassPathResource(ARQUIVO).getInputStream()) {
            return json.readValue(entrada, CatalogoJson.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Não foi possível ler " + ARQUIVO, e);
        }
    }

    record CatalogoJson(List<AtividadeJson> atividades) {
    }

    record AtividadeJson(String codigo, String titulo, int nivel, String descricao, List<QuestaoJson> questoes) {
    }

    record QuestaoJson(int ordem, String area, String enunciado, List<String> alternativas,
                       int indiceCorreto, String comentario) {
    }
}
