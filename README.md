# Vivo 144Hz 2.0 (iQOO 15T / OriginOS)

App sem root que fixa a tela em 144 Hz nativos votando direto no serviço `rms` da vivo. É um porte do `RmsVote.java` (AndroPlus-org/vivo-HighRefresh-NoRoot, MIT). Não usa `app_process`, `.dex`, download nem interpolação (MEMC).

## O que tem

| Função | Como funciona | Precisa de Shizuku? |
|---|---|---|
| Ligar/desligar 144 Hz | Voto direto no binder `rms` (tx 31 / 24, sem interface token). Primeiro tenta sem Shizuku e, se o SELinux bloquear, usa o Shizuku. | Só se o modo direto falhar |
| Ler taxa real | tx 26 (`readException` + `readFloat`), uma chamada de binder | Igual acima |
| Reason (quem manda no painel) | `settings get secure vivo_rms_active_request_reason` | Sim (no Android 12+) |
| Taxas suportadas | `Display.getSupportedModes()` | Não |
| Bloco nas Configurações Rápidas | TileService "144 Hz" | — |
| Reaplicar após reiniciar | BOOT_COMPLETED (o voto some no reboot) | No modo Shizuku, só se o Shizuku já estiver rodando |
| Monitor flutuante | Overlay que mostra a taxa e o reason; toque nele para votar de novo | Igual acima |
| Debloat CN | `pm uninstall -k --user 0` e, se der USER_RESTRICTED, `pm disable-user`. Restaura com `install-existing` + `enable`. | Sim |

O debloat sempre recusa: gamewatch, pem, sps, abe, rms, System UI, launcher, Configurações, o teclado que está em uso e o launcher padrão atual.

## Como compilar sem Android Studio

1. Crie um repositório no GitHub e suba esta pasta.
2. Em **Actions**, o workflow `Build APK` roda sozinho. Ao terminar, baixe o artefato `vivo144-apk`.
3. Instale o `app-debug.apk` no celular. Se a versão 1.0 foi assinada com outra chave, desinstale ela antes.

Com o Android Studio, basta abrir a pasta e rodar `assembleDebug`.

## Uso

1. Abra o app e toque em **LIGAR**. Se aparecer "Modo: direto (sem Shizuku)", o Shizuku não é necessário.
2. Se pedir o Shizuku, inicie-o (depuração sem fio) e toque em **Permitir Shizuku**.
3. Adicione o bloco **144 Hz** nas Configurações Rápidas.
4. Para o reaplicar no boot funcionar, permita **Início automático** do app no i Manager.

Se o reason mostrar `reqFps:<activity>|120`, o jogo está pedindo a própria taxa e o pedido dele vence qualquer voto. Escolha a taxa máxima dentro do jogo ou no Assistente de Jogo.

## Fora do app, de propósito

- **Ajustes de toque** (`MultitouchSettleInterval`, `QuietInterval`, `PressureForID`…). São parâmetros de arquivos `.idc` do InputReader, não chaves de `settings`. Gravar com `settings put system` não muda nada.
- **`game_mode_*`, `NV_FPSLIMIT`, `game_ub_turbo_mode`**. Não há evidência de que o OriginOS leia essas chaves (`NV_FPSLIMIT` vem dos aparelhos NVIDIA).
- **`gamecube_frame_interpolation_for_sr`**. O voto no `rms` já dá 144 Hz nativos sem MEMC. Se você ativou interpolação antes, desative-a no Assistente de Jogo.
- **`service call package 131…`**. O número da transação muda entre versões, e um número errado chama outro método do PackageManager. Além disso, `pm disable-user` faz a mesma coisa com as mesmas permissões.
- **Spoof de `com.ludashi.benchmark`**. Com esse nome, o app conflita com o LuDaShi real e força o perfil térmico máximo o tempo todo.
