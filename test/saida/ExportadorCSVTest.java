package saida;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import saida.ExportadorCSV.Resultado;

/**
 * Testes da classe ExportadorCSV.
 *
 * Cuidados:
 *  - Todos os arquivos são gravados em @TempDir; NUNCA chamamos exportar(lista) sem
 *    caminho, pois isso gravaria resultados.csv na raiz do projeto.
 *  - O Locale padrão é salvo antes e RESTAURADO depois de cada teste (mesmo se falhar),
 *    para um teste não contaminar os outros. Antes de cada teste fixamos pt-BR, para
 *    que o resultado não dependa do idioma do computador.
 *  - Os testes de injeção leem o arquivo com um parser CSV estrito (RFC 4180, separador ';'),
 *    o mesmo tipo de leitura que Excel/LibreOffice/pandas fazem.
 */
@DisplayName("ExportadorCSV")
class ExportadorCSVTest {

    private static final String CABECALHO = "algoritmo;tamanho;disposicao;tempo_ms";

    private final ExportadorCSV exportador = new ExportadorCSV();

    @TempDir
    Path dir;

    private Locale localePadrao;
    private Locale localeFormato;
    private Locale localeExibicao;

    @BeforeEach
    void salvarLocale() {
        localePadrao = Locale.getDefault();
        localeFormato = Locale.getDefault(Locale.Category.FORMAT);
        localeExibicao = Locale.getDefault(Locale.Category.DISPLAY);
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
    }

    @AfterEach
    void restaurarLocale() {
        Locale.setDefault(localePadrao);
        Locale.setDefault(Locale.Category.FORMAT, localeFormato);
        Locale.setDefault(Locale.Category.DISPLAY, localeExibicao);
    }

    // -----------------------------------------------------------------------
    // Auxiliares
    // -----------------------------------------------------------------------

    /** Exporta um único resultado para um arquivo novo e devolve o caminho. */
    private Path exportarUm(String algoritmo, int tamanho, String disposicao, double tempo) throws IOException {
        Path destino = dir.resolve("r.csv");
        exportador.exportar(List.of(new Resultado(algoritmo, tamanho, disposicao, tempo)), destino.toString());
        return destino;
    }

    /** Lê as linhas físicas do arquivo (UTF-8). */
    private static List<String> linhas(Path arquivo) throws IOException {
        return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
    }

    /** Devolve o campo tempo_ms (4ª coluna) da primeira linha de dados. */
    private static String campoTempo(Path arquivo) throws IOException {
        return linhas(arquivo).get(1).split(";", -1)[3];
    }

    /**
     * Parser CSV estrito (RFC 4180) com separador ';'.
     * Campo iniciado por aspas é "citado": "" vira ", e ';', CR e LF dentro das aspas
     * fazem parte do campo. Registros são separados por CRLF ou LF.
     */
    private static List<List<String>> lerCsvRfc4180(Path arquivo) throws IOException {
        String s = Files.readString(arquivo, StandardCharsets.UTF_8);
        List<List<String>> registros = new ArrayList<>();
        int n = s.length();
        int i = 0;
        while (i < n) {
            List<String> registro = new ArrayList<>();
            while (true) {
                StringBuilder campo = new StringBuilder();
                if (i < n && s.charAt(i) == '"') {
                    i++;
                    while (true) {
                        if (i >= n) {
                            fail("CSV inválido: aspas não fechadas no registro " + (registros.size() + 1));
                        }
                        char c = s.charAt(i);
                        if (c == '"') {
                            if (i + 1 < n && s.charAt(i + 1) == '"') {
                                campo.append('"');
                                i += 2;
                            } else {
                                i++;
                                break;
                            }
                        } else {
                            campo.append(c);
                            i++;
                        }
                    }
                    if (i < n && s.charAt(i) != ';' && s.charAt(i) != '\r' && s.charAt(i) != '\n') {
                        fail("CSV inválido: caractere após aspas de fechamento na posição " + i);
                    }
                } else {
                    while (i < n && s.charAt(i) != ';' && s.charAt(i) != '\r' && s.charAt(i) != '\n') {
                        campo.append(s.charAt(i));
                        i++;
                    }
                }
                registro.add(campo.toString());
                if (i < n && s.charAt(i) == ';') {
                    i++;
                    continue;
                }
                // Fim do registro: consome CRLF ou LF.
                if (i < n && s.charAt(i) == '\r') {
                    i++;
                }
                if (i < n && s.charAt(i) == '\n') {
                    i++;
                }
                break;
            }
            registros.add(registro);
        }
        return registros;
    }

    /** Exporta um resultado e confere que, relido como CSV, o campo indicado volta idêntico. */
    private void verificarCampoPreservado(String algoritmo, String disposicao, int indice, String esperado)
            throws IOException {
        List<List<String>> registros = lerCsvRfc4180(exportarUm(algoritmo, 10, disposicao, 1.5));
        assertEquals(2, registros.size(), "esperado cabeçalho + 1 registro, obtido: " + registros);
        assertEquals(4, registros.get(1).size(), "registro de dados desalinhado: " + registros.get(1));
        assertEquals(esperado, registros.get(1).get(indice));
    }

    // -----------------------------------------------------------------------
    // Bordas aceitáveis
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Lista vazia grava só o cabeçalho exato")
    void listaVaziaSoCabecalho() throws IOException {
        Path destino = dir.resolve("vazio.csv");
        exportador.exportar(List.of(), destino.toString());
        assertEquals(List.of(CABECALHO), linhas(destino));
    }

    @Test
    @DisplayName("Um resultado gera a linha 'Bubble Sort;10;aleatorio;1,500'")
    void umResultado() throws IOException {
        assertEquals(List.of(CABECALHO, "Bubble Sort;10;aleatorio;1,500"),
                linhas(exportarUm("Bubble Sort", 10, "aleatorio", 1.5)));
    }

    @Test
    @DisplayName("Ordem dos resultados é preservada e 10.000 resultados geram 10.001 linhas")
    void ordemEVolume() throws IOException {
        Path destino = dir.resolve("ordem.csv");
        exportador.exportar(List.of(
                new Resultado("A", 1, "x", 1),
                new Resultado("B", 2, "x", 2),
                new Resultado("C", 3, "x", 3),
                new Resultado("D", 4, "x", 4)), destino.toString());
        List<String> l = linhas(destino);
        assertEquals(5, l.size());
        assertAll(
                () -> assertTrue(l.get(1).startsWith("A;")),
                () -> assertTrue(l.get(2).startsWith("B;")),
                () -> assertTrue(l.get(3).startsWith("C;")),
                () -> assertTrue(l.get(4).startsWith("D;")));

        List<Resultado> muitos = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            muitos.add(new Resultado("Alg", i, "x", i));
        }
        Path grande = dir.resolve("grande.csv");
        exportador.exportar(muitos, grande.toString());
        assertEquals(10_001, linhas(grande).size());
    }

    @Test
    @DisplayName("Sobrescreve (trunca) um arquivo existente com conteúdo maior")
    void sobrescreveArquivoExistente() throws IOException {
        Path destino = dir.resolve("r.csv");
        Files.writeString(destino, "lixo antigo\n".repeat(1000), StandardCharsets.UTF_8);
        exportarUm("Q", 1, "x", 1);
        assertEquals(List.of(CABECALHO, "Q;1;x;1,000"), linhas(destino));
    }

    @Test
    @DisplayName("Acentos são gravados e relidos corretamente em UTF-8")
    void acentosEmUtf8() throws IOException {
        assertEquals("Ordenação;5;disposição;2,000", linhas(exportarUm("Ordenação", 5, "disposição", 2.0)).get(1));
    }

    @Test
    @DisplayName("Locale en-US e pt-BR: os dois gravam '1,500' (a troca '.' por ',' cobre ambos)")
    void localeUsEPtBr() throws IOException {
        Locale.setDefault(Locale.US);
        assertEquals("1,500", campoTempo(exportarUm("A", 1, "x", 1.5)));
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));
        assertEquals("1,500", campoTempo(exportarUm("A", 1, "x", 1.5)));
    }

    @Test
    @DisplayName("Sem separador de milhar; zero, tempo negativo e tamanho negativo são gravados sem validação")
    void valoresDeBordaNumericos() throws IOException {
        assertEquals("1234567,891", campoTempo(exportarUm("A", 1, "x", 1234567.891)));
        assertEquals("0,000", campoTempo(exportarUm("A", 1, "x", 0.0)));
        // Documenta: não há validação (o Cronometro nunca produz valores negativos).
        assertEquals("A;-1;x;-1,500", linhas(exportarUm("A", -1, "x", -1.5)).get(1));
    }

    @Test
    @DisplayName("Caminho em diretório inexistente lança NoSuchFileException")
    void diretorioInexistente() {
        String caminho = dir.resolve("nao").resolve("existe").resolve("r.csv").toString();
        assertThrows(NoSuchFileException.class, () -> exportador.exportar(List.of(), caminho));
    }

    @Test
    @DisplayName("Caminho null lança NullPointerException")
    void caminhoNulo() {
        assertThrows(NullPointerException.class, () -> exportador.exportar(List.of(), null));
    }

    @Test
    @DisplayName("Lista null lança NullPointerException (lançar é aceitável)")
    void listaNula() {
        assertThrows(NullPointerException.class, () -> exportador.exportar(null, dir.resolve("n.csv").toString()));
    }

    @Test
    @DisplayName("Elemento null dentro da lista lança NullPointerException")
    void elementoNulo() {
        List<Resultado> comNulo = new ArrayList<>();
        comNulo.add(null);
        assertThrows(NullPointerException.class, () -> exportador.exportar(comNulo, dir.resolve("n.csv").toString()));
    }

    // -----------------------------------------------------------------------
    // Regressão: defeitos corrigidos
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Lista null lança exceção sem apagar (truncar) o arquivo existente")
    void listaNulaNaoPodeDestruirArquivo() throws IOException {
        // Justificativa: uma chamada inválida não deveria destruir dados já gravados;
        // o argumento deve ser validado ANTES de abrir o arquivo para escrita.
        Path destino = dir.resolve("antigo.csv");
        Files.writeString(destino, "conteudo-antigo", StandardCharsets.UTF_8);
        assertThrows(Exception.class, () -> exportador.exportar(null, destino.toString()));
        assertEquals("conteudo-antigo", Files.readString(destino, StandardCharsets.UTF_8));
    }

    // Justificativa (injeção de CSV): campos de texto devem ser escapados como na RFC 4180
    // (entre aspas quando contêm ';', '"', CR ou LF, com '"' duplicada). Antes da correção,
    // um nome com ';' ou quebra de linha desalinhava as colunas ou criava registros falsos.

    @Test
    @DisplayName("Algoritmo com ';' é gravado entre aspas e relido igual (RFC 4180)")
    void injecaoPontoEVirgulaNoAlgoritmo() throws IOException {
        verificarCampoPreservado("Bubble;Sort", "aleatorio", 0, "Bubble;Sort");
    }

    @Test
    @DisplayName("Disposição com ';' é gravada entre aspas e relida igual (RFC 4180)")
    void injecaoPontoEVirgulaNaDisposicao() throws IOException {
        verificarCampoPreservado("Bubble Sort", "ale;atorio", 2, "ale;atorio");
    }

    @Test
    @DisplayName("Algoritmo com quebra de linha LF é gravado entre aspas e não cria registro falso")
    void injecaoQuebraDeLinhaLf() throws IOException {
        verificarCampoPreservado("Bubble\nSort", "aleatorio", 0, "Bubble\nSort");
    }

    @Test
    @DisplayName("Algoritmo com quebra de linha CRLF é gravado entre aspas e não cria registro falso")
    void injecaoQuebraDeLinhaCrlf() throws IOException {
        verificarCampoPreservado("Bubble\r\nSort", "aleatorio", 0, "Bubble\r\nSort");
    }

    @Test
    @DisplayName("Algoritmo com aspas '\"Bubble\"' tem as aspas duplicadas e é relido igual")
    void injecaoAspas() throws IOException {
        verificarCampoPreservado("\"Bubble\"", "aleatorio", 0, "\"Bubble\"");
    }

    @ParameterizedTest(name = "algoritmo = {0}")
    @ValueSource(strings = {"=HYPERLINK(\"http://exemplo.invalid\",\"x\")", "@SUM(1+1)"})
    @DisplayName("Nome começando com '=' ou '@' é neutralizado com prefixo ' (CSV injection)")
    void injecaoDeFormula(String algoritmo) throws IOException {
        // Justificativa: OWASP "CSV Injection": Excel/LibreOffice executam campos que começam
        // com =, +, - ou @. Campos de texto devem ser neutralizados antes de gravar.
        List<List<String>> registros = lerCsvRfc4180(exportarUm(algoritmo, 10, "aleatorio", 1.5));
        assertEquals(2, registros.size(), "registros: " + registros);
        String campo = registros.get(1).get(0);
        assertFalse(campo.startsWith("=") || campo.startsWith("+") || campo.startsWith("-") || campo.startsWith("@"),
                "campo gravado como fórmula: " + campo);
    }

    @Test
    @DisplayName("Algoritmo null é rejeitado (não grava o texto 'null')")
    void algoritmoNulo() throws IOException {
        // Justificativa: a palavra 'null' no relatório parece o nome de um algoritmo e
        // esconde o erro de quem chamou.
        Path destino = dir.resolve("r.csv");
        try {
            exportador.exportar(List.of(new Resultado(null, 1, "x", 1.0)), destino.toString());
        } catch (NullPointerException | IllegalArgumentException e) {
            return; // rejeitar é aceitável
        }
        assertNotEquals("null", linhas(destino).get(1).split(";", -1)[0]);
    }

    @ParameterizedTest(name = "tempo = {0}")
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    @DisplayName("Tempo NaN/Infinity é rejeitado com IllegalArgumentException")
    void tempoNaoFinito(double tempo) throws IOException {
        // Justificativa: 'NaN'/'Infinity' não é número para a planilha e quebra gráficos/médias.
        Path destino = dir.resolve("r.csv");
        try {
            exportador.exportar(List.of(new Resultado("A", 1, "x", tempo)), destino.toString());
        } catch (IllegalArgumentException e) {
            return; // rejeitar é aceitável
        }
        String campo = campoTempo(destino);
        assertTrue(campo.matches("^-?\\d+,\\d{3}$"), "campo tempo não numérico: " + campo);
    }

    @ParameterizedTest(name = "Locale = {0}")
    @ValueSource(strings = {"ar-SA", "th-TH-u-nu-thai"})
    @DisplayName("Em Locale árabe/tailandês o tempo sai em ASCII: '1,500'")
    void localeComDigitosNaoAscii(String tag) throws IOException {
        // Justificativa: o arquivo é para máquina/planilha; o formato não pode depender do
        // idioma do computador. Deve-se formatar com um Locale fixo.
        Locale.setDefault(Locale.forLanguageTag(tag));
        assertEquals("1,500", campoTempo(exportarUm("A", 1, "x", 1.5)));
    }
}
