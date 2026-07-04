package com.rizoma.pedidos.web;

import com.rizoma.pedidos.dto.Dtos.*;
import com.rizoma.pedidos.service.CatalogoService;
import com.rizoma.pedidos.service.CategoriaService;
import com.rizoma.pedidos.service.ImportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final CatalogoService catalogoService;
    private final CategoriaService categoriaService;
    private final ImportService importService;

    public AdminController(CatalogoService catalogoService, CategoriaService categoriaService,
                           ImportService importService) {
        this.catalogoService = catalogoService;
        this.categoriaService = categoriaService;
        this.importService = importService;
    }

    // ---- Categorías ----
    @GetMapping("/categorias")
    public List<CategoriaItem> listarCategorias() {
        return categoriaService.todas();
    }

    @PostMapping("/categorias")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaItem crearCategoria(@Valid @RequestBody CategoriaInput in) {
        return categoriaService.crear(in);
    }

    @PutMapping("/categorias/{id}")
    public CategoriaItem actualizarCategoria(@PathVariable Long id, @Valid @RequestBody CategoriaInput in) {
        return categoriaService.actualizar(id, in);
    }

    @DeleteMapping("/categorias/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarCategoria(@PathVariable Long id) {
        categoriaService.desactivar(id);
    }

    @GetMapping("/productos")
    public List<ProductoDTO> listar() {
        return catalogoService.todos();
    }

    @PostMapping("/productos")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductoDTO crear(@Valid @RequestBody ProductoInput in) {
        return catalogoService.crear(in);
    }

    @PutMapping("/productos/{id}")
    public ProductoDTO actualizar(@PathVariable Long id, @Valid @RequestBody ProductoInput in) {
        return catalogoService.actualizar(id, in);
    }

    @DeleteMapping("/productos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        catalogoService.desactivar(id);
    }

    /** Carga masiva desde el Excel normalizado. */
    @PostMapping("/import")
    public ImportResultDTO importar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío.");
        }
        try {
            return importService.importar(file.getInputStream());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo.");
        }
    }
}
