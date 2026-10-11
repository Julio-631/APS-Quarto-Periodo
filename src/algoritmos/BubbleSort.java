package algoritmos;

import java.util.Objects;

/**
 * Implementação do algoritmo Bubble Sort.
 *
 * O Bubble Sort percorre o vetor várias vezes, trocando elementos vizinhos
 * fora de ordem, fazendo os maiores valores "borbulharem" para o final.
 * É simples de entender, mas lento para vetores grandes.
 *
 * Complexidade: pior caso e caso médio O(n^2); melhor caso O(n) (vetor já
 * ordenado), graças à parada antecipada quando uma passada não faz nenhuma troca.
 */
public class BubbleSort implements Ordenador {

    @Override
    public void ordenar(int[] vetor) {
        // Vetor null é erro de quem chamou (mesmo comportamento de Arrays.sort(null)).
        Objects.requireNonNull(vetor, "vetor nao pode ser nulo");

        // A cada passada o maior elemento ainda fora do lugar vai para a posição "fim".
        // Por isso o trecho a percorrer diminui uma posição por passada.
        for (int fim = vetor.length - 1; fim > 0; fim--) {
            boolean houveTroca = false;

            // Compara cada par de vizinhos e troca se estiverem fora de ordem.
            for (int i = 0; i < fim; i++) {
                if (vetor[i] > vetor[i + 1]) {
                    trocar(vetor, i, i + 1);
                    houveTroca = true;
                }
            }

            // Parada antecipada: se nenhuma troca aconteceu, o vetor já está ordenado.
            if (!houveTroca) {
                break;
            }
        }
    }

    /** Troca de lugar os elementos das posições i e j. */
    private static void trocar(int[] v, int i, int j) {
        int temp = v[i];
        v[i] = v[j];
        v[j] = temp;
    }

    @Override
    public String nome() {
        return "Bubble Sort";
    }
}
