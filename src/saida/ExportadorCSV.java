package saida;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Classe responsável por gravar os resultados do benchmark em um arquivo CSV.
 *
 * Cada linha do arquivo representa um teste e contém:
 *      algoritmo;tamanho;disposicao;tempo_ms
 *
 * O arquivo gerado (resultados.csv) pode ser aberto no Excel, no Google Sheets
 * ou em qualquer ferramenta de planilha para análise e geração de gráficos.
 *
 * Cuidados na gravação:
 *  - os campos de texto (algoritmo e disposição) são escapados como manda a RFC 4180
 *    (entre aspas quando contêm ';', '"', CR ou LF, com '"' duplicada);
 *  - campos de texto que começam com =, +, - ou @ recebem um apóstrofo na frente,
 *    para a planilha não executá-los como fórmula (CSV injection);
 *  - o tempo é sempre formatado com dígitos ASCII e vírgula decimal (ex.: 1,500),
 *    independentemente do idioma configurado no computador.
 */
public class ExportadorCSV {

    // Separador de colunas usado no arquivo de saída.
    private static final String SEPARADOR = ";";

    // Nome padrão do arquivo de resultados.
    private static final String ARQUIVO_PADRAO = "resultados.csv";

    /**
     * Representa uma linha de resultado do benchmark.
     *
     * É uma classe interna simples (apenas para agrupar os quatro dados de um teste):
     * nome do algoritmo, tamanho da entrada, disposição e tempo medido.
     */
    public static class Resultado {
        public final String algoritmo;
        public final int tamanho;
        public final String disposicao;
        public final double tempoMs;

        public Resultado(String algoritmo, int tamanho, String disposicao, double tempoMs) {
            this.algoritmo = algoritmo;
            this.tamanho = tamanho;
            this.disposicao = disposicao;
            this.tempoMs = tempoMs;
        }
    }

    /**
     * Grava a lista de resultados no arquivo padrão "resultados.csv".
     *
     * @param resultados lista de resultados a serem gravados
     * @throws IOException se o arquivo não puder ser escrito
     */
    public void exportar(List<Resultado> resultados) throws IOException {
        exportar(resultados, ARQUIVO_PADRAO);
    }

    /**
     * Grava a lista de resultados em um arquivo CSV com o nome informado.
     *
     * Todos os resultados são validados ANTES de abrir o arquivo: assim uma chamada
     * inválida não apaga (trunca) um arquivo que já existia.
     *
     * @param resultados lista de resultados a serem gravados
     * @param caminho    caminho/nome do arquivo de saída
     * @throws IOException              se o arquivo não puder ser escrito
     * @throws NullPointerException     se a lista, algum resultado, o algoritmo, a disposição ou o caminho forem null
     * @throws IllegalArgumentException se algum tempo for NaN ou infinito
     */
    public void exportar(List<Resultado> resultados, String caminho) throws IOException {
        // 1) Valida tudo antes de tocar no arquivo.
        validar(resultados);

        Path destino = Paths.get(caminho);

        // 2) Só agora abre (e trunca) o arquivo de saída.
        try (BufferedWriter escritor = Files.newBufferedWriter(destino)) {
            // Escreve o cabeçalho do CSV.
            escritor.write("algoritmo" + SEPARADOR + "tamanho" + SEPARADOR + "disposicao" + SEPARADOR + "tempo_ms");
            escritor.newLine();

            // Escreve uma linha para cada resultado.
            for (Resultado r : resultados) {
                escritor.write(campoTexto(r.algoritmo) + SEPARADOR
                        + r.tamanho + SEPARADOR
                        + campoTexto(r.disposicao) + SEPARADOR
                        + formatarTempo(r.tempoMs));
                escritor.newLine();
            }
        }
    }

    /**
     * Confere a lista e cada resultado. Lança exceção no primeiro problema encontrado.
     */
    private static void validar(List<Resultado> resultados) {
        Objects.requireNonNull(resultados, "lista de resultados nao pode ser nula");
        for (Resultado r : resultados) {
            Objects.requireNonNull(r, "resultado nulo na lista");
            // Sem esta checagem, a palavra "null" seria gravada como se fosse um nome.
            Objects.requireNonNull(r.algoritmo, "algoritmo nao pode ser nulo");
            Objects.requireNonNull(r.disposicao, "disposicao nao pode ser nula");
            // NaN/Infinity não são números para a planilha (quebram médias e gráficos).
            if (!Double.isFinite(r.tempoMs)) {
                throw new IllegalArgumentException("tempo invalido (NaN/Infinito): " + r.tempoMs);
            }
        }
    }

    /**
     * Prepara um campo de TEXTO para o CSV: primeiro neutraliza fórmulas e
     * depois aplica o escapamento da RFC 4180.
     * Os campos numéricos (tamanho e tempo) não passam por aqui, para que
     * números negativos como -1 continuem sendo gravados como números.
     */
    private static String campoTexto(String s) {
        return escapar(neutralizarFormula(s));
    }

    /**
     * Proteção contra "CSV injection" (OWASP): Excel e LibreOffice interpretam
     * como fórmula um campo que começa com =, +, - ou @. Colocar um apóstrofo
     * na frente faz a planilha tratar o conteúdo como texto comum.
     */
    private static String neutralizarFormula(String s) {
        if (!s.isEmpty()) {
            char primeiro = s.charAt(0);
            if (primeiro == '=' || primeiro == '+' || primeiro == '-' || primeiro == '@') {
                return "'" + s;
            }
        }
        return s;
    }

    /**
     * Escapamento da RFC 4180: se o campo contém o separador (;), aspas ("),
     * CR ou LF, ele é colocado entre aspas e cada aspa interna é duplicada.
     * Exemplo: Bubble;Sort vira "Bubble;Sort" e "Bubble" vira """Bubble""".
     */
    private static String escapar(String s) {
        if (s.contains(SEPARADOR) || s.contains("\"") || s.contains("\r") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    /**
     * Formata o tempo com 3 casas decimais e vírgula decimal (ex.: 1,500).
     * Locale.ROOT garante dígitos ASCII e ponto decimal em qualquer computador
     * (em árabe ou tailandês, por exemplo, o Locale padrão usaria outros dígitos);
     * depois o ponto é trocado por vírgula para facilitar a leitura em planilhas em português.
     */
    private static String formatarTempo(double tempoMs) {
        return String.format(Locale.ROOT, "%.3f", tempoMs).replace('.', ',');
    }
}
