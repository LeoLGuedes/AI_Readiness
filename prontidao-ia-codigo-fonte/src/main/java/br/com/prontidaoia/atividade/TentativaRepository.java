package br.com.prontidaoia.atividade;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TentativaRepository extends JpaRepository<Tentativa, Long> {

    boolean existsByUsuarioId(Long usuarioId);

    Optional<Tentativa> findByUsuarioIdAndAtividadeLiberadaId(Long usuarioId, Long atividadeLiberadaId);

    /** Tentativas da própria pessoa no ciclo, para montar as pendências. */
    @Query("""
            select t from Tentativa t
            where t.usuario.id = :usuarioId and t.atividadeLiberada.ciclo.id = :cicloId""")
    List<Tentativa> daPessoaNoCiclo(Long usuarioId, Long cicloId);
}
