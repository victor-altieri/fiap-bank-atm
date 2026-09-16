# Roteiro de conferência visual e funcional

Use um banco novo de demonstração, sem remover um banco que alguém esteja utilizando.
O parâmetro `-Datm.database=data/validacao.db` permite separar o teste.

1. Iniciar a aplicação e comparar a janela com o emulador original: cores, tamanho, teclado, botões e LEDs.
2. Entrar na conta 12345 com PIN 1234. Conferir saldo inicial de R$ 5.000,00.
3. Sacar R$ 20,00 pelo botão lateral e aguardar a animação. Saldo: R$ 4.980,00.
4. Depositar R$ 100,00 pelo teclado e confirmar. Saldo: R$ 5.080,00.
5. Transferir R$ 50,00 para 67890. Saldo de origem: R$ 5.030,00; destino: R$ 1.250,00.
6. Solicitar extrato, aguardar a impressão e verificar as cinco últimas transações e a formatação.
7. Fechar e abrir usando o mesmo arquivo de banco; confirmar saldo e extrato mantidos.
8. Em 99999, errar o PIN três vezes. Fechar e abrir; PIN correto 9999 deve continuar bloqueado.
9. Em banco separado, sacar até o limite de 12345; reiniciar e conferir que o limite não foi restabelecido.
10. Conferir navegação por Enter, C, Escape, teclado e botões laterais, inclusive mensagens de erro.

O fluxo original de transferência foi preservado, incluindo a permanência na tela de sucesso até a
navegação já disponível no emulador. Não foi acrescentado um temporizador novo nessa operação.
