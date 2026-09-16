# Resultado da validação

## Concluído

- Fontes dos quatro módulos compilados com Java 21, respeitando dependências separadas.
- 19 testes JUnit executados com sucesso: 4 de domínio, 6 de JDBC, 8 de aplicação e 1 de isolamento.
- Verificação adicional de compilação: presentation aceita AccountInfoDTO e rejeita Account e AccountRepositoryJdbcImpl.
- Maven gerou os JARs normais e os dois JARs runtime dos módulos correspondentes.
- Duas execuções independentes usando somente os JARs runtime confirmaram persistência de saldo e extrato.
- Scripts PowerShell das cinco partes aplicados na ordem em um repositório temporário.
- Os arquivos resultantes das cinco partes são iguais aos do projeto final.
- Formulário Swing e ScreenState mantidos byte a byte; componentes gerados, transições de estado,
  tempos das animações e textos estáticos conferidos contra o clone original.

## Condições da execução

O sandbox Windows bloqueou a resolução de caminhos usada pelo compilador padrão ao abrir/fechar JARs
e o arquivo ct.sym. Por isso, `mvn clean verify` não foi concluído integralmente neste ambiente.

A validação foi dividida: compilação com a API JavaCompiler do JDK 21 e um gerenciador temporário de
leitura dos arquivos de dependências; execução dos testes pelo JUnit Platform; empacotamento Maven
aproveitando as classes já compiladas, com as fases de compilação e testes desabilitadas nessa chamada.
Esse ajuste ficou exclusivamente nas ferramentas temporárias de validação. O projeto entregue mantém
o build Maven convencional com release 21 e não depende dessas ferramentas.

No computador de cada integrante, executar os comandos Maven convencionais indicados no guia.
O teste de interação visual manual não foi executado nesta sessão; seguir VALIDACAO-MANUAL.md.

## Informações a completar pela equipe

- Preencher nomes completos e RMs no README.
- Conferir o diagrama do CP2, que não acompanhou os arquivos fornecidos.
- Cada integrante revisa sua etapa e registra seu próprio commit. Nenhum commit foi criado no clone original.
