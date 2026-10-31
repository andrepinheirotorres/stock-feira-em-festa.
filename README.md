# StockFácil Android (offline)

App Android em Java, com SQLite local, leitura de códigos de barras pela câmara, entradas com quantidade configurável e saídas de uma unidade por leitura. Sem servidor e sem Internet durante a utilização.

## Gerar APK com GitHub Actions
1. Crie um repositório GitHub e envie **o conteúdo desta pasta**, não a pasta como subdiretório.
2. Abra Actions > Criar APK StockFacil > Run workflow.
3. Depois da compilação, abra a execução > Artifacts > StockFacil-APK.
4. Extraia o ZIP e instale o `app-debug.apk` no Android.

O APK de debug é destinado a testes. Os dados SQLite ficam guardados no dispositivo; desinstalar a aplicação pode apagá-los. Não existe sincronização nem cópia de segurança nesta versão.

Se a compilação falhar, consulte a etapa vermelha em Actions e copie a mensagem de erro. É necessário Android SDK 35 no runner.
