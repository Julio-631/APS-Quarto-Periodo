<#
.SYNOPSIS
    Instala e configura o Git e o GitHub CLI no Windows e conecta o PC à sua conta do GitHub.

.DESCRIPTION
    Etapas executadas:
      0. Verificações iniciais (Windows, PowerShell, winget, internet)
      1. Instala o Git (se ainda não estiver instalado)
      2. Instala o GitHub CLI - "gh" (se ainda não estiver instalado)
      3. Faz login no GitHub (pelo navegador ou com token)
      4. Configura sua identidade no Git (nome e e-mail dos commits)
      5. Aplica configurações recomendadas (branch "main", quebras de linha, editor)
      6. Verificação final e resumo

    O script pode ser executado mais de uma vez sem problema: o que já estiver
    instalado/configurado é detectado e mantido.

.NOTES
    Como executar (escolha uma opção):
      - Clique com o botão direito no arquivo > "Executar com o PowerShell"
      - Ou, num terminal PowerShell, dentro da pasta do arquivo:
          powershell -ExecutionPolicy Bypass -File .\Configurar-Git-GitHub.ps1

    Um log da execução é salvo em %TEMP% (o caminho aparece no fim do script).
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$script:LogPath   = Join-Path $env:TEMP ("configurar-git-github_{0:yyyyMMdd_HHmmss}.log" -f (Get-Date))
$script:TemWinget = $false

# ---------------------------------------------------------------------------
# Funções de apoio (mensagens coloridas e perguntas)
# ---------------------------------------------------------------------------

function Write-Etapa {
    param([string]$Numero, [string]$Titulo)
    Write-Host ""
    Write-Host ("=" * 70) -ForegroundColor DarkCyan
    Write-Host (" ETAPA {0}  -  {1}" -f $Numero, $Titulo) -ForegroundColor Cyan
    Write-Host ("=" * 70) -ForegroundColor DarkCyan
}

function Write-Ok    { param([string]$Texto) Write-Host "  [OK]  $Texto" -ForegroundColor Green }
function Write-Info  { param([string]$Texto) Write-Host "  [..]  $Texto" -ForegroundColor Gray }
function Write-Aviso { param([string]$Texto) Write-Host "  [!!]  $Texto" -ForegroundColor Yellow }
function Write-Erro  { param([string]$Texto) Write-Host "  [XX]  $Texto" -ForegroundColor Red }
function Write-SuaVez { param([string]$Texto) Write-Host "  >>> SUA VEZ: $Texto" -ForegroundColor Magenta }

function Read-SimNao {
    param([string]$Pergunta, [bool]$Padrao = $true)
    $sufixo = if ($Padrao) { '[S/n]' } else { '[s/N]' }
    while ($true) {
        $resp = Read-Host "  $Pergunta $sufixo"
        if ($null -eq $resp) { $resp = '' }
        $resp = $resp.Trim().ToLower()
        if ($resp -eq '') { return $Padrao }
        if (@('s', 'sim', 'y', 'yes') -contains $resp) { return $true }
        if (@('n', 'nao', 'não', 'no') -contains $resp) { return $false }
        Write-Aviso "Responda S (sim) ou N (não)."
    }
}

function Read-ComPadrao {
    param([string]$Pergunta, [string]$Padrao)
    if ($Padrao) { $resp = Read-Host "  $Pergunta [$Padrao]" }
    else         { $resp = Read-Host "  $Pergunta" }
    if ([string]::IsNullOrWhiteSpace($resp)) { return $Padrao }
    return $resp.Trim()
}

function Test-Comando {
    param([string]$Nome)
    return [bool](Get-Command $Nome -ErrorAction SilentlyContinue)
}

# Recarrega o PATH na sessão atual, para enxergar programas recém-instalados
# sem precisar fechar e abrir o terminal.
function Update-PathSessao {
    $machine = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $user    = [Environment]::GetEnvironmentVariable('Path', 'User')
    $env:Path = "$machine;$user"
    $extras = @(
        (Join-Path $env:ProgramFiles 'Git\cmd'),
        (Join-Path $env:ProgramFiles 'GitHub CLI'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Git\cmd')
    )
    foreach ($p in $extras) {
        if ((Test-Path $p) -and (($env:Path -split ';') -notcontains $p)) {
            $env:Path += ";$p"
        }
    }
}

# Garante que uma ferramenta esteja instalada: tenta pelo winget e,
# se não der, abre a página de download para instalação manual.
function Install-Ferramenta {
    param(
        [string]$Comando,
        [string]$WingetId,
        [string]$Nome,
        [string]$UrlManual
    )

    Update-PathSessao
    if (Test-Comando $Comando) {
        Write-Ok "$Nome já está instalado."
        return $true
    }

    Write-Info "$Nome não encontrado. Vamos instalar."

    if ($script:TemWinget) {
        Write-Info "Instalando $Nome pelo winget. Isso pode levar alguns minutos..."
        Write-SuaVez "Se o Windows perguntar 'Deseja permitir que este aplicativo faça alterações?', clique em SIM."
        & winget install --id $WingetId -e --source winget --accept-package-agreements --accept-source-agreements --silent | Out-Host
        Update-PathSessao
    }

    if (-not (Test-Comando $Comando)) {
        Write-Aviso "Não consegui instalar o $Nome automaticamente."
        Write-SuaVez "Vou abrir a página de download. Baixe, instale (pode aceitar as opções padrão) e volte para esta janela."
        Start-Process $UrlManual
        Read-Host "  Depois de terminar a instalação, pressione ENTER para continuar" | Out-Null
        Update-PathSessao
    }

    if (Test-Comando $Comando) {
        Write-Ok "$Nome instalado com sucesso."
        return $true
    }

    Write-Erro "$Nome ainda não foi encontrado."
    Write-Erro "Feche esta janela, abra uma nova e execute o script novamente."
    return $false
}

function Test-GitHubLogado {
    & gh auth status --hostname github.com *> $null
    return ($LASTEXITCODE -eq 0)
}

function Connect-PeloNavegador {
    Write-Host ""
    Write-Host "  LOGIN PELO NAVEGADOR - o que vai acontecer:" -ForegroundColor White
    Write-Host "    1. Vai aparecer um CÓDIGO de 8 caracteres (ex.: ABCD-1234). Anote ou copie."
    Write-Host "    2. Pressione ENTER: o navegador abre na página do GitHub."
    Write-Host "    3. Faça login no GitHub (se ainda não estiver logado)."
    Write-Host "    4. Cole/digite o código e clique em 'Continue'."
    Write-Host "    5. Clique em 'Authorize github' (pode pedir sua senha ou 2FA)."
    Write-Host "    6. Quando aparecer 'Congratulations, you're all set!', volte para esta janela."
    Write-Host "    7. Se perguntarem 'Authenticate Git with your GitHub credentials?', responda Y."
    Write-Host ""
    Read-Host "  Pressione ENTER para começar" | Out-Null

    # Executado sem pipe para manter o modo interativo do gh.
    & gh auth login --hostname github.com --git-protocol https --web
}

function Connect-ComToken {
    $descricao = [uri]::EscapeDataString("Git no PC $env:COMPUTERNAME")
    $url = "https://github.com/settings/tokens/new?scopes=repo,read:org,workflow&description=$descricao"

    Write-Host ""
    Write-Host "  LOGIN COM TOKEN - como gerar o token:" -ForegroundColor White
    Write-Host "    1. Na página que vai abrir (GitHub > Settings > Developer settings > Tokens (classic)),"
    Write-Host "       as permissões 'repo', 'read:org' e 'workflow' já vêm marcadas."
    Write-Host "    2. Em 'Expiration', escolha a validade (ex.: 90 days)."
    Write-Host "    3. Clique em 'Generate token' no fim da página."
    Write-Host "    4. COPIE o token (começa com 'ghp_'). Ele só aparece UMA vez."
    Write-Host ""
    if (Read-SimNao "Abrir a página de criação de token no navegador agora?") {
        Start-Process $url
    }
    else {
        Write-Info "Endereço para criar o token: $url"
    }

    Write-Host ""
    Write-SuaVez "Cole o token abaixo (botão direito do mouse ou Ctrl+V) e pressione ENTER."
    Write-Info "Por segurança, NADA aparece na tela enquanto você cola. Isso é normal."
    $seguro = Read-Host "  Token" -AsSecureString

    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($seguro)
    try {
        $token = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }

    if ([string]::IsNullOrWhiteSpace($token)) {
        Write-Aviso "Nenhum token foi informado."
        return
    }

    $token.Trim() | & gh auth login --hostname github.com --git-protocol https --with-token
    $token = $null
    Remove-Variable token -ErrorAction SilentlyContinue
}

function Connect-GitHub {
    for ($tentativa = 1; $tentativa -le 3; $tentativa++) {
        Write-Host ""
        Write-Host "  Como você quer fazer login no GitHub?" -ForegroundColor White
        Write-Host "    [1] Pelo navegador   (RECOMENDADO - mais fácil, não expira)"
        Write-Host "    [2] Colando um token (Personal Access Token)"
        $opcao = Read-Host "  Digite 1 ou 2 e pressione ENTER"

        if ($opcao -eq '2') { Connect-ComToken }
        else                { Connect-PeloNavegador }

        if (Test-GitHubLogado) { return $true }
        Write-Aviso "O login não foi concluído (tentativa $tentativa de 3). Vamos tentar de novo."
    }
    return $false
}

function Set-GitConfigSeVazio {
    param([string]$Chave, [string]$Valor, [string]$Explicacao)
    $atual = & git config --global --get $Chave
    if ([string]::IsNullOrEmpty($atual)) {
        & git config --global $Chave $Valor
        Write-Ok "$Chave = $Valor  ($Explicacao)"
    }
    else {
        Write-Info "$Chave já estava definido como '$atual' - mantido."
    }
}

# ---------------------------------------------------------------------------
# Fluxo principal
# ---------------------------------------------------------------------------

function Invoke-Principal {

    Clear-Host
    Write-Host ""
    Write-Host "  ==============================================================" -ForegroundColor Cyan
    Write-Host "        CONFIGURAÇÃO DO GIT + GITHUB NO WINDOWS" -ForegroundColor Cyan
    Write-Host "  ==============================================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  Este script vai:"
    Write-Host "    1. Instalar o Git (se necessário)"
    Write-Host "    2. Instalar o GitHub CLI 'gh' (se necessário)"
    Write-Host "    3. Conectar este PC à sua conta do GitHub"
    Write-Host "    4. Configurar seu nome e e-mail nos commits"
    Write-Host "    5. Aplicar configurações recomendadas"
    Write-Host ""
    Write-Host "  Você vai precisar de: internet e o login (usuário/senha) do GitHub." -ForegroundColor Yellow
    Write-Host "  Ainda não tem conta? Crie em https://github.com/signup antes de continuar." -ForegroundColor Yellow
    Write-Host ""

    if (-not (Read-SimNao "Deseja continuar?")) {
        Write-Info "Execução cancelada."
        return
    }

    # ----- ETAPA 0 ---------------------------------------------------------
    Write-Etapa '0' 'Verificações iniciais'

    Write-Info ("PowerShell versão {0}" -f $PSVersionTable.PSVersion)
    Write-Info ("Windows: {0}" -f [Environment]::OSVersion.VersionString)

    try {
        $null = Invoke-WebRequest -Uri 'https://github.com' -Method Head -UseBasicParsing -TimeoutSec 15
        Write-Ok "Conexão com github.com funcionando."
    }
    catch {
        Write-Aviso "Não consegui acessar github.com. Verifique sua internet/proxy."
        if (-not (Read-SimNao "Continuar mesmo assim?" $false)) { return }
    }

    $script:TemWinget = Test-Comando 'winget'
    if ($script:TemWinget) {
        Write-Ok "winget disponível (será usado para instalar os programas)."
    }
    else {
        Write-Aviso "winget não encontrado. Se precisar instalar algo, vou abrir a página de download."
        Write-Info  "Dica: o winget vem no 'Instalador de Aplicativo' da Microsoft Store."
    }

    # ----- ETAPA 1 ---------------------------------------------------------
    Write-Etapa '1' 'Git'
    if (-not (Install-Ferramenta -Comando 'git' -WingetId 'Git.Git' -Nome 'Git' -UrlManual 'https://git-scm.com/download/win')) {
        return
    }
    Write-Info ("Versão: {0}" -f (& git --version))

    # ----- ETAPA 2 ---------------------------------------------------------
    Write-Etapa '2' 'GitHub CLI (gh)'
    if (-not (Install-Ferramenta -Comando 'gh' -WingetId 'GitHub.cli' -Nome 'GitHub CLI' -UrlManual 'https://cli.github.com/')) {
        return
    }
    Write-Info ("Versão: {0}" -f ((& gh --version) | Select-Object -First 1))

    # ----- ETAPA 3 ---------------------------------------------------------
    Write-Etapa '3' 'Login no GitHub'

    $precisaLogin = $true
    if (Test-GitHubLogado) {
        $loginAtual = & gh api user --jq .login 2>$null
        Write-Ok "Este PC já está conectado ao GitHub como: $loginAtual"
        if (Read-SimNao "Deseja manter essa conta?") {
            $precisaLogin = $false
        }
        else {
            & gh auth logout --hostname github.com
        }
    }

    if ($precisaLogin) {
        if (-not (Connect-GitHub)) {
            Write-Erro "Não foi possível concluir o login no GitHub."
            Write-Erro "Execute o script novamente quando quiser tentar de novo."
            return
        }
    }

    # Faz o Git usar o login do gh para push/pull/clone via HTTPS
    & gh auth setup-git --hostname github.com
    if ($LASTEXITCODE -eq 0) { Write-Ok "Git configurado para usar o login do GitHub (push/pull sem pedir senha)." }
    else                     { Write-Aviso "Não consegui rodar 'gh auth setup-git'. O Git pode pedir login no primeiro push." }

    $usuario = $null
    try { $usuario = ((& gh api user) -join "`n") | ConvertFrom-Json } catch { }
    if ($usuario) { Write-Ok "Conectado como: $($usuario.login)" }

    # ----- ETAPA 4 ---------------------------------------------------------
    Write-Etapa '4' 'Sua identidade nos commits'

    Write-Host "  Todo commit leva um NOME e um E-MAIL. Eles aparecem publicamente no histórico."
    Write-Host "  Recomendado: usar o e-mail 'noreply' do GitHub, que protege seu e-mail real"
    Write-Host "  e mesmo assim liga os commits ao seu perfil."
    Write-Host ""
    Write-SuaVez "Pressione ENTER para aceitar o valor entre [colchetes] ou digite outro."
    Write-Host ""

    $nomeAtual  = & git config --global --get user.name
    $emailAtual = & git config --global --get user.email

    $nomePadrao = ''
    if     ($nomeAtual)                  { $nomePadrao = $nomeAtual }
    elseif ($usuario -and $usuario.name) { $nomePadrao = $usuario.name }
    elseif ($usuario)                    { $nomePadrao = $usuario.login }

    $emailNoReply = ''
    if ($usuario) { $emailNoReply = "$($usuario.id)+$($usuario.login)@users.noreply.github.com" }

    $emailPadrao = if ($emailAtual) { $emailAtual } else { $emailNoReply }

    if ($emailNoReply) { Write-Info "Seu e-mail noreply do GitHub: $emailNoReply" }

    do {
        $nome = Read-ComPadrao "Seu nome (ex.: Maria Silva)" $nomePadrao
        if ([string]::IsNullOrWhiteSpace($nome)) { Write-Aviso "O nome não pode ficar vazio." }
    } while ([string]::IsNullOrWhiteSpace($nome))

    do {
        $email = Read-ComPadrao "Seu e-mail" $emailPadrao
        $emailValido = ($email -match '^[^@\s]+@[^@\s]+\.[^@\s]+$')
        if (-not $emailValido) { Write-Aviso "E-mail inválido. Tente novamente." }
    } while (-not $emailValido)

    & git config --global user.name  $nome
    & git config --global user.email $email
    Write-Ok "user.name  = $nome"
    Write-Ok "user.email = $email"

    if ($email -ne $emailNoReply -and $emailNoReply) {
        Write-Aviso "Você usou um e-mail próprio. Ele precisa estar cadastrado e verificado em"
        Write-Aviso "https://github.com/settings/emails para os commits aparecerem no seu perfil."
    }

    # ----- ETAPA 5 ---------------------------------------------------------
    Write-Etapa '5' 'Configurações recomendadas'
    Write-Info "Só aplico se você ainda não tiver definido algo diferente."

    Set-GitConfigSeVazio 'init.defaultBranch' 'main'  'novos repositórios começam na branch main'
    Set-GitConfigSeVazio 'core.autocrlf'      'true'  'converte quebras de linha corretamente no Windows'
    Set-GitConfigSeVazio 'pull.rebase'        'false' 'git pull faz merge (comportamento padrão)'

    if (Test-Comando 'code') { Set-GitConfigSeVazio 'core.editor' 'code --wait' 'mensagens de commit abrem no VS Code' }
    else                     { Set-GitConfigSeVazio 'core.editor' 'notepad'     'mensagens de commit abrem no Bloco de Notas' }

    # ----- ETAPA 6 ---------------------------------------------------------
    Write-Etapa '6' 'Verificação final'

    Write-Info ("Git:        {0}" -f (& git --version))
    Write-Info ("GitHub CLI: {0}" -f ((& gh --version) | Select-Object -First 1))
    Write-Host ""
    & gh auth status --hostname github.com
    Write-Host ""

    Write-Info "Testando acesso aos seus repositórios..."
    & gh repo list --limit 5
    if ($LASTEXITCODE -eq 0) { Write-Ok "Acesso ao GitHub funcionando (se você ainda não tem repositórios, a lista fica vazia)." }
    else                     { Write-Aviso "Não consegui listar os repositórios. Veja a mensagem acima." }

    Write-Host ""
    Write-Host "  ==============================================================" -ForegroundColor Green
    Write-Host "    TUDO PRONTO! Git e GitHub configurados neste PC." -ForegroundColor Green
    Write-Host "  ==============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  Comandos úteis para começar:" -ForegroundColor White
    Write-Host "    git clone https://github.com/USUARIO/REPO.git   -> baixa um repositório"
    Write-Host "    gh repo create meu-projeto --private --clone    -> cria um repo novo no GitHub"
    Write-Host "    git status                                      -> mostra o que mudou"
    Write-Host "    git add .                                       -> prepara as mudanças"
    Write-Host "    git commit -m 'mensagem'                        -> salva um commit"
    Write-Host "    git push                                        -> envia para o GitHub"
    Write-Host "    git pull                                        -> baixa as novidades"
    Write-Host ""
    Write-Info "Abra um NOVO terminal/editor para que ele enxergue o git e o gh recém-instalados."
}

# ---------------------------------------------------------------------------
# Execução
# ---------------------------------------------------------------------------

try { Start-Transcript -Path $script:LogPath -ErrorAction SilentlyContinue | Out-Null } catch { }

try {
    Invoke-Principal
}
catch {
    Write-Erro "Erro inesperado: $($_.Exception.Message)"
}
finally {
    try { Stop-Transcript | Out-Null } catch { }
    Write-Host ""
    Write-Info "Log desta execução salvo em: $script:LogPath"
    Read-Host "  Pressione ENTER para fechar" | Out-Null
}
