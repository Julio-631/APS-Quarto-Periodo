# Passo a passo: configurar Git + GitHub no Windows

Este guia acompanha o script `Configurar-Git-GitHub.ps1`. Ele explica o que o script faz em cada etapa e **o que você precisa fazer** em cada momento.

Tempo estimado: 5 a 10 minutos.

---

## Antes de começar

Você vai precisar de:

- Windows 10 ou 11
- Internet
- Uma conta no GitHub. Se não tiver, crie em https://github.com/signup **antes** de rodar o script
- Saber seu usuário/senha do GitHub (e o celular, se você usa verificação em duas etapas)

---

## Como executar o script

1. Salve o arquivo `Configurar-Git-GitHub.ps1` em uma pasta (ex.: `Downloads`).
2. Clique com o **botão direito** no arquivo e escolha **"Executar com o PowerShell"**.

Se isso não funcionar (a janela abre e fecha, ou aparece erro de "execução de scripts desabilitada"):

1. Abra o menu Iniciar, digite **PowerShell** e abra o **Windows PowerShell**.
2. Vá até a pasta do arquivo. Exemplo para a pasta Downloads:
   ```powershell
   cd $env:USERPROFILE\Downloads
   ```
3. Rode:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\Configurar-Git-GitHub.ps1
   ```

Não precisa abrir como administrador. Quando for necessário, o próprio Windows vai pedir permissão.

---

## Legenda das mensagens

| Mensagem | Significado |
|---|---|
| `[OK]` verde | Deu certo |
| `[..]` cinza | Informação, não precisa fazer nada |
| `[!!]` amarelo | Aviso, leia com atenção |
| `[XX]` vermelho | Erro |
| `>>> SUA VEZ:` roxo | **Você precisa fazer algo** |

Quando aparecer `[S/n]`, basta apertar ENTER para "Sim" ou digitar `n` para "Não". A letra maiúscula é a resposta padrão.

---

## Etapa por etapa

### Tela inicial

**O script:** mostra o resumo do que vai fazer.
**Você:** pressione **ENTER** para continuar.

### Etapa 0: Verificações iniciais

**O script:** confere a versão do Windows/PowerShell, testa a conexão com o GitHub e verifica se o `winget` (instalador de programas do Windows) existe.
**Você:** nada. Só se aparecer aviso de internet, confira sua conexão.

### Etapa 1: Git

**O script:** se o Git não estiver instalado, instala automaticamente.
**Você:**
- Se aparecer a janela azul do Windows **"Deseja permitir que este aplicativo faça alterações?"**, clique em **Sim**.
- Aguarde. Pode levar alguns minutos.
- Se a instalação automática falhar, o script abre o site https://git-scm.com/download/win. Baixe, instale clicando **Next** em todas as telas (as opções padrão estão boas), volte para o script e aperte **ENTER**.

### Etapa 2: GitHub CLI (gh)

**O script:** igual à etapa anterior, mas para o `gh`, a ferramenta oficial do GitHub que faz o login.
**Você:** mesma coisa. Clique em **Sim** se o Windows pedir permissão. Se falhar, o script abre https://cli.github.com/ para instalação manual.

### Etapa 3: Login no GitHub (a parte mais importante)

Se o PC já estiver conectado a uma conta, o script mostra qual é e pergunta se quer manter.

Caso contrário, ele pergunta como você quer entrar:

```
[1] Pelo navegador   (RECOMENDADO)
[2] Colando um token
```

#### Opção 1: pelo navegador (recomendada)

1. Digite **1** e aperte **ENTER**.
2. O script mostra as instruções. Aperte **ENTER** de novo.
3. Vai aparecer algo assim:
   ```
   ! First copy your one-time code: ABCD-1234
   Press Enter to open https://github.com/login/device in your browser...
   ```
   **Anote o código** (ex.: `ABCD-1234`). Para copiar, selecione com o mouse e clique com o botão direito.
4. Aperte **ENTER**. O navegador abre.
5. Faça login no GitHub, se pedir.
6. Digite o código na página e clique em **Continue**.
7. Clique em **Authorize github**. Pode pedir sua senha ou o código do celular (2FA).
8. Quando aparecer **"Congratulations, you're all set!"**, volte para a janela do script.
9. Se o terminal perguntar `Authenticate Git with your GitHub credentials? (Y/n)`, digite **Y** e aperte **ENTER**.

Pronto. Deve aparecer `Logged in as SEU-USUARIO`.

#### Opção 2: com token (use se o navegador não funcionar)

1. Digite **2** e aperte **ENTER**.
2. Responda **S** para abrir a página de criação do token.
3. No navegador (página **New personal access token (classic)**):
   - **Note:** já vem preenchido, pode deixar.
   - **Expiration:** escolha a validade, ex.: **90 days**. Quando vencer, rode o script de novo.
   - **Scopes:** `repo`, `read:org` e `workflow` já vêm marcados. Não precisa mudar nada.
   - Role até o fim e clique em **Generate token**.
4. **Copie o token** (começa com `ghp_`). Ele só aparece uma vez.
5. Volte ao script, **cole com o botão direito do mouse ou Ctrl+V** e aperte **ENTER**.
   > **Nada aparece na tela enquanto você cola. Isso é normal**, é uma proteção. Cole uma única vez e aperte ENTER.

Se o login falhar, o script deixa tentar até 3 vezes.

> **Segurança:** o token funciona como uma senha. Não mande por WhatsApp/e-mail, não tire print e não coloque em arquivos.

### Etapa 4: Sua identidade nos commits

**O script:** pergunta o nome e o e-mail que vão aparecer em cada commit. Ele já sugere valores entre `[colchetes]`.
**Você:**
- **Nome:** aperte ENTER para aceitar o sugerido ou digite seu nome (ex.: `Maria Silva`).
- **E-mail:** recomendo aceitar o sugerido, que é o e-mail **noreply** do GitHub (ex.: `12345678+usuario@users.noreply.github.com`). Ele esconde seu e-mail real e mesmo assim liga os commits ao seu perfil.
  - Se preferir usar seu e-mail real, ele precisa estar cadastrado e verificado em https://github.com/settings/emails.

### Etapa 5: Configurações recomendadas

**O script:** aplica alguns ajustes padrão. Se você já tinha algo configurado, ele mantém o seu.

| Configuração | Para que serve |
|---|---|
| `init.defaultBranch = main` | Novos repositórios começam na branch `main` |
| `core.autocrlf = true` | Evita problemas de quebra de linha no Windows |
| `pull.rebase = false` | `git pull` usa o comportamento padrão (merge) |
| `core.editor` | Editor das mensagens de commit: VS Code, se existir; senão, Bloco de Notas |

**Você:** nada.

### Etapa 6: Verificação final

**O script:** mostra as versões instaladas, a conta conectada e lista até 5 repositórios seus para confirmar o acesso. Se você ainda não tem repositórios, a lista vem vazia, e está tudo bem.
**Você:** confira se aparece **"TUDO PRONTO!"** em verde e aperte **ENTER** para fechar.

> Depois disso, **feche e abra de novo** qualquer terminal ou editor (VS Code, Kiro etc.) que já estava aberto, para ele reconhecer o `git` e o `gh`.

---

## Problemas comuns

| Problema | Solução |
|---|---|
| A janela abre e fecha na hora | Rode pelo PowerShell com o comando `-ExecutionPolicy Bypass` (veja "Como executar") |
| "execução de scripts foi desabilitada" | Mesma solução acima |
| winget não encontrado | O script abre o site para instalar manualmente. Ou instale o "Instalador de Aplicativo" na Microsoft Store |
| Git/gh "não encontrado" logo após instalar | Feche a janela, abra uma nova e rode o script de novo. O que já foi feito é mantido |
| O código do navegador expirou | Escolha a opção 1 de novo. O código vale por cerca de 15 minutos |
| Colei o token e deu erro | Confira se copiou o token inteiro (começa com `ghp_`) e se os scopes `repo` e `read:org` estavam marcados |
| Rede da empresa/faculdade bloqueia | Tente em outra rede ou fale com o suporte de TI (pode haver proxy) |

Um log de cada execução fica salvo em `%TEMP%`. O caminho aparece no fim do script. Se der problema, mande esse arquivo para quem estiver te ajudando. Ele **não** contém o token.

---

## Depois de configurado: primeiros comandos

```powershell
# Criar um repositório novo no GitHub e já baixar no PC
gh repo create meu-projeto --private --clone

# Ou baixar um repositório que já existe
git clone https://github.com/USUARIO/REPOSITORIO.git

# Fluxo do dia a dia, dentro da pasta do projeto
git status                    # ver o que mudou
git add .                     # preparar as mudanças
git commit -m "mensagem"      # salvar um commit
git push                      # enviar para o GitHub
git pull                      # baixar novidades do GitHub
```
