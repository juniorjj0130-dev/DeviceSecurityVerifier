# DeviceSecurityVerifier (Verificador de Segurança)

Este projeto é uma ferramenta de demonstração desenvolvida para fins educacionais e de pesquisa em ambientes de laboratório de segurança cibernética. Ele explora as capacidades e permissões dos **Serviços de Acessibilidade** no Android para automação e análise de interface.

⚠️ **AVISO IMPORTANTE**: Este software destina-se exclusivamente a uso ético e educacional em ambientes controlados. O uso indevido de ferramentas de acessibilidade pode violar as políticas de privacidade e segurança do Google Play e leis locais.

## 🚀 Funcionalidades Implementadas

### 1. Monitoramento e Análise
*   **Monitoramento de Apps**: Identifica o aplicativo em primeiro plano em tempo real (`foreground_app`).
*   **Inspeção de UI (Árvore de Elementos)**: Extrai a hierarquia completa da tela (`get_ui_tree`), incluindo textos, IDs, classes e coordenadas (`bounds`).
*   **Conectividade C2**: Comunicação persistente via WebSocket com reconexão automática.

### 2. Automação de Interface (Accessibility Service)
*   **Cliques Inteligentes**: Simula cliques por Texto ou ID de recurso. Se o alvo não for clicável, busca automaticamente o container pai que possua a ação.
*   **Escrita de Texto**: Preenche campos de formulário e inputs (`write_text`) de forma remota.
*   **Gestos Avançados**: Executa `swipes` entre coordenadas personalizadas e controle de `scroll` (rolagem) da tela.
*   **Ações Globais**: Executa comandos do sistema como "Voltar", "Home", "Recentes" e "Abrir Notificações".
*   **Auto-Concessão**: Lógica integrada para aceitar permissões de sistema e diálogos comuns automaticamente.

### 3. Módulos de Sobreposição (Overlays)
*   **Guia de Ativação**: Banner instrucional para guiar o usuário nas configurações.
*   **Bloqueio de Tela**: Overlay em tela cheia para simular manutenção ou atualizações.
*   **Captura de Credenciais**: Overlay de Phishing para demonstração de captura de e-mail e senha com envio imediato ao C2.

### 4. Persistência
*   **Auto-Início**: Inicia o serviço automaticamente após o reboot do dispositivo (`RECEIVE_BOOT_COMPLETED`).
*   **Direct Boot**: Capaz de rodar antes mesmo do primeiro desbloqueio de tela.
*   **Otimização de Bateria**: Solicita exclusão das regras de economia de energia para evitar o encerramento do WebSocket pelo sistema.

## 🛠️ Tecnologias Utilizadas
*   **Linguagem**: Kotlin (Sem Jetpack Compose para máxima compatibilidade com Android Go).
*   **Comunicação**: OkHttp (WebSockets).
*   **Serialização**: Gson.
*   **Interface**: Android XML (Material Design).
*   **Versões**: Min SDK 26 | Target SDK 35.

## ⚙️ Configuração do Ambiente
1. Configure o IP do seu servidor no arquivo `LabAccessibilityService.kt`:
   `private const val C2_URL = "ws://SEU_IP:8765"`
2. Compile e instale o APK no dispositivo de teste.
3. Conceda as permissões de **Sobreposição** e **Acessibilidade**.
4. No diálogo de bateria, selecione **"Não Otimizar"**.

## 🛡️ Segurança e Privacidade
O aplicativo inclui avisos visuais explícitos ("MODO DEMONSTRAÇÃO") para garantir a transparência e o uso restrito a ambientes de pesquisa.
