# AI Manifest — Underground

Atualizado em 30/09/2026. Este arquivo é o ponto de entrada para agentes. Status possíveis: **aprovado**, **proposto**, **implementado** e **validado**. Código observado comprova implementação, mas não altera decisões aprovadas.

| Área | Fonte | Status | Evidência / observação |
| --- | --- | --- | --- |
| Produto, limites e fluxo | [Requisitos](docs/requirements.md) | aprovado | Especificação v0.1 fornecida pelo produto |
| Arquitetura modular | [Arquitetura](docs/architecture.md) | aprovado / implementado | identity, profiles e shared; fronteira pública IdentityDirectory |
| Stack da primeira entrega | [Backend](backend/README.md) | aprovado / implementado | Java 21, Maven, Spring Boot, MySQL, Flyway, Security e OpenAPI, conforme pedido |
| Políticas comerciais | [Decisões](docs/decisions.md) | aprovado / em aberto | Itens abertos permanecem sem regra inventada |
| Protótipo navegável | [Frontend](frontend/README.md) | implementado | Dados fictícios, nove destinos, pt-BR e pt-PT; ainda não integrado à API |
| Acessibilidade do protótipo | [Checklist](docs/accessibility.md) | implementado | Auditoria humana pendente |
| Cadastro, autenticação e perfis | [Backend](backend/README.md) | implementado / validado | Maven verify com Java 21: seis testes de integração e dois testes ArchUnit, sem falhas |
| Migração Flyway | [V1](backend/src/main/resources/db/migration/V1__identity_and_profiles.sql) | implementado / validado em H2 | Mesma migração aplicada nos testes; execução em MySQL ainda não validada |
| Autorização e privacidade | [Testes](backend/src/test/java/com/underground/AccessIntegrationTest.java) | implementado / validado | CSRF, login real, rotação da sessão/token, logout, titularidade, projeção pública, pendência e bloqueio de privilégios |
| OpenAPI | [Configuração](backend/src/main/java/com/underground/shared/OpenApiConfiguration.java) | implementado / validado | Endpoint testado; habilitação por ambiente, desabilitado por padrão |
| Fornecedor de identidade e operação administrativa | [Decisões](docs/decisions.md) | proposto | Sem integração ou endpoint de aprovação; ADMIN não possui acesso irrestrito |
| Matching e módulos transacionais | [Arquitetura](docs/architecture.md) | proposto | Não implementados; elegibilidade já exclui contas pendentes/rejeitadas |

## Restrições para alterações

1. Divergências entre código e decisão aprovada devem ser registradas em `docs/decisions.md` antes de resolução.
2. Não promover recomendação técnica a regra comercial.
3. Toda mudança em autorização, saldo ou estado exige teste automatizado no backend.
4. Textos de interface pertencem a catálogos de idioma; dados privados não entram em projeções públicas.
5. Não marcar MySQL, integrações externas ou operação de produção como validados com base nos testes H2.
