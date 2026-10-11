package algoritmos;

/**
 * Contrato (interface) que todo algoritmo de ordenação do projeto deve seguir.
 *
 * Ao padronizar os métodos, o restante do sistema (benchmark, menu, etc.)
 * consegue tratar qualquer algoritmo da mesma forma, sem precisar conhecer
 * os detalhes internos de cada um. Isso é polimorfismo na prática.
 */
public interface Ordenador {

    /**
     * Ordena o vetor recebido em ordem crescente.
     * A ordenação é feita diretamente no vetor passado (in-place).
     *
     * @param vetor vetor de inteiros a ser ordenado
     */
    void ordenar(int[] vetor);

    /**
     * Retorna o nome do algoritmo (ex.: "Bubble Sort").
     * Usado para exibir no menu e identificar os resultados no relatório.
     *
     * @return nome legível do algoritmo
     */
    String nome();
}
