package algoritmos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Contrato que TODO algoritmo de ordenação do projeto precisa cumprir.
 *
 * Classe abstrata: cada algoritmo tem uma subclasse (BubbleSortTest, MergeSortTest...)
 * que só diz qual Ordenador criar. Todos os testes abaixo rodam para cada algoritmo.
 *
 * Os cenários nasceram em TDD, antes da implementação; a referência de resposta certa
 * é sempre Arrays.sort.
 */
@Tag("algoritmos")
abstract class OrdenadorContratoTest {

    /** Tempo máximo por cenário: um algoritmo em laço infinito não trava a suíte. */
    protected static final Duration LIMITE = Duration.ofSeconds(10);

    /** Semente fixa: os vetores "aleatórios" são sempre os mesmos (teste determinístico). */
    protected final Random random = new Random(12345);

    /** Cada subclasse devolve uma instância NOVA do algoritmo testado. */
    protected abstract Ordenador criarOrdenador();

    /** Tamanho do teste aleatório grande (algoritmos O(n log n) podem usar valores maiores). */
    protected int tamanhoGrande() {
        return 5_000;
    }

    /**
     * Ordena uma cópia com o algoritmo e compara com Arrays.sort.
     * assertArrayEquals garante ao mesmo tempo a ORDEM e que nenhum elemento sumiu ou foi duplicado.
     */
    protected void verificarOrdenacao(int[] entrada) {
        int[] esperado = entrada.clone();
        Arrays.sort(esperado);
        int[] obtido = entrada.clone();
        Ordenador ordenador = criarOrdenador();
        assertTimeoutPreemptively(LIMITE, () -> ordenador.ordenar(obtido));
        assertArrayEquals(esperado, obtido, ordenador.nome() + " não ordenou corretamente");
    }

    // -----------------------------------------------------------------------
    // Cenários triviais
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Vetor vazio não lança exceção e continua vazio")
    void vetorVazio() {
        int[] v = new int[0];
        assertDoesNotThrow(() -> criarOrdenador().ordenar(v));
        assertEquals(0, v.length);
    }

    @Test
    @DisplayName("Vetor com 1 elemento")
    void umElemento() {
        verificarOrdenacao(new int[] {42});
    }

    @Test
    @DisplayName("Vetor já ordenado 1..100")
    void jaOrdenado() {
        int[] v = new int[100];
        for (int i = 0; i < v.length; i++) {
            v[i] = i + 1;
        }
        verificarOrdenacao(v);
    }

    @Test
    @DisplayName("Todos os elementos iguais (7 x 100)")
    void todosIguais() {
        int[] v = new int[100];
        Arrays.fill(v, 7);
        verificarOrdenacao(v);
    }

    @Test
    @DisplayName("nome() não é nulo, não é vazio e é estável entre chamadas")
    void nomeValido() {
        Ordenador o = criarOrdenador();
        String nome = o.nome();
        assertNotNull(nome);
        assertFalse(nome.isBlank());
        assertEquals(nome, o.nome());
    }

    // -----------------------------------------------------------------------
    // Cenários que exigem ordenação real
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Dois elementos invertidos {2, 1}")
    void doisElementosInvertidos() {
        verificarOrdenacao(new int[] {2, 1});
    }

    @Test
    @DisplayName("Vetor invertido 100..1")
    void invertido() {
        int[] v = new int[100];
        for (int i = 0; i < v.length; i++) {
            v[i] = 100 - i;
        }
        verificarOrdenacao(v);
    }

    @Test
    @DisplayName("Duplicados embaralhados {5,3,5,1,3,1}")
    void duplicadosEmbaralhados() {
        verificarOrdenacao(new int[] {5, 3, 5, 1, 3, 1});
    }

    @Test
    @DisplayName("Negativos misturados {-1,-50,3,0,-7}")
    void negativosMisturados() {
        verificarOrdenacao(new int[] {-1, -50, 3, 0, -7});
    }

    @Test
    @DisplayName("Extremos MAX/MIN (pega comparação por subtração a - b, que estoura)")
    void extremosDeInt() {
        verificarOrdenacao(new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1, Integer.MAX_VALUE, Integer.MIN_VALUE});
    }

    @Test
    @DisplayName("Padrão serrilhado/'órgão' 1,3,5,...,6,4,2")
    void padraoOrgao() {
        int n = 101;
        int[] v = new int[n];
        int k = 0;
        for (int i = 1; i <= n; i += 2) {
            v[k++] = i;
        }
        for (int i = (n % 2 == 0) ? n : n - 1; i >= 2; i -= 2) {
            v[k++] = i;
        }
        verificarOrdenacao(v);
    }

    @Test
    @DisplayName("Vetor aleatório grande comparado com Arrays.sort")
    void aleatorioGrande() {
        int[] v = new int[tamanhoGrande()];
        for (int i = 0; i < v.length; i++) {
            v[i] = random.nextInt();
        }
        verificarOrdenacao(v);
    }

    @Test
    @DisplayName("Mesma instância ordena dois vetores seguidos (sem estado residual)")
    void mesmaInstanciaDuasVezes() {
        Ordenador o = criarOrdenador();
        int[] a = {9, 8, 7, 6, 5};
        int[] b = {3, -3, 2, -2, 1, -1, 0};
        assertTimeoutPreemptively(LIMITE, () -> {
            o.ordenar(a);
            o.ordenar(b);
        });
        assertArrayEquals(new int[] {5, 6, 7, 8, 9}, a);
        assertArrayEquals(new int[] {-3, -2, -1, 0, 1, 2, 3}, b);
    }

    @Test
    @DisplayName("ordenar(null) lança NullPointerException (igual a Arrays.sort(null))")
    void vetorNulo() {
        assertThrows(NullPointerException.class, () -> criarOrdenador().ordenar(null));
    }
}
