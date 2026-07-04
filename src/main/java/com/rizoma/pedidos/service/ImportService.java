package com.rizoma.pedidos.service;

import com.rizoma.pedidos.domain.Presentacion;
import com.rizoma.pedidos.domain.Producto;
import com.rizoma.pedidos.dto.Dtos.ImportResultDTO;
import com.rizoma.pedidos.repo.ProductoRepository;
import org.apache.poi.ss.usermodel.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;

/**
 * Carga masiva desde el Excel normalizado (una fila por presentación).
 * Columnas esperadas (encabezado en la fila 1, nombres flexibles):
 *   categoria | marca | producto | presentacion | precio | notas
 * Las filas con la misma (categoria, marca, producto) se agrupan en un producto
 * con varias presentaciones. Es un upsert: reemplaza las presentaciones del
 * producto por las del archivo.
 */
@Service
public class ImportService {

    private final ProductoRepository productoRepo;

    public ImportService(ProductoRepository productoRepo) {
        this.productoRepo = productoRepo;
    }

    @Transactional
    public ImportResultDTO importar(InputStream xlsx) {
        List<String> avisos = new ArrayList<>();
        int creados = 0, actualizados = 0, presentaciones = 0;

        try (Workbook wb = WorkbookFactory.create(xlsx)) {
            Sheet sheet = wb.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío.");
            }
            Map<String, Integer> col = mapearColumnas(header);
            requerir(col, "categoria", "producto", "presentacion", "precio");

            DataFormatter fmt = new DataFormatter();

            // índice de productos existentes por clave natural
            Map<String, Producto> existentes = new HashMap<>();
            for (Producto p : productoRepo.findAllByOrderByCategoriaAscOrdenAsc()) {
                existentes.put(clave(p.getCategoria(), p.getMarca(), p.getNombre()), p);
            }
            Map<String, Producto> tocados = new LinkedHashMap<>();

            for (int r = header.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                String categoria = str(row, col.get("categoria"), fmt);
                String marca = str(row, col.get("marca"), fmt);
                String nombre = str(row, col.get("producto"), fmt);
                String etiqueta = str(row, col.get("presentacion"), fmt);
                String notas = str(row, col.get("notas"), fmt);
                BigDecimal precio = num(row, col.get("precio"));

                if (isBlank(nombre) && isBlank(categoria)) continue; // fila vacía
                if (isBlank(nombre)) { avisos.add("Fila " + (r + 1) + ": sin nombre de producto, ignorada."); continue; }
                if (isBlank(categoria)) categoria = "SIN CATEGORIA";

                String key = clave(categoria, marca, nombre);
                Producto p = tocados.get(key);
                if (p == null) {
                    p = existentes.get(key);
                    if (p == null) { p = new Producto(); creados++; }
                    else { actualizados++; }
                    p.setCategoria(categoria.trim());
                    p.setMarca(isBlank(marca) ? null : marca.trim());
                    p.setNombre(nombre.trim());
                    p.setNotas(isBlank(notas) ? null : notas.trim());
                    p.setActivo(true);
                    p.getPresentaciones().clear();
                    tocados.put(key, p);
                }
                Presentacion pr = new Presentacion();
                pr.setEtiqueta(isBlank(etiqueta) ? null : etiqueta.trim());
                pr.setPrecio(precio);
                pr.setActivo(true);
                p.addPresentacion(pr);
                presentaciones++;
            }

            productoRepo.saveAll(tocados.values());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el Excel: " + e.getMessage());
        }
        return new ImportResultDTO(creados, actualizados, presentaciones, avisos);
    }

    private static Map<String, Integer> mapearColumnas(Row header) {
        Map<String, Integer> col = new HashMap<>();
        DataFormatter fmt = new DataFormatter();
        for (int c = header.getFirstCellNum(); c < header.getLastCellNum(); c++) {
            Cell cell = header.getCell(c);
            if (cell == null) continue;
            String name = fmt.formatCellValue(cell).trim().toLowerCase();
            if (!name.isEmpty()) col.putIfAbsent(name, c);
        }
        return col;
    }

    private static void requerir(Map<String, Integer> col, String... nombres) {
        List<String> faltan = new ArrayList<>();
        for (String n : nombres) if (!col.containsKey(n)) faltan.add(n);
        if (!faltan.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Faltan columnas en el Excel: " + String.join(", ", faltan));
        }
    }

    private static String str(Row row, Integer idx, DataFormatter fmt) {
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        return cell == null ? null : fmt.formatCellValue(cell).trim();
    }

    private static BigDecimal num(Row row, Integer idx) {
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return BigDecimal.valueOf(cell.getNumericCellValue());
        }
        String s = cell.toString().replaceAll("[^0-9.,]", "").trim();
        if (s.isEmpty()) return null;
        // separador argentino: los puntos son de miles salvo el último grupo de 2 dígitos
        s = s.replace(",", ".");
        String[] parts = s.split("\\.");
        if (parts.length > 1 && parts[parts.length - 1].length() == 2) {
            String ent = String.join("", Arrays.copyOf(parts, parts.length - 1));
            s = ent + "." + parts[parts.length - 1];
        } else {
            s = String.join("", parts);
        }
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }

    private static String clave(String categoria, String marca, String nombre) {
        return (n(categoria) + "||" + n(marca) + "||" + n(nombre)).toLowerCase();
    }

    private static String n(String s) { return s == null ? "" : s.trim(); }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }
}
