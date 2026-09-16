# FIAP Bank ATM — Checkpoint 4

Refatoração do emulador Java Swing em quatro módulos Maven, com persistência JDBC/SQLite.
Base: fork de https://github.com/prof-eduardo-ramos/fiap-bank-atm.

## Equipe

Preencher os dados reais antes da entrega:

| Parte | Nome completo | RM |
|---|---|---|
| 1 — Estrutura e domínio | PREENCHER | PREENCHER |
| 2 — Contratos e DTOs | PREENCHER | PREENCHER |
| 3 — JDBC e SQLite | PREENCHER | PREENCHER |
| 4 — Serviços de aplicação | PREENCHER | PREENCHER |
| 5 — Swing, testes e documentação | PREENCHER | PREENCHER |

## Requisitos e execução

- JDK 21 ou superior, com `java` e `javac` disponíveis.
- Maven 3.9 ou superior no PATH. A primeira compilação precisa acessar o Maven Central.
- No Windows, `run.bat` também procura o Maven no caminho padrão do Apache NetBeans.

Na raiz:

```powershell
mvn clean verify
.\run.bat
```

O script compila, executa os testes e inicia o Swing. Para executar após a compilação:

```powershell
java -cp "presentation/target/presentation-1.0-SNAPSHOT-runtime.jar;infrastructure/target/infrastructure-1.0-SNAPSHOT-runtime.jar" com.fiap.bank.atm.AtmApplication
```

No Linux/macOS, substitua o `;` do classpath por `:`.
Execute a partir da raiz para manter o banco no mesmo local. O caminho padrão é `data/fiap-bank.db`.
Para outro caminho, acrescente `-Datm.database=C:/caminho/banco.db` antes de `-cp`.
Na IDE, importe o POM raiz; use o comando acima ou inclua o módulo infrastructure no classpath
da configuração de execução. Não adicione dependências proibidas ao POM de presentation.

## Arquitetura

```text
presentation -> application -> domain
                           -> infrastructure -> domain
```

- `domain`: entidades, Money, regras de negócio e contratos de repositório.
- `application`: records, exceções de aplicação, sessão e coordenação das operações.
- `infrastructure`: SQLite, conexões, scripts de inicialização e repositório JDBC.
- `presentation`: telas e formulário originais, formatação de moeda e ponto de entrada.

O POM raiz tem packaging `pom`. Entre os módulos do projeto, presentation depende somente de application.
FlatLaf é a biblioteca visual original. JUnit tem escopo de teste.
As dependências de application para domain e infrastructure são `optional`: não são propagadas
ao classpath de compilação de presentation. `ArchitectureTest` comprova essa restrição com o compilador Java.

Para execução, o Maven gera dois JARs adicionais com sufixo `runtime`: presentation reúne a interface,
application e FlatLaf; infrastructure reúne JDBC, SQLite e domain. Os JARs normais continuam separados,
sem empacotar as dependências. Os JARs runtime não são usados para compilar os módulos.

O construtor público de AtmService recebe somente um Path. Ele monta a infraestrutura internamente;
nenhum método público recebe ou devolve Account, Money ou classes JDBC. Os dados expostos são records,
BigDecimal, String, Boolean e coleções de DTOs. As exceções de domínio são traduzidas na aplicação.

## Contas de demonstração

| Origem | Agência | Número | Digitar no teclado | PIN | Saldo inicial | Limite de saque |
|---|---|---|---|---|---|---|
| Emulador original | 0001 | 12345 | 12345 | 1234 | R$ 5.000,00 | R$ 1.500,00 |
| Emulador original | 0001 | 67890 | 67890 | 5678 | R$ 1.200,00 | R$ 1.000,00 |
| Emulador original | 0001 | 99999 | 99999 | 9999 | R$ 50,00 | R$ 500,00 |
| Anexo CP4 | 0001 | 12345-6 | 123456 | 1234 | R$ 1.500,00 | R$ 1.500,00 |
| Anexo CP4 | 0001 | 98765-4 | 987654 | 5678 | R$ 250,50 | R$ 1.000,00 |
| Anexo CP4, bloqueada | 0002 | 11111-1 | 111111 | 9999 | R$ 0,00 | R$ 500,00 |

A busca aceita o número com ou sem hífen porque o teclado original aceita apenas dígitos.
Os PINs e limites das contas do anexo foram definidos para permitir o teste; esses campos não constam no anexo.
Os saldos e extratos históricos das três contas originais foram preservados como dados demonstrativos.
São saldos de abertura acompanhados de histórico parcial, não uma reconstrução de saldo a partir do extrato.

## Persistência e regras

- `ATMRepository<T extends BaseEntity>` e `AccountRepository extends ATMRepository<Account>`.
- Toda busca pública do repositório retorna `Optional`, com tratamento explícito da ausência.
- Somente JDBC: PreparedStatement, parâmetros `set...`, ResultSet e try-with-resources; nenhum ORM.
- O schema mantém os campos do anexo e acrescenta PIN, limite, tentativas, versão e descrição para
  preservar o comportamento que já existia no emulador. O PIN em texto simples é dado fictício do protótipo.
- Inicialização idempotente: contas e transações de exemplo são inseridas uma vez e não redefinem saldos.
- Conta e histórico são lidos na mesma transação. Saldo e novos lançamentos são gravados juntos.
- Transferências usam uma única transação SQL para as duas contas, com rollback em caso de falha.
- Uma versão por conta evita que uma sessão sobrescreva alterações de outra; o usuário pode repetir a operação.
- Tentativas incorretas e bloqueio persistem; uma autenticação bem-sucedida grava o reset das tentativas.
- O total diário usa Streams sobre os saques da data local. Reiniciar não libera o limite; a troca de dia libera.
- A aplicação recarrega a conta antes de cada operação, sem manter um saldo mutável na sessão.
- Extrato retorna DTOs imutáveis em ordem decrescente. O comprovante mantém os cinco lançamentos mais recentes.

## Interface preservada

AtmFrame.form e ScreenState.java foram mantidos sem alterações. Em AtmFrame.java, os ajustes foram
limitados a imports, DTOs, passagem de BigDecimal, formatação e leitura do extrato com Streams.
Componentes, estilos, textos visuais, botões, estados e temporizadores foram preservados.
O diagrama do CP2 não foi fornecido: a modelagem parte das entidades presentes no clone e dos requisitos do CP4.

## Validação

`mvn clean verify` executa os testes de domínio, persistência em arquivos SQLite temporários,
serviços e isolamento de compilação. O conjunto verifica, entre outros cenários, reinicialização,
bloqueio, limite, transferência, rollback após falha SQL, escrita concorrente e entrada de SQL injection.
Os testes não usam o banco de demonstração de `data/`.

Faça também a conferência visual descrita em `docs/VALIDACAO-MANUAL.md`.

## Entrega

1. Trabalhar no fork compartilhado e integrar as cinco partes na ordem do guia.
2. Cada integrante deve revisar, testar e compreender sua parte e fazer o commit com sua própria conta Git.
3. Preencher nomes/RMs e manter o código-fonte visível no GitHub.
4. Compactar o projeto em `RMXXXXX_Nome_Sobrenome_CP4.zip`, sem .git, .idea, target ou bancos gerados.
5. O líder entrega ZIP e link do GitHub na tarefa correspondente do Teams.
