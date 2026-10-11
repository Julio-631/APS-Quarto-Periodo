package dados;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes da classe LeitorArquivo.
 *
 * Todos os arquivos de entrada são criados em uma pasta temporária (@TempDir),
 * gravando os BYTES exatos, para controlar codificação, BOM e finais de linha.
 *
 * Política adotada para os testes [BUG]: um valor inválido na coluna numérica
 * NÃO pode ser descartado em silêncio, porque isso altera o tamanho/conteúdo da
 * entrada do benchmark sem o usuário perceber. O esperado é uma exceção clara
 * (IOException ou IllegalArgumentException). Linhas totalmente vazias ou só com
 * separadores (";;;") continuam podendo ser puladas, pois não contêm dado algum.
 *
 * ATENÇÃO - mudança de contrato: o Javadoc de lerColunaNumerica em src/ DOCUMENTA que
 * o método "ignora a primeira linha (cabeçalho)" e "pula ... linhas em que o valor da
 * coluna não seja um número válido". Os testes [BUG] semCabecalhoNaoPodePerderPrimeiraLinha,
 * valorInvalidoNaoPodeSumirEmSilencio (7 casos), colunasFaltando e indiceAlemDasColunas
 * reprovam esse comportamento documentado: o código cumpre a própria especificação,
 * mas a especificação é perigosa para um benchmark. São PROPOSTAS de mudança de
 * contrato, não descumprimento do Javadoc. Os defeitos de robustez arquivoWindows1252ComAcentos
 * e indiceNegativo foram corrigidos e viraram testes de regressão (sem tag).
 *
 * Observação para a correção: com o contrato novo, ";;;" (só separadores) ainda pode
 * ser pulada (linhasVaziasESoSeparadores), mas "1;2026-01-05" (dados com coluna
 * faltando) deve falhar (colunasFaltando). Como ";;;".split(";") tem tamanho 0, a
 * correção precisa diferenciar "linha sem nenhum dado" de "linha com dados e coluna
 * faltando", e não apenas trocar o continue por throw.
 */
@DisplayName("LeitorArquivo")
class LeitorArquivoTest {

    private static final String CABECALHO = "id;data;coordenada;tamanho_kb\n";

    private final LeitorArquivo leitor = new LeitorArquivo();

    @TempDir
    Path dir;

    /** Grava o texto em UTF-8 (sem BOM) e devolve o caminho como String. */
    private String criar(String nome, String conteudo) throws IOException {
        return criar(nome, conteudo.getBytes(StandardCharsets.UTF_8));
    }

    /** Grava os bytes exatos e devolve o caminho como String. */
    private String criar(String nome, byte[] bytes) throws IOException {
        Path p = dir.resolve(nome);
        Files.write(p, bytes);
        return p.toString();
    }

    /** Verifica que a execução lança IOException ou IllegalArgumentException (falha clara). */
    private static void assertFalhaClara(Executable acao) {
        Throwable t = assertThrows(Exception.class, acao,
                "o valor inválido foi descartado em silêncio; esperado lançar IOException ou IllegalArgumentException");
        assertTrue(t instanceof IOException || t instanceof IllegalArgumentException,
                "tipo de exceção inesperado: " + t);
    }

    // -----------------------------------------------------------------------
    // Bordas aceitáveis (passam hoje)
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Arquivo de exemplo dados_entrada/imagens_satelite.csv: a vírgula da coordenada não atrapalha")
    void arquivoDeExemploReal() throws IOException {
        Path exemplo = Path.of("dados_entrada", "imagens_satelite.csv");
        assumeTrue(Files.exists(exemplo), "rode a partir da raiz do projeto");
        assertArrayEquals(new int[] {1540, 3820, 2210, 4760, 980, 3155, 5120, 1725, 2890, 640},
                leitor.lerColunaTamanho(exemplo.toString()));
    }

    @Test
    @DisplayName("Caminho null lança NullPointerException")
    void caminhoNulo() {
        assertThrows(NullPointerException.class, () -> leitor.lerColunaTamanho(null));
    }

    @Test
    @DisplayName("Caminho vazio \"\" (vira o diretório atual) lança IOException")
    void caminhoVazio() {
        assertThrows(IOException.class, () -> leitor.lerColunaTamanho(""));
    }

    @Test
    @DisplayName("Diretório no lugar de arquivo lança IOException")
    void diretorioComoCaminho() {
        assertThrows(IOException.class, () -> leitor.lerColunaTamanho(dir.toString()));
    }

    @Test
    @DisplayName("Arquivo inexistente lança NoSuchFileException")
    void arquivoInexistente() {
        assertThrows(NoSuchFileException.class,
                () -> leitor.lerColunaTamanho(dir.resolve("nao-existe.csv").toString()));
    }

    @Test
    @DisplayName("Arquivo vazio (0 bytes) devolve vetor vazio")
    void arquivoVazio() throws IOException {
        assertEquals(0, leitor.lerColunaTamanho(criar("vazio.csv", new byte[0])).length);
    }

    @Test
    @DisplayName("Arquivo só com cabeçalho devolve vetor vazio")
    void soCabecalho() throws IOException {
        assertEquals(0, leitor.lerColunaTamanho(criar("cab.csv", CABECALHO)).length);
    }

    @Test
    @DisplayName("Linhas em branco, só com espaços e ';;;' são ignoradas; ';;;5' lê o 5")
    void linhasVaziasESoSeparadores() throws IOException {
        String conteudo = CABECALHO
                + "1;d;c;10\n"
                + "\n"
                + "     \n"
                + ";;;\n"
                + ";;;5\n"
                + "2;d;c;20\n";
        assertArrayEquals(new int[] {10, 5, 20}, leitor.lerColunaTamanho(criar("vazias.csv", conteudo)));
    }

    @Test
    @DisplayName("Campo com espaços em volta (' 1540 ') é aparado e lido")
    void campoComEspacos() throws IOException {
        assertArrayEquals(new int[] {1540},
                leitor.lerColunaTamanho(criar("esp.csv", CABECALHO + "1;d;c; 1540 \n")));
    }

    @Test
    @DisplayName("Sinais explícitos +15 e -20 são aceitos")
    void sinaisExplicitos() throws IOException {
        assertArrayEquals(new int[] {15, -20},
                leitor.lerColunaTamanho(criar("sinal.csv", CABECALHO + "1;d;c;+15\n2;d;c;-20\n")));
    }

    @Test
    @DisplayName("Extremos Integer.MAX_VALUE e Integer.MIN_VALUE são lidos sem perda")
    void extremosDeInt() throws IOException {
        assertArrayEquals(new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE},
                leitor.lerColunaTamanho(criar("ext.csv", CABECALHO + "1;d;c;2147483647\n2;d;c;-2147483648\n")));
    }

    @Test
    @DisplayName("Valores duplicados são preservados na ordem do arquivo")
    void duplicadosPreservados() throws IOException {
        assertArrayEquals(new int[] {100, 100, 100},
                leitor.lerColunaTamanho(criar("dup.csv", CABECALHO + "1;d;c;100\n2;d;c;100\n3;d;c;100\n")));
    }

    @Test
    @DisplayName("Coluna extra no fim da linha não atrapalha")
    void colunaExtra() throws IOException {
        assertArrayEquals(new int[] {100},
                leitor.lerColunaTamanho(criar("extra.csv", CABECALHO + "1;d;c;100;extra\n")));
    }

    @Test
    @DisplayName("Finais de linha CRLF (Windows) e só CR (Mac antigo) são aceitos")
    void finaisDeLinhaCrlfECr() throws IOException {
        String crlf = criar("crlf.csv", "id;data;coordenada;tamanho_kb\r\n1;d;c;10\r\n2;d;c;20\r\n");
        String cr = criar("cr.csv", "id;data;coordenada;tamanho_kb\r1;d;c;10\r2;d;c;20\r");
        assertArrayEquals(new int[] {10, 20}, leitor.lerColunaTamanho(crlf));
        assertArrayEquals(new int[] {10, 20}, leitor.lerColunaTamanho(cr));
    }

    @Test
    @DisplayName("UTF-8 com BOM: o BOM fica no cabeçalho descartado e os dados são lidos")
    void utf8ComBom() throws IOException {
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] texto = (CABECALHO + "1;d;c;10\n2;d;c;20\n").getBytes(StandardCharsets.UTF_8);
        byte[] tudo = new byte[bom.length + texto.length];
        System.arraycopy(bom, 0, tudo, 0, bom.length);
        System.arraycopy(texto, 0, tudo, bom.length, texto.length);
        assertArrayEquals(new int[] {10, 20}, leitor.lerColunaTamanho(criar("bom.csv", tudo)));
    }

    @Test
    @DisplayName("lerColunaNumerica com índice 0 extrai a coluna id")
    void colunaIdComIndiceZero() throws IOException {
        String caminho = criar("ids.csv", CABECALHO + "1;d;c;10\n2;d;c;20\n3;d;c;30\n");
        assertArrayEquals(new int[] {1, 2, 3}, leitor.lerColunaNumerica(caminho, 0));
    }

    @Test
    @DisplayName("Arquivo com 100.000 linhas é lido por completo e na ordem")
    void arquivoGrande() throws IOException {
        StringBuilder sb = new StringBuilder(CABECALHO);
        for (int i = 0; i < 100_000; i++) {
            sb.append(i).append(";d;c;").append(i).append('\n');
        }
        int[] v = leitor.lerColunaTamanho(criar("grande.csv", sb.toString()));
        assertEquals(100_000, v.length);
        for (int i = 0; i < v.length; i++) {
            assertEquals(i, v[i], "posição " + i);
        }
    }

    // -----------------------------------------------------------------------
    // Comportamentos pendentes (tag "bug") e regressões de defeitos corrigidos (sem tag)
    // -----------------------------------------------------------------------

    @Test
    @Tag("bug")
    @DisplayName("[BUG] Arquivo SEM cabeçalho perde a primeira linha de dados - esperado: ler todas as linhas")
    void semCabecalhoNaoPodePerderPrimeiraLinha() throws IOException {
        // Justificativa: a primeira linha só deveria ser descartada se o campo da coluna
        // alvo não for numérico (ou seja, se for de fato um cabeçalho). Hoje ela é
        // descartada sempre, e o valor 100 some sem aviso.
        String caminho = criar("sem-cab.csv", "1;2026-01-05;x;100\n2;2026-01-06;y;200\n");
        assertArrayEquals(new int[] {100, 200}, leitor.lerColunaTamanho(caminho));
    }

    @Test
    @DisplayName("CSV em Windows-1252 com acentos (salvo pelo Excel) é lido: [100, 200]")
    void arquivoWindows1252ComAcentos() throws IOException {
        // Regressão (defeito corrigido): antes quebrava com MalformedInputException.
        // Justificativa: CSV exportado pelo Excel no Windows costuma vir em Windows-1252.
        // A coluna que interessa (tamanho_kb) é ASCII puro; um acento em OUTRA coluna
        // não deve impedir a leitura do arquivo inteiro.
        String texto = "id;cidade;coordenada;tamanho_kb\n1;São Paulo;x;100\n2;Brasília;y;200\n";
        String caminho = criar("cp1252.csv", texto.getBytes(Charset.forName("windows-1252")));
        int[] lido = assertDoesNotThrow(() -> leitor.lerColunaTamanho(caminho),
                "a leitura não deveria quebrar por causa do acento em outra coluna");
        assertArrayEquals(new int[] {100, 200}, lido);
    }

    @Tag("bug")
    @ParameterizedTest(name = "valor = {0}")
    @ValueSource(strings = {"abc", "1540.5", "1540,5", "99999999999", "2147483648", "1 540", "\"1540\""})
    @DisplayName("[BUG] Valor inválido na coluna (texto, decimal, overflow, espaço interno, aspas) some em silêncio - esperado: exceção clara")
    void valorInvalidoNaoPodeSumirEmSilencio(String valor) throws IOException {
        // Justificativa: descartar a linha muda o tamanho N do benchmark sem o usuário
        // saber; o resultado medido deixa de corresponder ao arquivo informado.
        String caminho = criar("invalido.csv", CABECALHO + "1;d;c;" + valor + "\n2;d;c;200\n");
        assertFalhaClara(() -> leitor.lerColunaTamanho(caminho));
    }

    @Test
    @Tag("bug")
    @DisplayName("[BUG] Linha com colunas faltando ('1;2026-01-05') é descartada em silêncio - esperado: exceção clara")
    void colunasFaltando() throws IOException {
        // Justificativa: mesma política de não perder dados sem aviso.
        String caminho = criar("faltando.csv", CABECALHO + "1;2026-01-05\n2;d;c;200\n");
        assertFalhaClara(() -> leitor.lerColunaTamanho(caminho));
    }

    @Test
    @DisplayName("Índice de coluna -1 lança IllegalArgumentException")
    void indiceNegativo() throws IOException {
        // Regressão (defeito corrigido): antes vazava ArrayIndexOutOfBoundsException.
        // Justificativa: índice negativo é argumento inválido do método público;
        // deve ser validado com IllegalArgumentException, sem vazar erro interno.
        String caminho = criar("neg.csv", CABECALHO + "1;d;c;10\n");
        assertThrows(IllegalArgumentException.class, () -> leitor.lerColunaNumerica(caminho, -1));
    }

    @Test
    @Tag("bug")
    @DisplayName("[BUG] Índice 99 (coluna que nenhuma linha tem) devolve vetor vazio em silêncio - esperado: exceção clara")
    void indiceAlemDasColunas() throws IOException {
        // Justificativa: pedir uma coluna que não existe em nenhuma linha é erro de uso;
        // devolver [] faz o benchmark rodar com 0 elementos sem explicação.
        String caminho = criar("idx.csv", CABECALHO + "1;d;c;10\n2;d;c;20\n");
        assertFalhaClara(() -> leitor.lerColunaNumerica(caminho, 99));
    }
}
