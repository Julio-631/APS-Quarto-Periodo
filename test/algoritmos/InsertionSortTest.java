package algoritmos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;

/** Aplica o contrato de OrdenadorContratoTest ao Insertion Sort (O(n^2): usa o tamanho grande padrão). */
@Tag("algoritmos")
@DisplayName("InsertionSort (contrato)")
class InsertionSortTest extends OrdenadorContratoTest {

    @Override
    protected Ordenador criarOrdenador() {
        return new InsertionSort();
    }
}
