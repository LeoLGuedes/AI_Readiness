package br.com.prontidaoia.diagnostico.indicador;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface IndicadorRepository extends JpaRepository<IndicadorTimeCiclo, Long> {

    /** A série do time, em ordem de ciclo. */
    @Query("""
            select i from IndicadorTimeCiclo i join fetch i.ciclo c
            where i.time.id = :timeId order by c.numero""")
    List<IndicadorTimeCiclo> serieDoTime(Long timeId);

    Optional<IndicadorTimeCiclo> findByTimeIdAndCicloId(Long timeId, Long cicloId);

    List<IndicadorTimeCiclo> findByCicloId(Long cicloId);
}
