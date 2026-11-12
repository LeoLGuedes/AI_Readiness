package br.com.prontidaoia.diagnostico.alerta;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlertaRepository extends JpaRepository<Alerta, Long> {

    @Query("select a from Alerta a where a.time.id = :timeId and a.status <> 'RESOLVIDO'")
    List<Alerta> naoResolvidosDoTime(Long timeId);

    @Query("select a from Alerta a where a.time.id = :timeId and a.tipo = :tipo and a.status <> 'RESOLVIDO'")
    Optional<Alerta> naoResolvidoDoTipo(Long timeId, TipoAlerta tipo);

    List<Alerta> findByTimeIdOrderById(Long timeId);

    List<Alerta> findByStatus(StatusAlerta status);

    /**
     * Alertas que estavam valendo no fechamento do ciclo {@code numero}:
     * nasceram até ele e não tinham sido resolvidos antes dele.
     */
    @Query("""
            select a from Alerta a join fetch a.ciclo c left join fetch a.cicloResolucao r
            where a.time.id = :timeId and c.numero <= :numero
              and (r is null or r.numero >= :numero)
            order by a.id""")
    List<Alerta> vigentesNoCiclo(Long timeId, int numero);
}
