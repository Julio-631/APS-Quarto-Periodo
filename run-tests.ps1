<#
.SYNOPSIS
    Compila o projeto Java (src/ e test/) e executa os testes unitários com JUnit.

.DESCRIPTION
    Projeto Java puro (sem Maven/Gradle). Este script:
      1. Baixa o JUnit (junit-platform-console-standalone, versão FIXADA) para lib/,
         caso ainda não exista, e confere a integridade (SHA-256 fixado + SHA-1 publicado).
      2. Compila src/ para out/classes e test/ para out/test-classes.
      3. Executa o Console Launcher do JUnit e grava os relatórios XML em out/test-reports.
      4. Devolve o código de saída do JUnit (0 = tudo passou, 1 = houve falhas, 2 = nenhum teste).

    Funciona no PowerShell 7 (pwsh) e no Windows PowerShell 5.1, inclusive com
    caminhos que contêm espaços.

.PARAMETER IncluirTag
    Executa apenas os testes com estas tags (ex.: -IncluirTag bug).

.PARAMETER ExcluirTag
    Não executa os testes com estas tags (ex.: -ExcluirTag bug,algoritmos).

.EXAMPLE
    .\run-tests.ps1
    .\run-tests.ps1 -ExcluirTag algoritmos
    .\run-tests.ps1 -IncluirTag bug
#>
[CmdletBinding()]
param(
    [string[]]$IncluirTag = @(),
    [string[]]$ExcluirTag = @()
)

$ErrorActionPreference = 'Stop'
# Barra de progresso deixa o download muito lento no Windows PowerShell 5.1.
$ProgressPreference = 'SilentlyContinue'
# Maven Central exige TLS 1.2 (o 5.1 pode vir com protocolos antigos por padrão).
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

# Via "-File", "bug,algoritmos" chega como UMA string; aqui separamos por vírgula.
function Normalizar-Tags([string[]]$tags) {
    return @($tags | ForEach-Object { $_ -split ',' } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}
$IncluirTag = Normalizar-Tags $IncluirTag
$ExcluirTag = Normalizar-Tags $ExcluirTag

# ---------------------------------------------------------------------------
# Caminhos (sempre derivados da pasta do script, nunca do diretório atual)
# ---------------------------------------------------------------------------
$VersaoJUnit    = '6.1.3'
$Sha256Esperado = 'E62B96AC475DBCDE8599EA905D088F65D90778F86E259B856A49FA5C4EA256EC'
$UrlBase        = "https://repo1.maven.org/maven2/org/junit/platform/junit-platform-console-standalone/$VersaoJUnit"
$NomeJar        = "junit-platform-console-standalone-$VersaoJUnit.jar"

$raiz       = $PSScriptRoot
$lib        = Join-Path $raiz 'lib'
$jar        = Join-Path $lib $NomeJar
$out        = Join-Path $raiz 'out'
$outClasses = Join-Path $out 'classes'
$outTest    = Join-Path $out 'test-classes'
$reports    = Join-Path $out 'test-reports'
$pastaSrc   = Join-Path $raiz 'src'
$pastaTest  = Join-Path $raiz 'test'

# ---------------------------------------------------------------------------
# Download e verificação do JUnit
# ---------------------------------------------------------------------------
function Obter-HashArquivo([string]$caminho, [string]$algoritmo) {
    return (Get-FileHash -LiteralPath $caminho -Algorithm $algoritmo).Hash
}

function Obter-JUnit {
    if (Test-Path -LiteralPath $jar) {
        if ((Obter-HashArquivo $jar 'SHA256') -ieq $Sha256Esperado) {
            Write-Host "JUnit $VersaoJUnit encontrado em lib\ (checksum OK)."
            return
        }
        Write-Warning "O jar em lib\ está com checksum diferente do esperado. Apagando e baixando de novo."
        Remove-Item -LiteralPath $jar -Force
    }

    if (-not (Test-Path -LiteralPath $lib)) {
        New-Item -ItemType Directory -Path $lib | Out-Null
    }

    $parcial = "$jar.part"
    $sha1Tmp = "$jar.sha1.tmp"
    Write-Host "Baixando JUnit $VersaoJUnit do Maven Central..."
    try {
        try {
            Invoke-WebRequest -UseBasicParsing -Uri "$UrlBase/$NomeJar" -OutFile $parcial
            Invoke-WebRequest -UseBasicParsing -Uri "$UrlBase/$NomeJar.sha1" -OutFile $sha1Tmp
        } catch {
            Write-Host "ERRO: não foi possível baixar o JUnit (sem internet?). Detalhe: $($_.Exception.Message)" -ForegroundColor Red
            exit 1
        }

        # O arquivo .sha1 contém o hash em hexadecimal (às vezes seguido do nome do arquivo).
        $sha1Remoto = ((Get-Content -LiteralPath $sha1Tmp -Raw).Trim() -split '\s+')[0]
        $sha256Real = Obter-HashArquivo $parcial 'SHA256'
        $sha1Real   = Obter-HashArquivo $parcial 'SHA1'

        if ($sha256Real -ine $Sha256Esperado) {
            Write-Host "ERRO: SHA-256 do download não confere. Esperado $Sha256Esperado, obtido $sha256Real." -ForegroundColor Red
            exit 1
        }
        if ($sha1Real -ine $sha1Remoto) {
            Write-Host "ERRO: SHA-1 do download não confere com o publicado. Esperado $sha1Remoto, obtido $sha1Real." -ForegroundColor Red
            exit 1
        }

        Move-Item -LiteralPath $parcial -Destination $jar -Force
        Write-Host "Download concluído e verificado (SHA-256 e SHA-1 OK)."
    } finally {
        # Remove temporários (o .part só sobra se a verificação falhou).
        foreach ($tmp in @($parcial, $sha1Tmp)) {
            if (Test-Path -LiteralPath $tmp) { Remove-Item -LiteralPath $tmp -Force }
        }
    }
}

# ---------------------------------------------------------------------------
# Pré-requisitos
# ---------------------------------------------------------------------------
foreach ($cmd in @('java', 'javac')) {
    if (-not (Get-Command $cmd -ErrorAction SilentlyContinue)) {
        Write-Host "ERRO: '$cmd' não encontrado no PATH. Instale um JDK (17 ou superior) e tente de novo." -ForegroundColor Red
        exit 1
    }
}

Obter-JUnit

# ---------------------------------------------------------------------------
# Compilação
# ---------------------------------------------------------------------------
# Limpa apenas as pastas de build (out\src-hash-*.csv e outros arquivos de out\ são preservados).
foreach ($pasta in @($outClasses, $outTest, $reports)) {
    if (Test-Path -LiteralPath $pasta) { Remove-Item -LiteralPath $pasta -Recurse -Force }
    New-Item -ItemType Directory -Path $pasta | Out-Null
}

$fontes = @(Get-ChildItem -LiteralPath $pastaSrc -Recurse -Filter *.java | ForEach-Object { $_.FullName })
Write-Host "Compilando src\ ($($fontes.Count) arquivos)..."
& javac -encoding UTF-8 -d $outClasses $fontes
if ($LASTEXITCODE -ne 0) {
    Write-Host "ERRO: falha ao compilar src\." -ForegroundColor Red
    exit 1
}

$fontesTeste = @()
if (Test-Path -LiteralPath $pastaTest) {
    $fontesTeste = @(Get-ChildItem -LiteralPath $pastaTest -Recurse -Filter *.java | ForEach-Object { $_.FullName })
}
if ($fontesTeste.Count -gt 0) {
    Write-Host "Compilando test\ ($($fontesTeste.Count) arquivos)..."
    $cpTeste = $outClasses + [IO.Path]::PathSeparator + $jar
    & javac -encoding UTF-8 -cp $cpTeste -d $outTest $fontesTeste
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERRO: falha ao compilar test\." -ForegroundColor Red
        exit 1
    }
}

# ---------------------------------------------------------------------------
# Execução dos testes
# ---------------------------------------------------------------------------
$argsJava = @(
    '-Dstdout.encoding=UTF-8',
    '-Dstderr.encoding=UTF-8',
    '-jar', $jar,
    'execute',
    '--disable-banner',
    '--fail-if-no-tests',
    '--details=tree',
    '--class-path', ($outClasses + [IO.Path]::PathSeparator + $outTest),
    "--scan-classpath=$outTest",
    "--reports-dir=$reports"
)
foreach ($t in $IncluirTag) { $argsJava += "--include-tag=$t" }
foreach ($t in $ExcluirTag) { $argsJava += "--exclude-tag=$t" }

if ($IncluirTag.Count -gt 0) { Write-Host "Incluindo apenas as tags: $($IncluirTag -join ', ')" }
if ($ExcluirTag.Count -gt 0) { Write-Host "Excluindo as tags: $($ExcluirTag -join ', ')" }

$encodingAnterior = $null
try { $encodingAnterior = [Console]::OutputEncoding } catch { }

# O Main grava resultados.csv relativo ao diretório de trabalho: roda a partir da raiz.
Push-Location -LiteralPath $raiz
try {
    try { [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false) } catch { }
    & java $argsJava
    $codigoJava = $LASTEXITCODE
} finally {
    Pop-Location
    if ($null -ne $encodingAnterior) {
        try { [Console]::OutputEncoding = $encodingAnterior } catch { }
    }
}

# ---------------------------------------------------------------------------
# Resumo
# ---------------------------------------------------------------------------
$xmlRelatorio = Join-Path $reports 'TEST-junit-jupiter.xml'
if (Test-Path -LiteralPath $xmlRelatorio) {
    [xml]$xml = Get-Content -LiteralPath $xmlRelatorio -Raw -Encoding UTF8
    $s = $xml.testsuite
    Write-Host ""
    Write-Host "Resumo: $($s.tests) testes, $($s.failures) falhas, $($s.errors) erros, $($s.skipped) ignorados"
    Write-Host "Obs.: falhas em testes da tag 'bug' são esperadas: descrevem comportamentos ainda não corrigidos."
}

exit $codigoJava
