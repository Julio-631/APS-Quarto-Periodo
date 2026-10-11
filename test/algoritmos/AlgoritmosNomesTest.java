package algoritmos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Confere os nomes dos algoritmos: são usados como identificador no resultados.csv,
 * então precisam ser distintos (senão os resultados de dois algoritmos se misturam).
 */
@Tag("algoritmos")
@DisplayName("Nomes dos algoritmos")
class AlgoritmosNomesTest {

    @Test
    @DisplayName("Os 4 nomes são distintos e iguais aos esperados")
    void nomesDistintosEEsperados() {
        List<Ordenador> todos = List.of(new BubbleSort(), new InsertionSort(), new MergeSort(), new QuickSort());
        Set<String> nomes = new HashSet<>();
        for (Ordenador o : todos) {
            nomes.add(o.nome());
        }
        assertEquals(4, nomes.size(), "nomes repetidos: " + nomes);
        assertEquals("Bubble Sort", todos.get(0).nome());
        assertEquals("Insertion Sort", todos.get(1).nome());
        assertEquals("Merge Sort", todos.get(2).nome());
        assertEquals("Quick Sort", todos.get(3).nome());
    }
}
