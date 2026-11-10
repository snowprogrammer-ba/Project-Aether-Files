# Aether Files

Explorador de arquivos Android feito em Kotlin e Jetpack Compose.

## Funcionalidades v0.1
- Escolher diretório via SAF (ACTION_OPEN_DOCUMENT_TREE) e manter permissão após reiniciar
- Navegar, pesquisar na pasta, abrir arquivos
- Criar pasta/arquivo, renomear, excluir, copiar e mover entre pastas do diretório escolhido
- Interface Material 3 escura

## Gerar APK com GitHub Actions (sem PC)
1. Crie um repositório no GitHub e envie **o conteúdo desta pasta** (inclusive `.github/workflows/android.yml`).
2. Abra **Actions > Build Android APK > Run workflow** (ou faça push para `main`).
3. Aguarde o job terminar; em **Artifacts**, baixe `AetherFiles-debug-apk`.
4. Extraia o ZIP de artifact e instale `app-debug.apk` no Android (permita instalação desta origem, se solicitado).

## Compilar localmente
Abra a pasta no Android Studio (JDK 17, Android SDK 35), sincronize Gradle e execute `:app:assembleDebug`. Se usar Gradle no terminal, instale Gradle 8.9 e rode `gradle :app:assembleDebug`.

## Permissões e limitações
O app usa Storage Access Framework: o usuário escolhe uma pasta e o Android concede acesso a ela e seus descendentes. Não solicita `MANAGE_EXTERNAL_STORAGE` ou permissões legadas. O seletor pode restringir raízes e `Android/data`/`Android/obb`, especialmente no Android 11+. Não acessa pastas fora da árvore autorizada. A versão inicial não implementa ZIP, favoritos, thumbnails ou FTP.

## Avisos
- Build não executado neste ambiente: o workflow é a primeira verificação de compilação.
- Operação de mover usa copiar + apagar. Se a cópia funcionar e a exclusão falhar, ambos os arquivos permanecerão.
- Cópias muito grandes e operações em muitos arquivos ainda não têm indicador detalhado de progresso.
- Não mova uma pasta para dentro de um de seus descendentes; a interface impede a origem dentro da rota atual, mas o provedor de documentos pode ter comportamentos específicos.
