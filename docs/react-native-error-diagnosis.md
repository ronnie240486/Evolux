# Diagnóstico do erro React Native

O stack trace recebido contém `com.facebook.react`, `ReactInstance`, `JavascriptException`, Metro e DevTools websocket. O APK Evolux verificado com `aapt dump badging` possui o pacote `com.evolux.tv`, label Evolux, target SDK 34 e dependências Compose/Kotlin no APK. A listagem do APK não contém React Native, Metro, Hermes ou JavaScriptCore.

Conclusão: esse stack trace não é produzido pelo APK nativo Evolux entregue nesta sessão. Ele pertence a outro aplicativo, a uma versão diferente ou a um ambiente React Native incorporado. O pacote correto a testar é `com.evolux.tv`.
