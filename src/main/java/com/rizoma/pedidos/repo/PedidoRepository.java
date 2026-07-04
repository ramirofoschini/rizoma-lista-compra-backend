package com.rizoma.pedidos.repo;

import com.rizoma.pedidos.domain.Pedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    @EntityGraph(attributePaths = {"items", "cliente"})
    Optional<Pedido> findWithItemsById(Long id);

    @EntityGraph(attributePaths = {"cliente"})
    List<Pedido> findAllByOrderByCreadoEnDesc();
}
