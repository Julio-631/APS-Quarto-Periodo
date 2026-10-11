package algoritmos;

import java.util.Objects;

/**
 * Implementação do algoritmo Insertion Sort.
 *
 * O Insertion Sort constrói o vetor ordenado aos poucos: pega cada elemento
 * e o "insere" na posição correta entre os elementos já ordenados à esquerda.
 * É eficiente para vetores pequenos ou quase ordenados.
 *
 * Complexidade: pior caso (vetor invertido) e caso médio O(n^2);
 * melhor caso O(n) (vetor já ordenado: nenhum elemento precisa ser deslocado).
 */
public class InsertionSort implements Ordenador {

    @Override
    public void ordenar(int[] vetor) {
        // Vetor null é erro de quem chamou (mesmo comportamento de Arrays.sort(null)).
        Objects.requireNonNull(vetor, "vetor nao pode ser nulo");

        // A parte vetor[0..i-1] já está ordenada; o elemento vetor[i] será inserido nela.
        for (int i = 1; i < vetor.length; i++) {
            int chave = vetor[i];
            int j = i - 1;

            // Desloca uma posição para a direita todos os elementos maiores que a chave.
            while (j >= 0 && vetor[j] > chave) {
                vetor[j + 1] = vetor[j];
                j--;
            }

            // Coloca a chave no espaço que sobrou.
            vetor[j + 1] = chave;
        }
    }

    @Override
    public String nome() {
        return "Insertion Sort";
    }
}
