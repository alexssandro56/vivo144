# Vivo 144Hz 3.0 (iQOO 15T / OriginOS)

Coloca a tela em 144 Hz nativos sem root e sem interpolação. O app vota direto no serviço `rms` da vivo, com a mesma técnica do [vivo HiHz](https://github.com/AndroPlus-org/vivo-HighRefresh-NoRoot) (MIT). O Shizuku só é usado quando o sistema esconde esse serviço do app.

## Modos

| Modo | O que faz |
|---|---|
| Desligado | A vivo decide a taxa (LTPO de 1 a 120 Hz). |
| Sempre | Taxa alvo fixa em tudo até desligar. Volta sozinha após reiniciar, se a opção estiver ligada. |
| Automático | Liga a taxa alvo quando um dos jogos escolhidos abre e volta ao LTPO quando você sai. Detecta o jogo pelo acesso ao uso, com uma leitura por segundo e só com a tela ligada. |

O bloco **144 Hz** das Configurações Rápidas alterna entre Desligado, Sempre e Automático.

## Outras funções

- **Jogos:** na primeira abertura, o app já marca os jogos instalados, pela categoria do Android e por uma lista de jogos conhecidos.
- **Ajustes:** com o Shizuku, um toque libera acesso ao uso, sobreposição, notificações, bateria sem restrição e execução em segundo plano.
- **Debloat CN:** remove os apps da lista de uma vez, com restauração. Recusa sempre os apps essenciais e o teclado em uso.
- **Indicador flutuante:** mostra a taxa real por cima dos jogos.

## Build

A cada push, o GitHub Actions compila o APK de release, assinado com `keystore/vivo144.jks`, e publica em **Releases**. Como a chave é fixa, cada build novo instala por cima do anterior.
