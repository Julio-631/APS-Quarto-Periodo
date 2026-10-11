package algoritmos;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Aplica o contrato de OrdenadorContratoTest ao Quick Sort e acrescenta armadilhas
 * clássicas: com pivô ingênuo (primeiro/último elemento) ou partição de Lomuto sem
 * tratar iguais, vetores já ordenados ou todos iguais viram O(n^2) e a recursão
 * pode estourar a pilha (StackOverflowError).
 *
 * Limitação: com 50.000 elementos, um QuickSort O(n^2) faz ~1,25 bilhão de comparações
 * e costuma terminar em poucos segundos, abaixo do limite de 10 s. Por isso estes
 * testes pegam de forma confiável só o ESTOURO DE PILHA (recursão com ~50.000 níveis).
 * Uma versão O(n^2) que recursa no lado menor e itera no maior pode passar.
 */
@Tag("algoritmos")
@DisplayName("QuickSort (contrato)")
class QuickSortTest extends OrdenadorContratoTest {

    @Override
    protected Ordenador criarOrdenador() {
        return new QuickSort();
    }

    @Override
    protected int tamanhoGrande() {
        return 200_000;
    }

    @Test
    @DisplayName("50.000 elementos já ordenados em até 10 s (pega estouro de pilha do pivô ingênuo; não garante detectar O(n^2) pelo tempo)")
    void cinquentaMilOrdenados() {
        int[] v = new int[50_000];
        for (int i = 0; i < v.length; i++) {
            v[i] = i;
        }
        verificarOrdenacao(v);
    }

    @Test
    @DisplayName("50.000 elementos todos iguais em até 10 s (pega estouro de pilha da partição que não trata iguais; não garante detectar O(n^2) pelo tempo)")
    void cinquentaMilIguais() {
        int[] v = new int[50_000];
        Arrays.fill(v, 3);
        verificarOrdenacao(v);
    }
}
