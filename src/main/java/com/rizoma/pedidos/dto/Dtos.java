package com.rizoma.pedidos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Contenedor de todos los DTOs de la API (records anidados). */
public final class Dtos {

    private Dtos() {}

    // ---- Catálogo (respuesta) ------------------------------------------
    public record PresentacionDTO(Long id, String etiqueta, BigDecimal precio, boolean disponible) {}

    public record ProductoDTO(Long id, String categoria, String marca, String nombre,
                              String notas, List<PresentacionDTO> presentaciones) {}

    public record CategoriaDTO(String nombre, List<ProductoDTO> productos) {}

    // ---- Cliente -------------------------------------------------------
    public record ClienteDTO(
            @NotBlank String nombre,
            @NotBlank String apellido,
            @NotBlank String direccion,
            @NotBlank @Pattern(regexp = "[0-9+\\-() ]{6,25}", message = "celular inválido") String celular) {}

    // ---- Crear pedido (request) ----------------------------------------
    public record ItemPedidoDTO(@NotNull Long presentacionId, @Min(1) int cantidad) {}

    public record CrearPedidoRequest(
            @NotNull @Valid ClienteDTO cliente,
            String diaEntrega,
            @NotEmpty @Valid List<ItemPedidoDTO> items) {}

    // ---- Pedido (respuesta) --------------------------------------------
    public record PedidoItemResponse(String nombreProducto, String etiqueta,
                                     BigDecimal precioUnitario, int cantidad, BigDecimal subtotal) {}

    public record PedidoResponse(Long id, String estado, String diaEntrega, BigDecimal total,
                                 ClienteDTO cliente, List<PedidoItemResponse> items, Instant creadoEn) {}

    // ---- Admin: alta/edición de producto -------------------------------
    public record PresentacionInput(Long id, String etiqueta, BigDecimal precio, Boolean activo) {}

    public record ProductoInput(
            @NotBlank String categoria,
            String marca,
            @NotBlank String nombre,
            String notas,
            Boolean activo,
            @Valid List<PresentacionInput> presentaciones) {}

    // ---- Import masivo -------------------------------------------------
    public record ImportResultDTO(int productosCreados, int productosActualizados,
                                  int presentaciones, List<String> avisos) {}
}
