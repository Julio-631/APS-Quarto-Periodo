import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import algoritmos.BubbleSort;
import algoritmos.InsertionSort;
import algoritmos.MergeSort;
import algoritmos.Ordenador;
import algoritmos.QuickSort;
import benchmark.Cronometro;
import dados.GeradorDados;
import dados.LeitorArquivo;
import saida.ExportadorCSV;

/**
 * Classe principal do sistema.
 *
 * Exibe um menu de console que pergunta de onde virão os dados, executa todos
 * os algoritmos de ordenação sobre esses dados, mede o tempo de cada um e
 * grava os resultados em um arquivo CSV.
 *
 * Fluxo geral:
 *  1. Pergunta a origem dos dados (gerar aleatório, digitar ou carregar de arquivo).
 *  2. Monta o vetor de entrada.
 *  3. Para cada algoritmo, mede o tempo de ordenação (sobre uma CÓPIA do vetor).
 *  4. Exporta os resultados para resultados.csv.
 */
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Objetos auxiliares usados ao longo do programa.
        GeradorDados gerador = new GeradorDados();
        LeitorArquivo leitor = new LeitorArquivo();
        Cronometro cronometro = new Cronometro();
        ExportadorCSV exportador = new ExportadorCSV();

        // Lista com todos os algoritmos que serão testados.
        List<Ordenador> algoritmos = new ArrayList<>();
        algoritmos.add(new BubbleSort());
        algoritmos.add(new InsertionSort());
        algoritmos.add(new MergeSort());
        algoritmos.add(new QuickSort());

        System.out.println("==============================================");
        System.out.println(" COMPARADOR DE ALGORITMOS DE ORDENACAO");
        System.out.println("==============================================");

        // Vetor de dados que será ordenado e texto que descreve a disposição.
        int[] dados = null;
        String disposicao = "desconhecida";

        // Mostra o menu e lê a opção escolhida.
        System.out.println();
        System.out.println("De onde vem os dados?");
        System.out.println("  1 - Gerar vetor aleatorio");
        System.out.println("  2 - Digitar os numeros manualmente");
        System.out.println("  3 - Carregar de um arquivo CSV");
        System.out.print("Escolha uma opcao: ");
        String opcao = lerLinha(scanner);
        if (opcao == null) {
            // Fim da entrada (Ctrl+Z no Windows, Ctrl+D no Linux ou entrada vazia).
            encerrar(scanner, "Fim da entrada. Encerrando.");
            return;
        }

        switch (opcao) {
            case "1":
                // Opção 1: gerar um vetor aleatório de tamanho N.
                System.out.print("Informe o tamanho N do vetor: ");
                String textoN = lerLinha(scanner);
                if (textoN == null) {
                    encerrar(scanner, "Fim da entrada. Encerrando.");
                    return;
                }

                // Converte o texto digitado em número. Texto, linha vazia ou número
                // fora da faixa de int (ex.: 99999999999) lançam NumberFormatException.
                int n;
                try {
                    n = Integer.parseInt(textoN);
                } catch (NumberFormatException e) {
                    encerrar(scanner, "Valor invalido para N: '" + textoN
                            + "'. Informe um numero inteiro. Encerrando.");
                    return;
                }

                // Não existe vetor com tamanho negativo. N = 0 é aceito (vetor vazio).
                if (n < 0) {
                    encerrar(scanner, "N nao pode ser negativo. Encerrando.");
                    return;
                }

                dados = gerador.gerarAleatorio(n);
                disposicao = "aleatorio";
                break;

            case "2":
                // Opção 2: o usuário digita os números separados por espaço.
                System.out.print("Digite os numeros separados por espaco: ");
                String linha = lerLinha(scanner);
                if (linha == null) {
                    encerrar(scanner, "Fim da entrada. Encerrando.");
                    return;
                }

                // Linha vazia: "".split(...) devolveria [""], que não é um número.
                if (linha.isEmpty()) {
                    encerrar(scanner, "Nenhum numero informado. Encerrando.");
                    return;
                }

                // Aceita um ou mais espaços/TABs entre os números.
                String[] partes = linha.split("\\s+");
                dados = new int[partes.length];
                for (int i = 0; i < partes.length; i++) {
                    try {
                        dados[i] = Integer.parseInt(partes[i]);
                    } catch (NumberFormatException e) {
                        // Letras, decimais (1,5) ou números fora da faixa de int.
                        encerrar(scanner, "Valor invalido: '" + partes[i]
                                + "'. Digite apenas numeros inteiros separados por espaco. Encerrando.");
                        return;
                    }
                }
                disposicao = "manual";
                break;

            case "3":
                // Opção 3: carregar a coluna tamanho_kb de um arquivo CSV.
                System.out.print("Informe o caminho do arquivo CSV: ");
                String caminho = lerLinha(scanner);
                if (caminho == null) {
                    encerrar(scanner, "Fim da entrada. Encerrando.");
                    return;
                }
                try {
                    dados = leitor.lerColunaTamanho(caminho);
                    disposicao = "arquivo";
                } catch (IOException e) {
                    System.out.println("Erro ao ler o arquivo: " + e.getMessage());
                    scanner.close();
                    return;
                }

                // O usuário esperava os dados do arquivo: se nenhum valor foi lido
                // (arquivo só com cabeçalho, por exemplo), não faz sentido medir nem gravar.
                // Esta checagem fica só aqui: na opção 1, N = 0 é uma escolha do usuário.
                if (dados.length == 0) {
                    encerrar(scanner, "O arquivo nao contem nenhum valor valido na coluna tamanho_kb. Nada foi gravado.");
                    return;
                }
                break;

            default:
                System.out.println("Opcao invalida. Encerrando.");
                scanner.close();
                return;
        }

        System.out.println();
        System.out.println("Entrada com " + dados.length + " elementos (disposicao: " + disposicao + ").");
        System.out.println("Executando os algoritmos...");
        System.out.println();

        // Lista que vai acumular os resultados de cada algoritmo.
        List<ExportadorCSV.Resultado> resultados = new ArrayList<>();

        // Executa cada algoritmo sobre uma CÓPIA do vetor original.
        // Assim todos ordenam exatamente a mesma entrada e um não interfere no outro.
        for (Ordenador algoritmo : algoritmos) {
            int[] copia = dados.clone();

            // O cronômetro mede apenas a chamada ordenar() (a cópia já foi feita fora da medição).
            double tempoMs = cronometro.medir(algoritmo, copia);

            System.out.printf("  %-15s -> %.3f ms%n", algoritmo.nome(), tempoMs);

            resultados.add(new ExportadorCSV.Resultado(
                    algoritmo.nome(), dados.length, disposicao, tempoMs));
        }

        // Grava todos os resultados no arquivo CSV.
        try {
            exportador.exportar(resultados);
            System.out.println();
            System.out.println("Resultados gravados em resultados.csv");
        } catch (IOException e) {
            System.out.println("Erro ao gravar o arquivo de resultados: " + e.getMessage());
        }

        scanner.close();
    }

    /**
     * Lê a próxima linha digitada, já sem espaços nas pontas.
     * Devolve null quando a entrada acabou (EOF), em vez de deixar o
     * Scanner lançar NoSuchElementException.
     */
    private static String lerLinha(Scanner scanner) {
        return scanner.hasNextLine() ? scanner.nextLine().trim() : null;
    }

    /** Mostra a mensagem ao usuário e fecha o Scanner (usado ao encerrar mais cedo). */
    private static void encerrar(Scanner scanner, String mensagem) {
        System.out.println();
        System.out.println(mensagem);
        scanner.close();
    }
}
