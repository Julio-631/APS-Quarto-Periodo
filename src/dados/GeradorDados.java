package dados;

import java.util.Random;

/**
 * Classe responsável por gerar vetores de inteiros para os testes de ordenação.
 *
 * Gera vetores de tamanho N (parametrizável) em três disposições diferentes:
 *  - aleatório: valores em ordem totalmente aleatória;
 *  - ordenado:  valores já em ordem crescente (melhor caso para alguns algoritmos);
 *  - invertido: valores em ordem decrescente (pior caso para alguns algoritmos).
 *
 * Serve para avaliar como cada algoritmo de ordenação se comporta em cada cenário.
 * N = 0 é aceito (vetor vazio); N negativo é rejeitado com IllegalArgumentException.
 */
public class GeradorDados {

    // Gerador de números aleatórios reutilizado pela classe.
    private final Random random;

    /**
     * Cria um gerador com semente fixa.
     * A semente fixa garante que os vetores aleatórios sejam sempre os mesmos
     * entre execuções, tornando a comparação entre algoritmos mais justa.
     */
    public GeradorDados() {
        this.random = new Random(42);
    }

    /**
     * Gera um vetor de tamanho N com valores em ordem aleatória.
     *
     * @param n quantidade de elementos do vetor (0 ou mais)
     * @return vetor de inteiros embaralhado
     * @throws IllegalArgumentException se n for negativo
     */
    public int[] gerarAleatorio(int n) {
        validarTamanho(n);
        int[] vetor = new int[n];
        for (int i = 0; i < n; i++) {
            // Valores entre 0 e 999999 apenas para ter uma faixa ampla.
            vetor[i] = random.nextInt(1_000_000);
        }
        return vetor;
    }

    /**
     * Gera um vetor de tamanho N já ordenado em ordem crescente (0, 1, 2, ...).
     *
     * @param n quantidade de elementos do vetor (0 ou mais)
     * @return vetor ordenado de forma crescente
     * @throws IllegalArgumentException se n for negativo
     */
    public int[] gerarOrdenado(int n) {
        validarTamanho(n);
        int[] vetor = new int[n];
        for (int i = 0; i < n; i++) {
            vetor[i] = i;
        }
        return vetor;
    }

    /**
     * Gera um vetor de tamanho N em ordem decrescente (N-1, N-2, ..., 1, 0).
     *
     * @param n quantidade de elementos do vetor (0 ou mais)
     * @return vetor ordenado de forma decrescente (invertido)
     * @throws IllegalArgumentException se n for negativo
     */
    public int[] gerarInvertido(int n) {
        validarTamanho(n);
        int[] vetor = new int[n];
        for (int i = 0; i < n; i++) {
            vetor[i] = n - 1 - i;
        }
        return vetor;
    }

    /**
     * Valida o tamanho pedido. Sem esta checagem, "new int[n]" com n negativo
     * lançaria NegativeArraySizeException, um detalhe interno pouco claro para
     * quem chamou o método.
     */
    private static void validarTamanho(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("N nao pode ser negativo: " + n);
        }
    }
}
