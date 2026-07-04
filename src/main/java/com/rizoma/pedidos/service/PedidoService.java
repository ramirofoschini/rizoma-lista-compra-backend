package com.rizoma.pedidos.service;

import com.rizoma.pedidos.domain.*;
import com.rizoma.pedidos.dto.Dtos.*;
import com.rizoma.pedidos.repo.ClienteRepository;
import com.rizoma.pedidos.repo.PedidoRepository;
import com.rizoma.pedidos.repo.PresentacionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PedidoService {

    private final PresentacionRepository presentacionRepo;
    private final ClienteRepository clienteRepo;
    private final PedidoRepository pedidoRepo;

    public PedidoService(PresentacionRepository presentacionRepo, ClienteRepository clienteRepo,
                         PedidoRepository pedidoRepo) {
        this.presentacionRepo = presentacionRepo;
        this.clienteRepo = clienteRepo;
        this.pedidoRepo = pedidoRepo;
    }

    @Transactional
    public PedidoResponse crear(CrearPedidoRequest req) {
        Cliente cliente = clienteRepo.findFirstByCelularOrderByIdDesc(req.cliente().celular())
                .orElseGet(Cliente::new);
        cliente.setNombre(req.cliente().nombre().trim());
        cliente.setApellido(req.cliente().apellido().trim());
        cliente.setDireccion(req.cliente().direccion().trim());
        cliente.setCelular(req.cliente().celular().trim());
        clienteRepo.save(cliente);

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDiaEntrega(req.diaEntrega());

        BigDecimal total = BigDecimal.ZERO;
        for (ItemPedidoDTO it : req.items()) {
            Presentacion pr = presentacionRepo.findById(it.presentacionId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Presentación inexistente: " + it.presentacionId()));
            if (!pr.isActivo() || !pr.getProducto().isActivo()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto '" + pr.getProducto().getNombre() + "' ya no está disponible.");
            }
            if (pr.getPrecio() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto '" + pr.getProducto().getNombre() + "' no tiene precio publicado; consultá por WhatsApp.");
            }
            BigDecimal subtotal = pr.getPrecio().multiply(BigDecimal.valueOf(it.cantidad()));
            PedidoItem item = new PedidoItem();
            item.setPresentacion(pr);
            item.setNombreProducto(pr.getProducto().getNombre());
            item.setEtiqueta(pr.getEtiqueta());
            item.setPrecioUnitario(pr.getPrecio());
            item.setCantidad(it.cantidad());
            item.setSubtotal(subtotal);
            pedido.addItem(item);
            total = total.add(subtotal);
        }
        pedido.setTotal(total);
        pedidoRepo.save(pedido);
        return toResponse(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        Pedido pedido = pedidoRepo.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido no encontrado"));
        return toResponse(pedido);
    }

    private PedidoResponse toResponse(Pedido p) {
        Cliente c = p.getCliente();
        List<PedidoItemResponse> items = p.getItems().stream()
                .map(i -> new PedidoItemResponse(i.getNombreProducto(), i.getEtiqueta(),
                        i.getPrecioUnitario(), i.getCantidad(), i.getSubtotal()))
                .toList();
        ClienteDTO cli = new ClienteDTO(c.getNombre(), c.getApellido(), c.getDireccion(), c.getCelular());
        return new PedidoResponse(p.getId(), p.getEstado(), p.getDiaEntrega(), p.getTotal(), cli, items, p.getCreadoEn());
    }
}
