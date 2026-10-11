package algoritmos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

/** Aplica o contrato de OrdenadorContratoTest ao Merge Sort (O(n log n): aguenta um vetor bem maior). */
@Tag("algoritmos")
@DisplayName("MergeSort (contrato)")
class MergeSortTest extends OrdenadorContratoTest {

    @Override
    protected Ordenador criarOrdenador() {
        return new MergeSort();
    }

    @Override
    protected int tamanhoGrande() {
        return 200_000;
    }
}
