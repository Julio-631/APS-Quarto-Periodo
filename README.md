# Comparador de Algoritmos de Ordenação

Programa de console em Java puro que compara o desempenho de quatro algoritmos de ordenação (Bubble Sort, Insertion Sort, Merge Sort e Quick Sort). Ele ordena o mesmo conjunto de números com cada algoritmo, mede o tempo de cada um e grava os resultados em um arquivo CSV para análise.

Projeto de APS (Atividades Práticas Supervisionadas) do quarto período.

## O que o programa faz

1. Pergunta de onde vêm os números a ordenar (gerar aleatório, digitar à mão ou ler de um arquivo CSV).
2. Monta o vetor de entrada.
3. Ordena uma cópia desse vetor com cada um dos quatro algoritmos, cronometrando apenas a ordenação (a geração ou leitura dos dados não entra na medição).
4. Mostra os tempos na tela e grava um `resultados.csv` com as colunas `algoritmo;tamanho;disposicao;tempo_ms`.

## Como executar

Pré-requisito: JDK 17 ou superior no `PATH` (testado com JDK 25).

```powershell
# 1. Compilar
javac -d out/classes (Get-ChildItem -Recurse -Filter *.java -Path src).FullName

# 2. Executar
java -cp out/classes Main
```

## Como usar o menu

O programa primeiro pergunta a opção; só depois pede o dado correspondente.

```
Escolha uma opcao: 1
Informe o tamanho N do vetor: 50000
```

- **Opção 1 — Gerar vetor aleatório:** informe o tamanho `N` (um número inteiro). O programa cria `N` valores aleatórios. É a melhor opção para comparar a velocidade: com `N` grande (ex.: `50000`), o Bubble Sort e o Insertion Sort ficam visivelmente mais lentos que o Merge Sort e o Quick Sort.
- **Opção 2 — Digitar manualmente:** informe os números inteiros separados por espaço, ex.: `9 3 7 1 5`. Aceita negativos e repetidos. Útil para testar com uma lista pequena.
- **Opção 3 — Carregar de arquivo CSV:** informe o caminho de um CSV no formato `id;data;coordenada;tamanho_kb`. O programa lê a coluna `tamanho_kb`. Há um exemplo pronto em `dados_entrada/imagens_satelite.csv`.

Entradas inválidas (texto onde se espera número, `N` negativo ou grande demais para `int`, linha vazia) são recusadas com uma mensagem, sem derrubar o programa.

## Os algoritmos

- **Bubble Sort** — compara vizinhos e troca; para antes se o vetor já está ordenado. O(n²) no pior caso.
- **Insertion Sort** — insere cada elemento na posição certa entre os já ordenados. O(n²) no pior caso, rápido em listas quase ordenadas.
- **Merge Sort** — divide ao meio e intercala; estável. O(n log n) sempre.
- **Quick Sort** — particiona em torno de um pivô (mediana de três, com partição em três partes). O(n log n) em média.

## Estrutura do projeto

- `src/` — código do programa (Java puro, console):
  - `Main.java` — menu de console e orquestração.
  - `dados/` — geração (`GeradorDados`) e leitura de CSV (`LeitorArquivo`).
  - `algoritmos/` — interface `Ordenador` e os quatro algoritmos.
  - `benchmark/` — `Cronometro`, mede o tempo de ordenação.
  - `saida/` — `ExportadorCSV`, grava o `resultados.csv`.
- `test/` — testes unitários (JUnit), espelhando os pacotes de `src/`.
- `dados_entrada/` — arquivo CSV de exemplo para a opção 3.
- `docs/TESTES.md` — documentação da suíte de testes e dos erros corrigidos.
- `run-tests.ps1` — compila e executa os testes.
- `setup-git-github/` — script e guia para instalar e configurar o Git e o GitHub no Windows.

## Testes

Pré-requisito: JDK 17 ou superior no `PATH` (testado com JDK 25). Na primeira execução o script
baixa o JUnit 6.1.3 do Maven Central para `lib/` e confere o checksum (precisa de internet).

```powershell
.\run-tests.ps1                               # todos os testes
.\run-tests.ps1 -ExcluirTag algoritmos        # sem os testes dos algoritmos
.\run-tests.ps1 -ExcluirTag bug               # tudo que deve passar (exit 0)
.\run-tests.ps1 -IncluirTag bug               # só os testes de comportamentos pendentes
```

Tags:

- `bug` — comportamentos fora do escopo desta entrega. Ficam de fora com `-ExcluirTag bug`.
- `algoritmos` — contrato dos algoritmos de ordenação, comparados com `Arrays.sort`.

O script devolve código de saída 0 quando tudo passa e 1 quando há falhas.

Detalhes em [docs/TESTES.md](docs/TESTES.md).
