package benchmark;

import java.util.Objects;

import algoritmos.Ordenador;

/**
 * Classe responsável por medir o tempo de execução de um algoritmo de ordenação.
 *
 * Ponto importante: o cronômetro mede APENAS a chamada ao método ordenar().
 * A geração ou leitura dos dados acontece ANTES e NÃO entra na medição,
 * garantindo que o tempo reflita só o custo da ordenação.
 */
public class Cronometro {

    /**
     * Mede o tempo que o algoritmo leva para ordenar o vetor informado.
     *
     * Usa System.nanoTime(), que é próprio para medir intervalos de tempo
     * (mais preciso que System.currentTimeMillis() para esse fim).
     *
     * @param ordenador algoritmo que fará a ordenação
     * @param vetor     vetor a ser ordenado (já pronto, fora da medição)
     * @return tempo gasto na ordenação, em milissegundos
     * @throws NullPointerException se o ordenador ou o vetor forem null
     */
    public double medir(Ordenador ordenador, int[] vetor) {
        // Valida os argumentos ANTES de começar a medir (a validação não entra no tempo).
        // Medir a ordenação de um vetor inexistente não tem significado: sem esta
        // checagem, ordenar(null) poderia ser chamado e devolveríamos um tempo "válido".
        Objects.requireNonNull(ordenador, "ordenador nao pode ser nulo");
        Objects.requireNonNull(vetor, "vetor nao pode ser nulo");

        // Marca o instante imediatamente antes de ordenar.
        long inicio = System.nanoTime();

        // Única coisa cronometrada: a ordenação em si.
        ordenador.ordenar(vetor);

        // Marca o instante imediatamente após ordenar.
        long fim = System.nanoTime();

        // A diferença está em nanossegundos; divide por 1.000.000 para obter milissegundos.
        long duracaoNanos = fim - inicio;
        return duracaoNanos / 1_000_000.0;
    }
}
