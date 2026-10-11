package dados;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Testes da classe GeradorDados.
 *
 * A primeira parte documenta comportamentos de borda que já estavam corretos.
 * A seção final são testes de regressão de defeitos já corrigidos (N negativo):
 * eles afirmam o comportamento robusto e garantem que o defeito não volte.
 */
@DisplayName("GeradorDados")
class GeradorDadosTest {

    private final GeradorDados gerador = new GeradorDados();

    // -----------------------------------------------------------------------
    // Bordas aceitáveis
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("N = 0 devolve vetor vazio nas três disposições")
    void nZeroDevolveVetorVazio() {
        assertAll(
                () -> assertEquals(0, gerador.gerarAleatorio(0).length, "aleatório"),
                () -> assertEquals(0, gerador.gerarOrdenado(0).length, "ordenado"),
                () -> assertEquals(0, gerador.gerarInvertido(0).length, "invertido"));
    }

    @Test
    @DisplayName("N = 1 devolve um único elemento válido em cada disposição")
    void nUmDevolveUmElemento() {
        int[] aleatorio = gerador.gerarAleatorio(1);
        assertAll(
                () -> assertArrayEquals(new int[] {0}, gerador.gerarOrdenado(1)),
                () -> assertArrayEquals(new int[] {0}, gerador.gerarInvertido(1)),
                () -> assertEquals(1, aleatorio.length),
                () -> assertTrue(aleatorio[0] >= 0 && aleatorio[0] <= 999_999, "fora da faixa: " + aleatorio[0]));
    }

    @Test
    @DisplayName("N = 2 gera [0,1] ordenado e [1,0] invertido")
    void nDoisOrdenadoEInvertido() {
        assertArrayEquals(new int[] {0, 1}, gerador.gerarOrdenado(2));
        assertArrayEquals(new int[] {1, 0}, gerador.gerarInvertido(2));
    }

    @Test
    @DisplayName("Ordenado com N = 1000 é 0..999 e o invertido é exatamente o reverso")
    void ordenadoEInvertidoSaoReversos() {
        int[] ordenado = gerador.gerarOrdenado(1000);
        int[] invertido = gerador.gerarInvertido(1000);
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, ordenado[i], "ordenado na posição " + i);
            assertEquals(ordenado[999 - i], invertido[i], "invertido na posição " + i);
        }
    }

    @Test
    @DisplayName("Aleatório com N = 10.000 fica sempre na faixa [0, 999999]")
    void aleatorioDentroDaFaixa() {
        for (int v : gerador.gerarAleatorio(10_000)) {
            assertTrue(v >= 0 && v <= 999_999, "valor fora da faixa: " + v);
        }
    }

    @Test
    @DisplayName("Semente fixa: duas instâncias novas geram o mesmo vetor aleatório")
    void sementeFixaEhDeterministica() {
        assertArrayEquals(new GeradorDados().gerarAleatorio(500), new GeradorDados().gerarAleatorio(500));
    }

    @Test
    @DisplayName("Duas chamadas na MESMA instância geram vetores diferentes (o Random guarda estado)")
    void mesmaInstanciaGeraVetoresDiferentes() {
        int[] primeiro = gerador.gerarAleatorio(500);
        int[] segundo = gerador.gerarAleatorio(500);
        assertFalse(Arrays.equals(primeiro, segundo));
    }

    @Test
    @DisplayName("Aleatório com N = 1000 não sai ordenado por acaso")
    void aleatorioNaoEstaOrdenado() {
        int[] v = gerador.gerarAleatorio(1000);
        int[] copia = v.clone();
        Arrays.sort(copia);
        assertFalse(Arrays.equals(v, copia));
    }

    @Test
    @DisplayName("N = Integer.MAX_VALUE: não há limite na classe e a JVM rejeita com OutOfMemoryError (sem alocar)")
    void nMaximoEstouraLimiteDaJvm() {
        // Documenta: a JVM recusa o tamanho na hora ("Requested array size exceeds VM limit").
        assertThrows(OutOfMemoryError.class, () -> gerador.gerarOrdenado(Integer.MAX_VALUE));
    }

    // -----------------------------------------------------------------------
    // Regressão: defeitos corrigidos
    // -----------------------------------------------------------------------

    // Justificativa (vale para os três testes abaixo): N negativo é argumento inválido;
    // a convenção Java é IllegalArgumentException com mensagem clara. Antes da correção
    // vazava NegativeArraySizeException, um detalhe interno da implementação.

    @ParameterizedTest(name = "N = {0}")
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    @DisplayName("gerarAleatorio com N negativo lança IllegalArgumentException")
    void aleatorioComNNegativo(int n) {
        assertThrows(IllegalArgumentException.class, () -> gerador.gerarAleatorio(n));
    }

    @ParameterizedTest(name = "N = {0}")
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    @DisplayName("gerarOrdenado com N negativo lança IllegalArgumentException")
    void ordenadoComNNegativo(int n) {
        assertThrows(IllegalArgumentException.class, () -> gerador.gerarOrdenado(n));
    }

    @ParameterizedTest(name = "N = {0}")
    @ValueSource(ints = {-1, -100, Integer.MIN_VALUE})
    @DisplayName("gerarInvertido com N negativo lança IllegalArgumentException")
    void invertidoComNNegativo(int n) {
        assertThrows(IllegalArgumentException.class, () -> gerador.gerarInvertido(n));
    }
}
