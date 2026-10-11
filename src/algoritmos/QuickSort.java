package algoritmos;

import java.util.Objects;

/**
 * Implementação do algoritmo Quick Sort.
 *
 * O Quick Sort também usa "dividir para conquistar": escolhe um elemento como pivô,
 * separa os menores de um lado e os maiores do outro (particionamento) e repete
 * o processo em cada parte. Costuma ser muito rápido na prática.
 *
 * Complexidade: melhor caso e caso médio O(n log n); pior caso O(n^2)
 * (raro com a mediana de três). Memória extra O(log n) para a pilha de recursão.
 *
 * Três cuidados desta versão, para não travar nem estourar a pilha:
 *  1. Pivô = mediana de três (primeiro, meio e último). Com o pivô ingênuo (primeiro
 *     ou último elemento), um vetor JÁ ORDENADO gera partições de tamanho n-1 e 0,
 *     o que leva a O(n^2) e a uma recursão com n níveis (StackOverflowError).
 *  2. Partição em três partes (menores | iguais | maiores). A partição de Lomuto
 *     comum deixa todos os elementos iguais ao pivô de um lado só: com um vetor de
 *     valores todos iguais, cai de novo em O(n^2) e em n níveis de recursão.
 *  3. Recursão só na parte MENOR e laço na parte maior: cada chamada recursiva
 *     recebe no máximo metade do trecho, então a pilha tem no máximo log2(n) níveis.
 */
public class QuickSort implements Ordenador {

    @Override
    public void ordenar(int[] vetor) {
        // Vetor null é erro de quem chamou (mesmo comportamento de Arrays.sort(null)).
        Objects.requireNonNull(vetor, "vetor nao pode ser nulo");
        quickSort(vetor, 0, vetor.length - 1);
    }

    /** Ordena o trecho v[inicio..fim] (fim incluído). */
    private static void quickSort(int[] v, int inicio, int fim) {
        // O laço substitui a segunda chamada recursiva (a da parte maior).
        while (inicio < fim) {
            // 1) Escolhe o pivô: o valor do meio entre primeiro, meio e último.
            int meio = inicio + (fim - inicio) / 2;
            int pivo = medianaDeTres(v[inicio], v[meio], v[fim]);

            // 2) Partição em três partes (Dijkstra), percorrendo com i:
            //    v[inicio..lt-1] < pivo | v[lt..gt] == pivo | v[gt+1..fim] > pivo
            int lt = inicio;
            int i = inicio;
            int gt = fim;
            while (i <= gt) {
                if (v[i] < pivo) {
                    trocar(v, lt, i);
                    lt++;
                    i++;
                } else if (v[i] > pivo) {
                    // O elemento que veio de gt ainda não foi analisado: não avança i.
                    trocar(v, i, gt);
                    gt--;
                } else {
                    i++; // igual ao pivô: fica no bloco do meio
                }
            }
            // O bloco dos iguais (v[lt..gt]) já está no lugar certo e nunca é revisitado.
            // Como o pivô é um valor do trecho, esse bloco tem pelo menos 1 elemento,
            // então cada volta do laço sempre diminui o trecho.

            // 3) Recursão na parte menor; a maior continua no próprio laço.
            if (lt - inicio < fim - gt) {
                quickSort(v, inicio, lt - 1);
                inicio = gt + 1;
            } else {
                quickSort(v, gt + 1, fim);
                fim = lt - 1;
            }
        }
    }

    /**
     * Devolve o valor do meio entre a, b e c usando só comparações
     * (sem subtração, que poderia estourar com Integer.MIN_VALUE/MAX_VALUE).
     */
    private static int medianaDeTres(int a, int b, int c) {
        return Math.max(Math.min(a, b), Math.min(Math.max(a, b), c));
    }

    /** Troca de lugar os elementos das posições i e j. */
    private static void trocar(int[] v, int i, int j) {
        int temp = v[i];
        v[i] = v[j];
        v[j] = temp;
    }

    @Override
    public String nome() {
        return "Quick Sort";
    }
}
