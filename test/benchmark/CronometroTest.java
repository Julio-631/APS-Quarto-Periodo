package benchmark;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import algoritmos.BubbleSort;
import algoritmos.Ordenador;

/**
 * Testes da classe Cronometro.
 *
 * Usa um "ordenador falso" (OrdenadorFalso) que não ordena nada: só conta quantas
 * vezes foi chamado, guarda o vetor recebido e executa uma ação configurável
 * (esperar alguns milissegundos, lançar uma exceção, etc.). Assim testamos o
 * cronômetro isoladamente, sem depender dos algoritmos reais.
 */
@DisplayName("Cronometro")
class CronometroTest {

    private final Cronometro cronometro = new Cronometro();

    /** Ordenador de mentira, controlado pelo teste. */
    private static class OrdenadorFalso implements Ordenador {
        int chamadas = 0;
        int[] recebido;
        private final Runnable acao;

        OrdenadorFalso() {
            this(() -> { });
        }

        OrdenadorFalso(Runnable acao) {
            this.acao = acao;
        }

        @Override
        public void ordenar(int[] vetor) {
            chamadas++;
            recebido = vetor;
            acao.run();
        }

        @Override
        public String nome() {
            return "Falso";
        }
    }

    // -----------------------------------------------------------------------
    // Bordas aceitáveis
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Tempo medido é >= 0, finito e não NaN")
    void tempoNaoNegativoEFinito() {
        double t = cronometro.medir(new OrdenadorFalso(), new int[] {3, 1, 2});
        assertAll(
                () -> assertTrue(t >= 0, "tempo negativo: " + t),
                () -> assertTrue(!Double.isNaN(t), "tempo NaN"),
                () -> assertTrue(!Double.isInfinite(t), "tempo infinito"));
    }

    @Test
    @DisplayName("Vetor vazio: tempo >= 0")
    void vetorVazio() {
        assertTrue(cronometro.medir(new OrdenadorFalso(), new int[0]) >= 0);
    }

    @Test
    @DisplayName("Repassa a MESMA instância do vetor e chama ordenar() exatamente 1 vez")
    void repassaMesmoVetorUmaVez() {
        OrdenadorFalso falso = new OrdenadorFalso();
        int[] vetor = {5, 4, 3};
        cronometro.medir(falso, vetor);
        assertSame(vetor, falso.recebido);
        assertEquals(1, falso.chamadas);
    }

    @Test
    @DisplayName("Unidade é milissegundo: ordenação que gasta >= 5 ms resulta em >= 5.0 (e não em nanossegundos)")
    void unidadeEmMilissegundos() {
        // Espera ocupada (sem Thread.sleep) até o relógio avançar 5 ms.
        OrdenadorFalso lento = new OrdenadorFalso(() -> {
            long inicio = System.nanoTime();
            while (System.nanoTime() - inicio < 5_000_000L) {
                // gira até passar 5 ms
            }
        });
        double t = cronometro.medir(lento, new int[] {1});
        assertTrue(t >= 5.0, "esperado >= 5 ms, obtido " + t);
        assertTrue(t < 60_000, "valor alto demais, parece estar em nanossegundos: " + t);
    }

    @Test
    @DisplayName("Exceção lançada pelo ordenador é propagada sem ser engolida")
    void propagaExcecaoDoOrdenador() {
        IllegalStateException erro = new IllegalStateException("falha proposital");
        IllegalStateException capturada = assertThrows(IllegalStateException.class,
                () -> cronometro.medir(new OrdenadorFalso(() -> { throw erro; }), new int[] {1}));
        assertSame(erro, capturada);
    }

    @Test
    @DisplayName("StackOverflowError do ordenador (ex.: recursão infinita) é propagado")
    void propagaStackOverflow() {
        assertThrows(StackOverflowError.class,
                () -> cronometro.medir(new OrdenadorFalso(() -> { throw new StackOverflowError(); }), new int[] {1}));
    }

    @Test
    @DisplayName("Ordenador null lança NullPointerException")
    void ordenadorNulo() {
        assertThrows(NullPointerException.class, () -> cronometro.medir(null, new int[] {1}));
    }

    @Test
    @DisplayName("Com o BubbleSort real e 1.000 elementos o tempo é >= 0")
    void comBubbleSortReal() {
        int[] v = new int[1000];
        for (int i = 0; i < v.length; i++) {
            v[i] = v.length - i;
        }
        assertTrue(cronometro.medir(new BubbleSort(), v) >= 0);
    }

    // -----------------------------------------------------------------------
    // Regressão: defeitos corrigidos
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Vetor null é rejeitado (NullPointerException) antes de chamar ordenar()")
    void vetorNuloDeveSerRejeitado() {
        // Justificativa: medir a ordenação de "nada" não tem significado; antes da correção
        // o Cronometro devolvia um número, dando a falsa impressão de medição bem-sucedida.
        // Agora ele valida o argumento e não chama o ordenador.
        OrdenadorFalso aceitaNull = new OrdenadorFalso();
        Throwable t = assertThrows(RuntimeException.class, () -> cronometro.medir(aceitaNull, null),
                "medir(ordenador, null) devolveu um tempo em vez de rejeitar o vetor nulo");
        assertTrue(t instanceof NullPointerException || t instanceof IllegalArgumentException,
                "tipo de exceção inesperado: " + t);
        assertEquals(0, aceitaNull.chamadas, "ordenar() não deveria ter sido chamado");
    }
}
