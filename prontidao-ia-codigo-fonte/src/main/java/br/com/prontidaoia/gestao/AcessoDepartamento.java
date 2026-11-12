package br.com.prontidaoia.gestao;

import br.com.prontidaoia.comum.excecao.RecursoNaoEncontradoException;
import br.com.prontidaoia.diagnostico.alerta.AlertaRepository;
import br.com.prontidaoia.diagnostico.recomendacao.RecomendacaoRepository;
import br.com.prontidaoia.usuario.TimeEntity;
import br.com.prontidaoia.usuario.TimeRepository;
import br.com.prontidaoia.usuario.UsuarioAutenticado;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>Hierarquia de departamentos</b>: a única porta que decide quais times
 * uma pessoa da gestão pode acompanhar.
 *
 * <ul>
 *   <li><b>Gerente de RH</b> (papel GESTOR): todos os departamentos;</li>
 *   <li><b>Líder</b> (papel LIDER): somente o departamento que lidera;</li>
 *   <li>qualquer outro papel: nenhum.</li>
 * </ul>
 *
 * <p>É a segunda camada da segurança. A primeira, por prefixo de URL, fica em
 * {@code SecurityConfig} e só separa quem entra em {@code /gestao}. Esta
 * classe confere o <i>departamento</i> de cada time, alerta e recomendação
 * pedidos pela URL: trocar o id no navegador devolve 403.</p>
 */
@Component
public class AcessoDepartamento {

    private final TimeRepository times;
    private final AlertaRepository alertas;
    private final RecomendacaoRepository recomendacoes;

    public AcessoDepartamento(TimeRepository times, AlertaRepository alertas,
                              RecomendacaoRepository recomendacoes) {
        this.times = times;
        this.alertas = alertas;
        this.recomendacoes = recomendacoes;
    }

    /** Departamentos que o usuário acompanha, em ordem alfabética. */
    @Transactional(readOnly = true)
    public List<TimeEntity> departamentosVisiveis(UsuarioAutenticado usuario) {
        if (usuario == null) {
            return List.of();
        }
        if (usuario.isGestor()) {
            return times.findAllByOrderByNome();
        }
        if (usuario.isLider() && usuario.getDepartamentoLideradoId() != null) {
            return times.findById(usuario.getDepartamentoLideradoId()).map(List::of).orElse(List.of());
        }
        return List.of();
    }

    @Transactional(readOnly = true)
    public Set<Long> idsVisiveis(UsuarioAutenticado usuario) {
        return departamentosVisiveis(usuario).stream().map(TimeEntity::getId).collect(Collectors.toSet());
    }

    /** Verdadeiro quando o usuário vê a empresa toda (gerente de RH). */
    public boolean veTodos(UsuarioAutenticado usuario) {
        return usuario != null && usuario.isGestor();
    }

    public boolean podeVer(UsuarioAutenticado usuario, Long timeId) {
        if (usuario == null || timeId == null) {
            return false;
        }
        if (usuario.isGestor()) {
            return true;
        }
        return usuario.isLider() && timeId.equals(usuario.getDepartamentoLideradoId());
    }

    /** Barra o acesso a um time de outro departamento (HTTP 403). */
    public void exigirTime(UsuarioAutenticado usuario, Long timeId) {
        if (!podeVer(usuario, timeId)) {
            throw new AccessDeniedException("Este departamento não está sob a sua liderança.");
        }
    }

    /**
     * Confere o departamento dono do alerta.
     *
     * @return o id do time do alerta
     */
    @Transactional(readOnly = true)
    public Long exigirAlerta(UsuarioAutenticado usuario, Long alertaId) {
        Long timeId = alertas.findById(alertaId)
                .map(a -> a.getTime().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado."));
        exigirTime(usuario, timeId);
        return timeId;
    }

    /**
     * Confere o departamento dono da recomendação.
     *
     * @return o id do time da recomendação
     */
    @Transactional(readOnly = true)
    public Long exigirRecomendacao(UsuarioAutenticado usuario, Long recomendacaoId) {
        Long timeId = recomendacoes.findById(recomendacaoId)
                .map(r -> r.getAlerta().getTime().getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Recomendação não encontrada."));
        exigirTime(usuario, timeId);
        return timeId;
    }
}
