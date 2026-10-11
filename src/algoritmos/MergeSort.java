package algoritmos;

import java.util.Objects;

/**
 * Implementação do algoritmo Merge Sort.
 *
 * O Merge Sort usa a estratégia "dividir para conquistar": divide o vetor ao meio
 * repetidamente até sobrarem pedaços de tamanho 1, depois junta (merge) esses pedaços
 * de volta já ordenados. É rápido e estável.
 *
 * Complexidade: O(n log n) no melhor, no médio e no pior caso.
 * Usa um vetor auxiliar de tamanho n (memória extra O(n)).
 */
public class MergeSort implements Ordenador {

    @Override
    public void ordenar(int[] vetor) {
        // Vetor null é erro de quem chamou (mesmo comportamento de Arrays.sort(null)).
        Objects.requireNonNull(vetor, "vetor nao pode ser nulo");

        // Vetor com 0 ou 1 elemento já está ordenado.
        if (vetor.length < 2) {
            return;
        }

        // O vetor auxiliar é criado UMA vez e reaproveitado em todas as intercalações.
        // Ele é local (não é campo da classe), então a mesma instância pode ser reutilizada.
        int[] aux = new int[vetor.length];
        mergeSort(vetor, aux, 0, vetor.length - 1);
    }

    /**
     * Ordena o trecho v[inicio..fim] (fim incluído).
     * A profundidade da recursão é cerca de log2(n): 18 níveis para 200.000 elementos.
     */
    private static void mergeSort(int[] v, int[] aux, int inicio, int fim) {
        if (inicio >= fim) {
            return; // trecho com 0 ou 1 elemento
        }

        // Calcula o meio sem somar inicio + fim (evita estouro de int em vetores enormes).
        int meio = inicio + (fim - inicio) / 2;

        // Divide: ordena cada metade separadamente.
        mergeSort(v, aux, inicio, meio);
        mergeSort(v, aux, meio + 1, fim);

        // Conquista: junta as duas metades ordenadas.
        intercalar(v, aux, inicio, meio, fim);
    }

    /**
     * Junta as metades ordenadas v[inicio..meio] e v[meio+1..fim] em um único trecho ordenado.
     */
    private static void intercalar(int[] v, int[] aux, int inicio, int meio, int fim) {
        // Copia o trecho para o auxiliar; a intercalação escreve de volta em v.
        System.arraycopy(v, inicio, aux, inicio, fim - inicio + 1);

        int i = inicio;     // percorre a metade esquerda
        int j = meio + 1;   // percorre a metade direita
        int k = inicio;     // próxima posição a preencher em v

        // Escolhe sempre o menor dos dois "primeiros da fila".
        // O "<=" mantém a ordem original dos iguais (algoritmo estável).
        while (i <= meio && j <= fim) {
            if (aux[i] <= aux[j]) {
                v[k++] = aux[i++];
            } else {
                v[k++] = aux[j++];
            }
        }

        // Copia o que sobrou de cada metade (só uma delas ainda terá elementos).
        while (i <= meio) {
            v[k++] = aux[i++];
        }
        while (j <= fim) {
            v[k++] = aux[j++];
        }
    }

    @Override
    public String nome() {
        return "Merge Sort";
    }
}
