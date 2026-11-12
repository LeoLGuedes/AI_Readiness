package br.com.prontidaoia.atividade;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AtividadeLiberadaRepository extends JpaRepository<AtividadeLiberada, Long> {

    /** O escopo do time no ciclo, em ordem de código ("4A" antes de "4B"). */
    @Query("""
            select l from AtividadeLiberada l
            join fetch l.atividade a
            where l.time.id = :timeId and l.ciclo.id = :cicloId
            order by a.codigo""")
    List<AtividadeLiberada> escopoDoTime(Long timeId, Long cicloId);

    /** O escopo de todos os times no ciclo, para copiar ao abrir o próximo. */
    @Query("""
            select l from AtividadeLiberada l
            join fetch l.atividade join fetch l.time
            where l.ciclo.id = :cicloId""")
    List<AtividadeLiberada> doCiclo(Long cicloId);

    @Query("""
            select l from AtividadeLiberada l
            join fetch l.atividade
            join fetch l.ciclo
            where l.id = :id""")
    Optional<AtividadeLiberada> comAtividadeECiclo(Long id);
}
