package com.rizoma.pedidos.web;

import com.rizoma.pedidos.dto.Dtos.CategoriaDTO;
import com.rizoma.pedidos.service.CatalogoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/catalogo")
public class CatalogoController {

    private final CatalogoService catalogoService;

    public CatalogoController(CatalogoService catalogoService) {
        this.catalogoService = catalogoService;
    }

    /** Catálogo público agrupado por categoría (para el front del cliente). */
    @GetMapping
    public List<CategoriaDTO> catalogo() {
        return catalogoService.catalogoPorCategoria();
    }
}
