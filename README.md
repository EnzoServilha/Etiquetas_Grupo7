# etiquetas

Microserviço **consumer** de etiquetas. Ele lê as solicitações da fila do RabbitMQ, gera o PDF da etiqueta
(JasperReports) e o envia como anexo para o e-mail do Marcos.

Este serviço **não publica** mensagens: quem publica é o `BackEnd_Grupo7` (`POST /etiquetas/{itemId}/fila`), que
consulta o item no próprio banco e envia na mensagem os dados necessários. O consumer não consulta o banco do backend.

```text
Frontend -> BackEnd_Grupo7 -> banco próprio
BackEnd_Grupo7 (producer) -> RabbitMQ -> etiquetas (consumer) -> PDF -> e-mail
```

## RabbitMQ

| Item | Nome |
|------|------|
| Exchange (direct, durável) | `exchange_etiqueta` |
| Routing key | `routing_key_etiqueta` |
| Fila principal (durável) | `queue_etiqueta` |
| Exchange da DLQ | `exchange_etiqueta_dlx` |
| Routing key da DLQ | `routing_key_etiqueta_dlq` |
| DLQ (durável) | `queue_etiqueta_dlq` |

- O consumer e o producer declaram a **mesma** exchange, fila e binding principais. A fila `queue_etiqueta` é
  declarada sem argumentos, como sempre foi: o RabbitMQ não altera os argumentos de uma fila existente e recusa a
  declaração com argumentos diferentes (`PRECONDITION_FAILED`). Por isso a DLQ não depende de argumentos da fila.
- A DLQ é declarada e alimentada somente por este serviço.
- Ack automático após o processamento com sucesso; `prefetch=1`; `default-requeue-rejected=false`.

## Processamento de cada mensagem

1. **Validação**: `solicitacaoId` (UUID), `itemId` > 0 e `codigoInterno` preenchido. JSON malformado ou dados
   inválidos não são retentados e vão direto para a DLQ.
2. **Idempotência**: cada `solicitacaoId` é registrado (tabela `solicitacao_etiqueta`). Se já estiver `CONCLUIDA`, a
   reentrega é ignorada (nenhum novo e-mail). Se estiver `PROCESSANDO` ou `ERRO`, é processada novamente.
3. **PDF**: gerado com JasperReports a partir dos dados da mensagem.
4. **E-mail**: enviado ao destinatário configurado, assunto `Etiqueta do item <codigoInterno>`, anexo
   `etiqueta-item-<itemId>.pdf` (`application/pdf`).
5. A solicitação é marcada como `CONCLUIDA` e a mensagem é confirmada (ack).

### Falhas, retry e DLQ

- Falhas de processamento (PDF, SMTP, banco) são retentadas no próprio consumer: 3 execuções no total
  (`RABBITMQ_RETRY_MAX_RETRIES=2`), com intervalo inicial de 2 s, multiplicador 2 e máximo de 10 s.
- Esgotadas as tentativas (ou em falha não retentável), a mensagem original é republicada na DLQ
  (persistente, mesmo `messageId`) com os cabeçalhos `x-exception-message`, `x-original-exchange` e
  `x-original-routingKey`, e é confirmada na fila principal. O stack trace completo fica no log (`ERROR`).
- A solicitação fica registrada como `ERRO` com a mensagem da falha.
- Logs: recebimento, conclusão (com duração), cada falha e o envio para a DLQ trazem `solicitacaoId` e `itemId`.

### Reprocessando mensagens da DLQ

Depois de corrigir a causa (ex.: SMTP voltou), republique a mensagem em `exchange_etiqueta` com a routing key
`routing_key_etiqueta` mantendo o mesmo `messageId` (pelo painel `http://localhost:15672`: *Queues > queue_etiqueta_dlq >
Get messages* e *Publish message* na exchange `exchange_etiqueta`; ou com o *shovel*). Como a solicitação não está
`CONCLUIDA`, ela é processada normalmente.

## E-mail (configuração fixa)
O e-mail é configurado direto em `src/main/resources/application.properties`, sem variáveis de ambiente: `spring.mail.host/port/username/password` (servidor SMTP e conta que envia) e `etiqueta.email.destinatario` (e-mail do Marcos). O remetente é a conta `spring.mail.username`. É preciso um servidor SMTP com login; no Gmail, use uma senha de app (https://myaccount.google.com/apppasswords). Para testar sem conta, use o Mailpit (`docker run -d -p 1025:1025 -p 8025:8025 axllent/mailpit`) com `host=localhost`, `port=1025`.

## Variáveis de ambiente

| Variável | Padrão | Uso |
|----------|--------|-----|
| `RABBITMQ_HOST` / `RABBITMQ_PORT` | `localhost` / `5672` | Broker |
| `RABBITMQ_USERNAME` / `RABBITMQ_PASSWORD` | `admin` / `admin` | Credenciais; os padrões são só os do `compose.yml` local, defina as variáveis nos demais ambientes |
| `RABBITMQ_RETRY_MAX_RETRIES` | `2` | Novas tentativas após a primeira falha |
| `RABBITMQ_RETRY_INITIAL_INTERVAL` | `2s` | Intervalo inicial do retry (multiplicador 2, máximo 10 s) |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | H2 em memória / `sa` / vazio | Banco do registro de idempotência |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | Criação/atualização da tabela |
| `BACKEND_API_BASE_URL` | `http://localhost:8080` | Usado somente pelo endpoint `GET /etiquetas/{itemId}/pdf` |
| `SERVER_PORT` | `8081` | Porta HTTP |

## Executando

```powershell
docker compose up -d            # RabbitMQ local (AMQP 5672, painel http://localhost:15672, admin/admin)

.\mvnw spring-boot:run
```

Testes: `.\mvnw test`.

## Endpoints HTTP

- `GET /etiquetas/solicitacoes/{solicitacaoId}`: consulta o andamento de uma solicitação (`PROCESSANDO`,
  `CONCLUIDA` ou `ERRO`, tentativas, datas e mensagem de erro); `404` se não existir.
- `GET /etiquetas/{itemId}/pdf`: geração síncrona sob demanda, anterior ao fluxo assíncrono e mantida como estava;
  exige o header `Authorization`, que é repassado à API do backend (`BACKEND_API_BASE_URL`) para buscar o item. Não faz
  parte do fluxo do botão "Gerar Etiqueta".
- Removidos: `POST /etiquetas/{itemId}/fila` (a publicação passou para o backend) e
  `GET /etiquetas/solicitacoes/{id}/pdf` (o PDF não é mais armazenado; ele é enviado por e-mail).

## Limitações

- Entrega *at-least-once*: se o consumer cair depois de o SMTP aceitar o e-mail e antes do ack, a mensagem é reentregue
  e o e-mail pode chegar duplicado.
- O registro de idempotência usa H2 em memória por padrão e se perde ao reiniciar o consumer. Para manter a proteção
  entre reinícios, aponte `SPRING_DATASOURCE_*` para um banco persistente.
- Os endpoints HTTP deste serviço não exigem autenticação (configuração anterior mantida); não o exponha publicamente.
