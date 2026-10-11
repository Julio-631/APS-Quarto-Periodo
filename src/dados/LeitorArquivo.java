package dados;

import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável por ler dados de um arquivo CSV.
 *
 * O CSV esperado tem o separador ponto e vírgula (;) e as colunas:
 *      id;data;coordenada;tamanho_kb
 *
 * A ideia é extrair UMA coluna numérica (por padrão, "tamanho_kb") e
 * transformá-la em um vetor de inteiros, que depois será ordenado.
 */
public class LeitorArquivo {

    // Separador de colunas usado no arquivo CSV.
    private static final String SEPARADOR = ";";

    // Codificação usada pelo Excel no Windows ao salvar CSV (acentos em 1 byte).
    private static final Charset WINDOWS_1252 = Charset.forName("windows-1252");

    /**
     * Lê o arquivo e extrai a coluna "tamanho_kb" (a quarta coluna) como vetor de int.
     *
     * @param caminho caminho do arquivo CSV
     * @return vetor de inteiros com os valores da coluna tamanho_kb
     * @throws IOException se o arquivo não puder ser lido
     */
    public int[] lerColunaTamanho(String caminho) throws IOException {
        // A coluna tamanho_kb é a quarta, portanto índice 3 (contando a partir de 0).
        return lerColunaNumerica(caminho, 3);
    }

    /**
     * Lê o arquivo CSV e extrai os valores de uma coluna numérica específica.
     *
     * O método ignora a primeira linha (cabeçalho) e também pula linhas vazias
     * ou linhas em que o valor da coluna não seja um número válido.
     *
     * O arquivo é lido em UTF-8; se não for UTF-8 válido (por exemplo, um CSV com
     * acentos salvo pelo Excel no Windows), é lido de novo em Windows-1252.
     *
     * @param caminho      caminho do arquivo CSV
     * @param indiceColuna índice da coluna a extrair (0 = id, 1 = data, 2 = coordenada, 3 = tamanho_kb)
     * @return vetor de inteiros com os valores lidos
     * @throws IOException              se o arquivo não puder ser lido
     * @throws IllegalArgumentException se indiceColuna for negativo
     */
    public int[] lerColunaNumerica(String caminho, int indiceColuna) throws IOException {
        // Índice negativo é erro de quem chamou: avisa com uma mensagem clara
        // (sem isso, colunas[-1] lançaria ArrayIndexOutOfBoundsException lá dentro).
        if (indiceColuna < 0) {
            throw new IllegalArgumentException("indiceColuna nao pode ser negativo: " + indiceColuna);
        }

        // Lista dinâmica porque não sabemos de antemão quantas linhas válidas existem.
        List<Integer> valores = new ArrayList<>();

        List<String> linhas = lerLinhas(Paths.get(caminho));
        boolean primeiraLinha = true;

        for (String linha : linhas) {
            // A primeira linha é o cabeçalho (id;data;coordenada;tamanho_kb) e deve ser ignorada.
            if (primeiraLinha) {
                primeiraLinha = false;
                continue;
            }

            // Pula linhas em branco.
            if (linha.trim().isEmpty()) {
                continue;
            }

            // Divide a linha nas colunas usando o separador.
            String[] colunas = linha.split(SEPARADOR);

            // Garante que a coluna desejada existe nessa linha.
            if (colunas.length <= indiceColuna) {
                continue;
            }

            String campo = colunas[indiceColuna].trim();

            // Tenta converter o campo para inteiro; se não for número, ignora a linha.
            try {
                valores.add(Integer.parseInt(campo));
            } catch (NumberFormatException e) {
                // Valor inválido na coluna: simplesmente ignora essa linha.
            }
        }

        // Converte a lista de Integer para um vetor primitivo de int.
        int[] vetor = new int[valores.size()];
        for (int i = 0; i < vetor.length; i++) {
            vetor[i] = valores.get(i);
        }
        return vetor;
    }

    /**
     * Lê todas as linhas do arquivo, tentando primeiro UTF-8 e depois Windows-1252.
     *
     * Um CSV salvo pelo Excel no Windows costuma vir em Windows-1252: um acento como
     * o "ã" de "São Paulo" vira um byte que não é UTF-8 válido e a leitura em UTF-8
     * falha com MalformedInputException (subclasse de CharacterCodingException).
     * Nesse caso o arquivo é lido de novo em Windows-1252. Os finais de linha
     * LF, CRLF e CR continuam sendo aceitos nas duas leituras.
     */
    private static List<String> lerLinhas(Path arquivo) throws IOException {
        try {
            return Files.readAllLines(arquivo, StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return Files.readAllLines(arquivo, WINDOWS_1252);
        }
    }
}
