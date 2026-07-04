package com.rizoma.pedidos.repo;

import com.rizoma.pedidos.domain.Producto;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    /** Activos, ordenados por orden de categoría y luego orden del producto (para el catálogo). */
    @EntityGraph(attributePaths = {"presentaciones", "categoria"})
    @Query("select p from Producto p where p.activo = true and p.categoria.activa = true "
            + "order by p.categoria.orden asc, p.orden asc")
    List<Producto> findActivosOrdenados();

    /** Todos (incluye inactivos), para el panel admin y la importación. */
    @EntityGraph(attributePaths = {"presentaciones", "categoria"})
    @Query("select p from Producto p order by p.categoria.orden asc, p.orden asc")
    List<Producto> findTodosOrdenados();
}
