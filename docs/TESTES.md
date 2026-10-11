# Testes unitários

Suíte JUnit 6.1.3 (Jupiter) do comparador de algoritmos de ordenação. Além do caminho feliz, os testes forçam entradas inválidas, casos de borda, problemas de codificação de arquivo e injeção de conteúdo no CSV, para encontrar pontos em que o programa quebrava.

## Como rodar

Pré-requisitos: JDK 17 ou superior no `PATH` (testado com JDK 25) e internet na primeira execução. O script baixa o JUnit para `lib/` e confere o checksum (SHA-256 e SHA-1).

```powershell
.\run-tests.ps1                          # todos os testes
.\run-tests.ps1 -ExcluirTag bug          # tudo que deve passar (exit 0)
.\run-tests.ps1 -IncluirTag algoritmos   # só os algoritmos de ordenação
.\run-tests.ps1 -ExcluirTag algoritmos   # tudo, menos os algoritmos
```

O script compila `src/` para `out/classes` e `test/` para `out/test-classes`, e grava o relatório XML em `out/test-reports`. Código de saída: 0 = tudo passou, 1 = houve falhas, 2 = nenhum teste encontrado.

Tags:

- `algoritmos`: contrato dos 4 algoritmos de ordenação.
- `bug`: comportamentos fora do escopo desta entrega. Ficam de fora com `-ExcluirTag bug`.

Os testes do `Main` simulam o teclado (`System.in`) e fazem backup e restauração de um `resultados.csv` que já exista na pasta.

## Organização da pasta `test/`

| Arquivo | O que cobre | Testes |
|---|---|---|
| `dados/GeradorDadosTest` | N = 0, 1 e 2, faixa de valores, semente fixa, N negativo, N máximo | 18 |
| `dados/LeitorArquivoTest` | CSV de exemplo; arquivo inexistente, vazio ou que é uma pasta; BOM; finais de linha CRLF/CR; Windows-1252; extremos de `int`; 100.000 linhas; índice de coluna | 29 |
| `benchmark/CronometroTest` | unidade em milissegundos, exceções do algoritmo, argumentos nulos | 9 |
| `saida/ExportadorCSVTest` | formato do arquivo, idioma do sistema (Locale), aspas e separadores (RFC 4180), injeção de fórmula, valores inválidos, arquivo já existente | 25 |
| `MainTest` | menu com entrada simulada, opções 1, 2 e 3, entrada inválida, fim da entrada (EOF) | 22 |
| `algoritmos/OrdenadorContratoTest` | classe abstrata: compara cada algoritmo com `Arrays.sort`, com limite de 10 s | (base) |
| `algoritmos/BubbleSortTest` | contrato aplicado ao Bubble Sort | 14 |
| `algoritmos/InsertionSortTest` | contrato aplicado ao Insertion Sort | 14 |
| `algoritmos/MergeSortTest` | contrato aplicado ao Merge Sort | 14 |
| `algoritmos/QuickSortTest` | contrato + 50.000 elementos já ordenados e 50.000 iguais (estouro de pilha) | 16 |
| `algoritmos/AlgoritmosNomesTest` | os 4 nomes são distintos | 1 |

Cenários do contrato dos algoritmos: vetor vazio, 1 elemento, já ordenado, invertido, todos iguais, duplicados, negativos, `Integer.MIN_VALUE`/`MAX_VALUE`, padrão serrilhado, vetor aleatório grande, reutilização da mesma instância e `ordenar(null)`.

## Resultado atual

162 testes. Com `.\run-tests.ps1 -ExcluirTag bug`, os 152 testes executados passam, entre eles os 59 dos algoritmos e os de regressão dos erros abaixo.

## Erros encontrados e corrigidos

| # | Classe/método | Entrada que quebrava | Antes | Depois | Teste |
|---|---|---|---|---|---|
| 1 | `Main`, opção 1 | N = `abc`, vazio ou `99999999999` | `NumberFormatException` derrubava o programa | avisa e encerra sem gravar CSV | `MainTest.opcao1NTexto`, `opcao1NVazio`, `opcao1NOverflow` |
| 2 | `Main`, opção 1 | N = `-5` | `NegativeArraySizeException` derrubava o programa | avisa e encerra sem gravar CSV | `MainTest.opcao1NNegativo` |
| 3 | `Main`, opção 2 | linha vazia, `3 a 1`, `1,5 2` | `NumberFormatException` derrubava o programa | avisa e encerra sem gravar CSV | `MainTest.opcao2LinhaVazia`, `opcao2ComLetras`, `opcao2ComDecimal` |
| 4 | `Main` | fim da entrada (Ctrl+Z ou entrada vazia) no menu ou depois da opção | `NoSuchElementException` | encerra de forma limpa | `MainTest.eofNoMenu`, `eofDepoisDaOpcao` |
| 5 | `Main`, opção 3 | CSV só com o cabeçalho | rodava o benchmark com 0 elementos e gravava `resultados.csv` | avisa e não grava (N = 0 na opção 1 continua aceito) | `MainTest.opcao3ArquivoSemLinhasValidas` |
| 6 | `LeitorArquivo.lerColunaNumerica` | CSV em Windows-1252 com acentos (salvo pelo Excel) | `MalformedInputException` | lê em UTF-8 e, se falhar, em Windows-1252 | `LeitorArquivoTest.arquivoWindows1252ComAcentos`, `MainTest.opcao3ArquivoWindows1252` |
| 7 | `LeitorArquivo.lerColunaNumerica` | `indiceColuna = -1` | `ArrayIndexOutOfBoundsException` | `IllegalArgumentException` com mensagem clara | `LeitorArquivoTest.indiceNegativo` |
| 8 | `GeradorDados.gerarAleatorio`, `gerarOrdenado`, `gerarInvertido` | N = -1, -100, `Integer.MIN_VALUE` | `NegativeArraySizeException` | `IllegalArgumentException` com mensagem clara | `GeradorDadosTest.aleatorioComNNegativo`, `ordenadoComNNegativo`, `invertidoComNNegativo` |
| 9 | `Cronometro.medir` | vetor `null` | chamava `ordenar(null)` e devolvia um tempo | `NullPointerException` antes de chamar `ordenar` | `CronometroTest.vetorNuloDeveSerRejeitado` |
| 10 | `ExportadorCSV.exportar` | lista `null` com o arquivo já existente | apagava o conteúdo do arquivo e depois lançava NPE | valida antes de abrir; arquivo fica intacto | `ExportadorCSVTest.listaNulaNaoPodeDestruirArquivo` |
| 11 | `ExportadorCSV.exportar` | texto com `;`, `"`, LF ou CRLF | colunas desalinhadas, linhas falsas, aspas perdidas | campo entre aspas, com `"` duplicada (RFC 4180) | `ExportadorCSVTest.injecaoPontoEVirgulaNoAlgoritmo`, `injecaoPontoEVirgulaNaDisposicao`, `injecaoQuebraDeLinhaLf`, `injecaoQuebraDeLinhaCrlf`, `injecaoAspas` |
| 12 | `ExportadorCSV.exportar` | nome começando com `=`, `+`, `-` ou `@` | virava fórmula ao abrir na planilha (CSV injection) | prefixo `'` nos campos de texto; números negativos não mudam | `ExportadorCSVTest.injecaoDeFormula` |
| 13 | `ExportadorCSV.exportar` | sistema em árabe (`ar-SA`) ou tailandês (`th-TH-u-nu-thai`) | tempo com dígitos não ASCII (`١٫٥٠٠`, `๑,๕๐๐`) | sempre `1,500` (`Locale.ROOT` + troca de `.` por `,`) | `ExportadorCSVTest.localeComDigitosNaoAscii` |
| 14 | `ExportadorCSV.exportar` | algoritmo `null` | gravava a palavra `null` como nome | `NullPointerException` antes de abrir o arquivo | `ExportadorCSVTest.algoritmoNulo` |
| 15 | `ExportadorCSV.exportar` | tempo `NaN` ou `±Infinity` | gravava `NaN`/`Infinity` | `IllegalArgumentException` | `ExportadorCSVTest.tempoNaoFinito` |

## Algoritmos de ordenação

Os 4 algoritmos foram implementados em `src/algoritmos/` e passam no contrato completo:

- **Bubble Sort:** para assim que uma passada não faz nenhuma troca, então um vetor já ordenado custa O(n).
- **Insertion Sort:** desloca para a direita os elementos maiores e insere a chave no espaço que sobra.
- **Merge Sort:** é estável, usa um único vetor auxiliar e custa O(n log n) em todos os casos.
- **Quick Sort:** escolhe o pivô pela mediana de três e separa o trecho em três partes (menores, iguais e maiores). A recursão vai só na parte menor, o que evita estouro de pilha com vetores já ordenados ou com todos os valores iguais.
