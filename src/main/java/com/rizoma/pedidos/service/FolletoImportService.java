package com.rizoma.pedidos.service;

import com.rizoma.pedidos.domain.Categoria;
import com.rizoma.pedidos.domain.Presentacion;
import com.rizoma.pedidos.domain.Producto;
import com.rizoma.pedidos.dto.Dtos.*;
import com.rizoma.pedidos.repo.CategoriaRepository;
import com.rizoma.pedidos.repo.ProductoRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importa el FOLLETO ESTILADO de Rizoma (el que reciben los clientes): la jerarquía
 * está codificada por el color de relleno de la celda A (rojo=categoría, naranja=marca,
 * resto=producto) y el precio viene mezclado con la presentación en texto libre.
 *
 * Ofrece una PREVISUALIZACIÓN (analizar) que devuelve qué entendió + avisos, sin tocar
 * la base; y la importación (importar) que hace el upsert.
 */
@Service
public class FolletoImportService {

    private static final Set<String> RED = Set.of("CC4125", "E25420", "E1541F", "A61C00", "CC0000", "FF0000");
    private static final Set<String> ORANGE = Set.of("E69138", "F6B26B", "FFD966");
    private static final Pattern PRICE = Pattern.compile("\\$\\s*(\\d+(?:\\.\\d+)*)");
    private static final Pattern TRAILING = Pattern.compile("(?<![\\d/])(\\d+(?:\\.\\d+)*)\\s*$");
    private static final Pattern SUSPECT = Pattern.compile("\\dO|O\\d");

    private final ProductoRepository productoRepo;
    private final CategoriaRepository categoriaRepo;

    public FolletoImportService(ProductoRepository productoRepo, CategoriaRepository categoriaRepo) {
        this.productoRepo = productoRepo;
        this.categoriaRepo = categoriaRepo;
    }

    // ---- Resultado interno del parseo ----
    private record Parseo(List<ItemPreview> items, List<String> avisos, int categorias, int marcas) {}

    @Transactional(readOnly = true)
    public FolletoPreview analizar(InputStream xlsx) {
        Parseo p = parsear(xlsx);
        int presentaciones = p.items().stream().mapToInt(i -> i.presentaciones().size()).sum();
        return new FolletoPreview(p.categorias(), p.marcas(), p.items().size(), presentaciones, p.items(), p.avisos());
    }

    @Transactional
    public ImportResultDTO importar(InputStream xlsx) {
        Parseo p = parsear(xlsx);

        Map<String, Producto> existentes = new HashMap<>();
        for (Producto pr : productoRepo.findTodosOrdenados()) {
            existentes.put(clave(pr.getCategoria().getNombre(), pr.getMarca(), pr.getNombre()), pr);
        }
        Map<String, Categoria> catCache = new HashMap<>();
        for (Categoria c : categoriaRepo.findAll()) catCache.put(c.getNombre().trim().toLowerCase(), c);

        int creados = 0, actualizados = 0, presentaciones = 0;
        Map<String, Producto> tocados = new LinkedHashMap<>();
        for (ItemPreview it : p.items()) {
            String key = clave(it.categoria(), it.marca(), it.nombre());
            Producto prod = tocados.get(key);
            if (prod == null) {
                prod = existentes.get(key);
                if (prod == null) { prod = new Producto(); creados++; }
                else { actualizados++; }
                prod.setCategoria(resolverCategoria(it.categoria(), catCache));
                prod.setMarca(blankToNull(it.marca()));
                prod.setNombre(it.nombre());
                prod.setNotas(blankToNull(it.notas()));
                prod.setActivo(true);
                prod.getPresentaciones().clear();
                tocados.put(key, prod);
            }
            for (PresPreview pp : it.presentaciones()) {
                Presentacion pr = new Presentacion();
                pr.setEtiqueta(blankToNull(pp.etiqueta()));
                pr.setPrecio(pp.precio());
                pr.setActivo(true);
                prod.addPresentacion(pr);
                presentaciones++;
            }
        }
        productoRepo.saveAll(tocados.values());
        return new ImportResultDTO(creados, actualizados, presentaciones, p.avisos());
    }

    // ---- Parseo del folleto ----
    private Parseo parsear(InputStream xlsx) {
        List<ItemPreview> items = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Set<String> categorias = new LinkedHashSet<>();
        Set<String> marcas = new LinkedHashSet<>();
        DataFormatter fmt = new DataFormatter();

        try (Workbook wb = WorkbookFactory.create(xlsx)) {
            for (Sheet sheet : wb) {
                String curCat = "", curMarca = "";
                for (Row row : sheet) {
                    if (row == null) continue;
                    int nCols = Math.max(row.getLastCellNum(), 1);
                    List<String> vals = new ArrayList<>();
                    boolean hayNumerico = false;
                    for (int c = 0; c < nCols; c++) {
                        Cell cell = row.getCell(c);
                        vals.add(cell == null ? "" : fmt.formatCellValue(cell).trim());
                        if (c > 0 && cell != null && cell.getCellType() == CellType.NUMERIC) hayNumerico = true;
                    }
                    String a = vals.isEmpty() ? "" : vals.get(0);
                    String resto = String.join(" ", vals.subList(Math.min(1, vals.size()), vals.size()));
                    String joined = String.join(" ", vals);

                    if (a.isBlank() && resto.isBlank()) continue;
                    if (esBoilerplate(a, joined)) continue;

                    boolean hayPrecio = PRICE.matcher(resto).find() || hayNumerico;
                    Cell aCell = row.getCell(0);
                    String fill = fillHex(aCell);
                    boolean bold = esBold(aCell);
                    double size = tamanio(aCell);
                    int fila = row.getRowNum() + 1;

                    String kind = clasificar(hayPrecio, fill, bold, size);
                    if ("CAT".equals(kind)) { curCat = a; curMarca = ""; categorias.add(a); continue; }
                    if ("BRAND".equals(kind)) { curMarca = a; marcas.add(a); continue; }

                    // PRODUCTO
                    List<PresPreview> pres = new ArrayList<>();
                    List<String> notas = new ArrayList<>();
                    List<String> flags = new ArrayList<>();
                    for (int c = 1; c < vals.size(); c++) {
                        Cell cell = row.getCell(c);
                        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
                            pres.add(new PresPreview("", BigDecimal.valueOf(cell.getNumericCellValue())));
                        } else {
                            parsearCelda(vals.get(c), pres, notas, flags);
                        }
                    }
                    String cat = curCat.isBlank() ? "SIN CATEGORIA" : curCat;
                    if (curCat.isBlank()) flags.add("producto sin categoría arriba");
                    if (pres.isEmpty()) {
                        if (fill == null && !bold) {
                            avisos.add("Fila " + fila + " (" + a + "): sin precio y sin color — ¿es un título al que le falta color?");
                        } else {
                            flags.add("producto sin precio");
                        }
                    }
                    for (String f : flags) avisos.add("Fila " + fila + " (" + recorta(a) + "): " + f);
                    String notasStr = String.join(" | ", new LinkedHashSet<>(notas));
                    items.add(new ItemPreview(cat, curMarca.isBlank() ? null : curMarca, a, pres, notasStr.isBlank() ? null : notasStr));
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el folleto: " + e.getMessage());
        }
        return new Parseo(items, avisos, categorias.size(), marcas.size());
    }

    private void parsearCelda(String text, List<PresPreview> pres, List<String> notas, List<String> flags) {
        if (text == null || text.isBlank()) return;
        Matcher m = PRICE.matcher(text);
        List<int[]> spans = new ArrayList<>();
        List<String> nums = new ArrayList<>();
        while (m.find()) { spans.add(new int[]{m.start(), m.end()}); nums.add(m.group(1)); }
        if (!spans.isEmpty()) {
            if (spans.size() == 1) {
                String label = limpiar(text.substring(0, spans.get(0)[0]) + " " + text.substring(spans.get(0)[1]));
                pres.add(new PresPreview(label, parsePrecio(nums.get(0))));
            } else {
                int prev = 0;
                for (int i = 0; i < spans.size(); i++) {
                    String label = limpiar(text.substring(prev, spans.get(i)[0]));
                    pres.add(new PresPreview(label, parsePrecio(nums.get(i))));
                    prev = spans.get(i)[1];
                }
            }
            if (SUSPECT.matcher(text).find()) flags.add("precio con caracter sospechoso (¿O en vez de 0?)");
            return;
        }
        Matcher t = TRAILING.matcher(text);
        if (t.find() && parsePrecio(t.group(1)).compareTo(BigDecimal.valueOf(100)) >= 0) {
            String label = limpiar(text.substring(0, t.start()));
            pres.add(new PresPreview(label, parsePrecio(t.group(1))));
            flags.add("precio sin $ (revisar)");
            return;
        }
        notas.add(text.trim());
    }

    private static BigDecimal parsePrecio(String s) {
        s = s.trim();
        if (!s.contains(".")) return new BigDecimal(s);
        String[] parts = s.split("\\.");
        String last = parts[parts.length - 1];
        if (last.length() == 2) {
            StringBuilder ent = new StringBuilder();
            for (int i = 0; i < parts.length - 1; i++) ent.append(parts[i]);
            return new BigDecimal(ent + "." + last);
        }
        StringBuilder all = new StringBuilder();
        for (String p : parts) all.append(p);
        return new BigDecimal(all.toString());
    }

    private static String clasificar(boolean hayPrecio, String fill, boolean bold, double size) {
        if (hayPrecio) return "PROD";
        if (fill != null && RED.contains(fill)) return "CAT";
        if (fill != null && ORANGE.contains(fill)) return "BRAND";
        if (bold && size >= 14) return "CAT";
        if (bold && size >= 12) return "BRAND";
        return "PROD";
    }

    private static String fillHex(Cell cell) {
        if (cell == null) return null;
        CellStyle cs = cell.getCellStyle();
        if (!(cs instanceof XSSFCellStyle xcs)) return null;
        if (xcs.getFillPattern() != FillPatternType.SOLID_FOREGROUND) return null;
        XSSFColor color = xcs.getFillForegroundColorColor();
        if (color == null) return null;
        String argb = color.getARGBHex();
        if (argb == null || argb.length() < 6) return null;
        return argb.substring(argb.length() - 6).toUpperCase();
    }

    private static boolean esBold(Cell cell) {
        if (cell == null) return false;
        try {
            Font f = cell.getSheet().getWorkbook().getFontAt(cell.getCellStyle().getFontIndex());
            return f.getBold();
        } catch (Exception e) { return false; }
    }

    private static double tamanio(Cell cell) {
        if (cell == null) return 11;
        try {
            Font f = cell.getSheet().getWorkbook().getFontAt(cell.getCellStyle().getFontIndex());
            return f.getFontHeightInPoints();
        } catch (Exception e) { return 11; }
    }

    private static boolean esBoilerplate(String a, String joined) {
        String up = a.trim().toUpperCase();
        return up.equals("RIZOMA") || up.equals("BIO ALMACEN ORGANICO")
                || joined.toUpperCase().contains("ENVIOS A DOMICILIO")
                || joined.toUpperCase().contains("ESCRIBIR AL WSP");
    }

    private static String limpiar(String s) {
        s = s.trim();
        while (s.startsWith("/") || s.startsWith("-")) s = s.substring(1).trim();
        while (s.endsWith("/") || s.endsWith("-")) s = s.substring(0, s.length() - 1).trim();
        return s.replaceAll("\\s+", " ").trim();
    }

    private static String recorta(String s) { return s.length() > 40 ? s.substring(0, 40) : s; }

    private Categoria resolverCategoria(String nombre, Map<String, Categoria> cache) {
        String key = nombre.trim().toLowerCase();
        Categoria c = cache.get(key);
        if (c != null) return c;
        c = new Categoria();
        c.setNombre(nombre.trim());
        int max = cache.values().stream().map(Categoria::getOrden)
                .filter(Objects::nonNull).mapToInt(Integer::intValue).max().orElse(0);
        c.setOrden(max + 1);
        c.setActiva(true);
        c = categoriaRepo.save(c);
        cache.put(key, c);
        return c;
    }

    private static String clave(String categoria, String marca, String nombre) {
        return (n(categoria) + "||" + n(marca) + "||" + n(nombre)).toLowerCase();
    }

    private static String n(String s) { return s == null ? "" : s.trim(); }

    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
}
