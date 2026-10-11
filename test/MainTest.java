import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Testes do menu de console (classe Main, pacote default).
 *
 * Como funciona:
 *  - System.in é trocado por um texto pronto (simulando o que o usuário digitaria)
 *    e System.out é capturado em memória. Os dois são RESTAURADOS depois de cada teste.
 *  - O Main grava resultados.csv no diretório de trabalho. Se já existir um, ele é
 *    renomeado para resultados.csv.bak-teste (na MESMA pasta, fora do @TempDir, para
 *    nunca ser apagado junto com a pasta temporária) e devolvido no fim do teste,
 *    mesmo que a limpeza falhe (try/finally). O arquivo criado pelo teste é apagado.
 *  - Nunca usamos System.exit.
 *
 * Política dos testes de entrada inválida: entrada inválida do usuário (texto onde se espera número,
 * N negativo, linha vazia, fim da entrada) NÃO deve derrubar o programa com uma exceção
 * nem gerar resultados.csv. Não exigimos um texto específico de mensagem.
 *
 * Política de "entrada sem dados":
 *  - Opção 1 com N = 0: o usuário PEDIU explicitamente 0 elementos. Rodar e gravar
 *    tamanho 0 é aceitável (documentado, passa).
 *  - Opção 3 com arquivo que não rende nenhum valor: o usuário esperava os dados do
 *    arquivo e recebeu 0 sem saber por quê (arquivo errado, só cabeçalho, linhas
 *    descartadas). Nesse caso o programa avisa e não grava. A validação fica SÓ no case "3",
 *    logo depois da leitura, para não barrar o N = 0 da opção 1.
 */
@DisplayName("Main (menu de console)")
class MainTest {

    private static final String CABECALHO = "algoritmo;tamanho;disposicao;tempo_ms";

    @TempDir
    Path dir;

    private InputStream inOriginal;
    private PrintStream outOriginal;
    private PrintStream errOriginal;
    private Locale localeOriginal;
    private Locale localeFormato;
    private Locale localeExibicao;
    private Path csv;
    private Path backup;

    @BeforeEach
    void preparar() throws IOException {
        inOriginal = System.in;
        outOriginal = System.out;
        errOriginal = System.err;
        localeOriginal = Locale.getDefault();
        localeFormato = Locale.getDefault(Locale.Category.FORMAT);
        localeExibicao = Locale.getDefault(Locale.Category.DISPLAY);
        // Fixa pt-BR para o resultado não depender do idioma do computador.
        Locale.setDefault(Locale.forLanguageTag("pt-BR"));

        csv = Path.of("resultados.csv").toAbsolutePath();
        backup = null;
        if (Files.exists(csv) && Files.isRegularFile(csv)) {
            // Backup ao lado do original (não no @TempDir). Sem REPLACE_EXISTING: se sobrou
            // um backup de uma execução interrompida, o move falha e nada é sobrescrito.
            Path destino = csv.resolveSibling("resultados.csv.bak-teste");
            Files.move(csv, destino);
            backup = destino;
        }
        assertFalse(Files.exists(csv), "resultados.csv não deveria existir no início do teste");
    }

    @AfterEach
    void restaurar() throws IOException {
        System.setIn(inOriginal);
        System.setOut(outOriginal);
        System.setErr(errOriginal);
        Locale.setDefault(localeOriginal);
        Locale.setDefault(Locale.Category.FORMAT, localeFormato);
        Locale.setDefault(Locale.Category.DISPLAY, localeExibicao);

        try {
            apagar(csv);
        } finally {
            // Roda mesmo se apagar() falhar (ex.: resultados.csv aberto no Excel).
            // Se o move também falhar, o backup continua em resultados.csv.bak-teste.
            if (backup != null) {
                Files.move(backup, csv, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    /** Apaga arquivo ou diretório (recursivamente), se existir. */
    private static void apagar(Path p) throws IOException {
        if (!Files.exists(p)) {
            return;
        }
        try (Stream<Path> s = Files.walk(p)) {
            for (Path item : s.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(item);
            }
        }
    }

    /** Executa o Main com a entrada informada e devolve tudo o que foi impresso. */
    private String rodarMain(String entrada) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        System.setIn(new ByteArrayInputStream(entrada.getBytes(StandardCharsets.UTF_8)));
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            Main.main(new String[0]);
        } finally {
            System.setOut(outOriginal);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }

    /** Versão para os testes de entrada inválida: o Main não pode lançar exceção. */
    private String rodarMainSemQuebrar(String entrada) {
        return assertDoesNotThrow(() -> rodarMain(entrada),
                "o programa quebrou com exceção para a entrada "
                        + (entrada.isEmpty() ? "(vazia/EOF)" : entrada.replace("\n", "\\n")));
    }

    /** Confere o resultados.csv: cabeçalho + 4 algoritmos na ordem, com tamanho e disposição esperados. */
    private void verificarCsv(int tamanho, String disposicao) throws IOException {
        assertTrue(Files.isRegularFile(csv), "resultados.csv não foi criado");
        List<String> linhas = Files.readAllLines(csv, StandardCharsets.UTF_8);
        assertEquals(5, linhas.size(), "linhas: " + linhas);
        assertEquals(CABECALHO, linhas.get(0));
        String[] nomes = {"Bubble Sort", "Insertion Sort", "Merge Sort", "Quick Sort"};
        for (int i = 0; i < nomes.length; i++) {
            String[] campos = linhas.get(i + 1).split(";", -1);
            int linha = i + 1;
            assertAll("linha " + linha,
                    () -> assertEquals(4, campos.length),
                    () -> assertEquals(nomes[linha - 1], campos[0]),
                    () -> assertEquals(String.valueOf(tamanho), campos[1]),
                    () -> assertEquals(disposicao, campos[2]),
                    () -> assertTrue(campos[3].matches("^\\d+,\\d{3}$"), "tempo: " + campos[3]));
        }
    }

    private String criarCsvEntrada(String nome, byte[] bytes) throws IOException {
        Path p = dir.resolve(nome);
        Files.write(p, bytes);
        return p.toAbsolutePath().toString();
    }

    // -----------------------------------------------------------------------
    // Bordas aceitáveis
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Opção 1 com N = 10 grava resultados.csv com os 4 algoritmos")
    void opcao1Normal() throws IOException {
        String saida = rodarMain("1\n10\n");
        assertTrue(saida.contains("Resultados gravados em resultados.csv"), saida);
        verificarCsv(10, "aleatorio");
    }

    @Test
    @DisplayName("Opção ' 1 ' e N ' 5 ' com espaços em volta funcionam (trim)")
    void opcao1ComEspacos() throws IOException {
        rodarMain(" 1 \n 5 \n");
        verificarCsv(5, "aleatorio");
    }

    @Test
    @DisplayName("Opção 1 com N = 0 roda e grava 4 linhas com tamanho 0 (documenta: o usuário pediu 0 explicitamente)")
    void opcao1NZero() throws IOException {
        // Diferente da opção 3 sem dados (ver opcao3ArquivoSemLinhasValidas): aqui o 0 foi
        // escolha consciente do usuário, então gravar tamanho 0 é aceitável.
        rodarMain("1\n0\n");
        verificarCsv(0, "aleatorio");
    }

    @Test
    @DisplayName("Opção 2 com '3 1 2' grava tamanho 3 e disposição 'manual'")
    void opcao2Normal() throws IOException {
        rodarMain("2\n3 1 2\n");
        verificarCsv(3, "manual");
    }

    @Test
    @DisplayName("Opção 2 aceita espaços múltiplos e TAB entre os números")
    void opcao2EspacosETab() throws IOException {
        rodarMain("2\n  3 \t 1   2  \n");
        verificarCsv(3, "manual");
    }

    @Test
    @DisplayName("Opção 2 aceita Integer.MAX_VALUE e Integer.MIN_VALUE")
    void opcao2Extremos() throws IOException {
        rodarMain("2\n2147483647 -2147483648\n");
        verificarCsv(2, "manual");
    }

    @Test
    @DisplayName("Opção inválida '9' e opção vazia: avisa 'Opcao invalida', não quebra e não grava CSV")
    void opcaoInvalida() {
        String saida9 = rodarMain("9\n");
        assertTrue(saida9.contains("Opcao invalida"), saida9);
        assertFalse(Files.exists(csv));

        String saidaVazia = rodarMain("\n");
        assertTrue(saidaVazia.contains("Opcao invalida"), saidaVazia);
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 3 com arquivo inexistente: mostra 'Erro ao ler o arquivo', não quebra e não grava CSV")
    void opcao3ArquivoInexistente() {
        String saida = rodarMain("3\n" + dir.resolve("nao existe.csv").toAbsolutePath() + "\n");
        assertTrue(saida.contains("Erro ao ler o arquivo"), saida);
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 3 com arquivo válido de 3 linhas grava tamanho 3 e disposição 'arquivo'")
    void opcao3ArquivoValido() throws IOException {
        String caminho = criarCsvEntrada("entrada com espaco.csv",
                "id;data;coordenada;tamanho_kb\n1;d;c;30\n2;d;c;10\n3;d;c;20\n".getBytes(StandardCharsets.UTF_8));
        rodarMain("3\n" + caminho + "\n");
        verificarCsv(3, "arquivo");
    }

    @Test
    @DisplayName("Opção 3 com arquivo Windows-1252: não quebra (o arquivo é lido e grava tamanho 1)")
    void opcao3ArquivoWindows1252() throws IOException {
        // Com a correção do LeitorArquivo (fallback para Windows-1252) o caminho esperado é ler [100]
        // e gravar o CSV com tamanho 1. O ramo "Erro ao ler o arquivo" continua aceito (era o
        // comportamento antes da correção); só não pode quebrar nem gravar algo incoerente.
        String caminho = criarCsvEntrada("cp1252.csv",
                "id;cidade;coordenada;tamanho_kb\n1;São Paulo;x;100\n".getBytes(Charset.forName("windows-1252")));
        String saida = rodarMainSemQuebrar("3\n" + caminho + "\n");
        if (Files.exists(csv)) {
            verificarCsv(1, "arquivo");
        } else {
            assertTrue(saida.contains("Erro ao ler o arquivo"), saida);
        }
    }

    @Test
    @DisplayName("Opção 3 com valor inválido ('abc') em tamanho_kb: não quebra (hoje grava tamanho 1; corrigido, avisa e não grava)")
    void opcao3ValorInvalidoNaoQuebra() throws IOException {
        // Guarda contra uma correção errada do LeitorArquivo (bug #6 do LeitorArquivoTest):
        // se ele passar a lançar IllegalArgumentException, o Main (que só captura IOException)
        // quebraria aqui. Hoje a linha 'abc' é descartada e o CSV sai com tamanho 1; depois
        // da correção o Main deve mostrar uma mensagem e não gravar. Os dois são aceitos.
        String caminho = criarCsvEntrada("invalido.csv",
                "id;data;coordenada;tamanho_kb\n1;d;c;100\n2;d;c;abc\n".getBytes(StandardCharsets.UTF_8));
        rodarMainSemQuebrar("3\n" + caminho + "\n");
        if (Files.exists(csv)) {
            verificarCsv(1, "arquivo");
        }
    }

    @Test
    @DisplayName("resultados.csv existindo como DIRETÓRIO: mostra 'Erro ao gravar' e não quebra")
    void resultadosComoDiretorio() throws IOException {
        Files.createDirectory(csv);
        String saida = rodarMain("1\n3\n");
        assertTrue(saida.contains("Erro ao gravar o arquivo de resultados"), saida);
    }

    // -----------------------------------------------------------------------
    // Regressão: defeitos corrigidos
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Opção 1 com N = 'abc': avisa, não quebra e não grava CSV")
    void opcao1NTexto() {
        // Justificativa: erro de digitação do usuário não pode encerrar o programa com stack trace.
        rodarMainSemQuebrar("1\nabc\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 1 com N vazio: avisa, não quebra e não grava CSV")
    void opcao1NVazio() {
        // Justificativa: apertar Enter sem digitar nada é comum e não pode derrubar o programa.
        rodarMainSemQuebrar("1\n\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 1 com N = -5: avisa, não quebra e não grava CSV")
    void opcao1NNegativo() {
        // Justificativa: N negativo deve ser validado na entrada, com mensagem ao usuário.
        rodarMainSemQuebrar("1\n-5\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 1 com N = 99999999999 (fora da faixa de int): avisa, não quebra e não grava CSV")
    void opcao1NOverflow() {
        // Justificativa: número fora da faixa de int é entrada inválida, não motivo para crash.
        rodarMainSemQuebrar("1\n99999999999\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 2 com linha vazia: avisa, não quebra e não grava CSV")
    void opcao2LinhaVazia() {
        // Justificativa: "".split("\\s+") devolve [""], e parseInt("") lança exceção não tratada.
        rodarMainSemQuebrar("2\n\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 2 com letras '3 a 1': avisa, não quebra e não grava CSV")
    void opcao2ComLetras() {
        // Justificativa: entrada digitada errada deve ser recusada com mensagem, não com crash.
        rodarMainSemQuebrar("2\n3 a 1\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 2 com decimal '1,5 2': avisa, não quebra e não grava CSV")
    void opcao2ComDecimal() {
        // Justificativa: no Brasil é natural digitar vírgula decimal; o programa só aceita inteiros e deve avisar.
        rodarMainSemQuebrar("2\n1,5 2\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Entrada vazia/EOF (Ctrl+Z) no menu: encerra sem quebrar e sem gravar CSV")
    void eofNoMenu() {
        // Justificativa: fim da entrada (Ctrl+Z/Ctrl+D ou pipe vazio) deve encerrar de forma limpa.
        rodarMainSemQuebrar("");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("EOF depois da opção 1 (sem informar N): encerra sem quebrar e sem gravar CSV")
    void eofDepoisDaOpcao() {
        // Justificativa: mesma do EOF no menu.
        rodarMainSemQuebrar("1\n");
        assertFalse(Files.exists(csv));
    }

    @Test
    @DisplayName("Opção 3 com arquivo sem nenhum valor (só cabeçalho): avisa e não grava CSV")
    void opcao3ArquivoSemLinhasValidas() throws IOException {
        // Justificativa: o usuário escolheu um arquivo esperando dados; antes da correção recebia 0
        // sem aviso e o CSV gerado parecia um resultado legítimo. Diferente do N = 0 da opção 1,
        // que é escolha explícita (opcao1NZero). Por isso a validação fica só no case "3".
        String caminho = criarCsvEntrada("so-cabecalho.csv",
                "id;data;coordenada;tamanho_kb\n".getBytes(StandardCharsets.UTF_8));
        rodarMainSemQuebrar("3\n" + caminho + "\n");
        assertFalse(Files.exists(csv), "resultados.csv foi gravado para uma entrada sem nenhum dado");
    }
}
