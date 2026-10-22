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

## Extras: brilho e botão Power (v3.1)

Um serviço de acessibilidade (`UtilityAccessibilityService`) cuida de duas coisas:

- **Botão Power:** quando o assistente da vivo abre com o Power segurado por cerca de 0,5 s, o serviço fecha o assistente (com BACK, HOME ou nenhuma ação, configurável) e abre o menu de energia nativo. Ele nunca intercepta teclas, então o print com Power + Volume − continua funcionando. O **modo descoberta**, que dura 30 s, mostra qual pacote abriu e deixa adicioná-lo à lista vigiada.
- **Brilho:** uma janela de overlay de 1×1 px com `screenBrightness` e as chaves padrão `screen_brightness_mode`, `screen_brightness` (0–255) e `screen_brightness_float`. O app guarda os valores originais antes de mudar e os restaura depois.
- **Laboratório:** testa apenas as 8 chaves de brilho da vivo listadas na especificação. Cada teste faz backup, lê o valor de volta, reverte sozinho e registra as mudanças.
- **Blocos nas Configurações Rápidas:** 144 Hz, Brilho Turbo e Botão Power.

O botão "Liberar tudo com 1 toque" do Shizuku concede `WRITE_SETTINGS` e `WRITE_SECURE_SETTINGS` e ativa o serviço de acessibilidade. O comando manual equivalente é:

    pm grant com.vivo144.app android.permission.WRITE_SECURE_SETTINGS

## Créditos

A técnica de voto no serviço `rms` vem de [AndroPlus-org/vivo-HighRefresh-NoRoot](https://github.com/AndroPlus-org/vivo-HighRefresh-NoRoot), sob licença MIT, Copyright (c) AndroPlus.
