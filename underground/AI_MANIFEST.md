# AI Manifest — Underground

Atualizado em 30/09/2026. Este arquivo é o ponto de entrada para agentes. Status possíveis: **aprovado**, **proposto**, **implementado** e **validado**. Código observado comprova implementação, mas não altera decisões aprovadas.

| Área | Fonte | Status | Evidência / observação |
| --- | --- | --- | --- |
| Produto, limites e fluxo | [Requisitos](docs/requirements.md) | aprovado | Especificação v0.1 fornecida pelo produto |
| Arquitetura modular | [Arquitetura](docs/architecture.md) | proposto | Fronteiras e dependências documentadas; backend ainda não implementado |
| Políticas comerciais | [Decisões](docs/decisions.md) | aprovado / em aberto | Itens abertos permanecem explicitamente sem regra inventada |
| Protótipo navegável | [Frontend](frontend/README.md) | implementado | Dados fictícios, nove destinos, pt-BR e pt-PT |
| Acessibilidade do protótipo | [Checklist](docs/accessibility.md) | implementado | Skip link, foco visível, semântica e regiões de estado; auditoria humana pendente |
| Backend e integrações | [Arquitetura](docs/architecture.md) | proposto | Nenhuma API, pagamento, mapa ou identidade é simulada como real |

## Restrições para alterações

1. Divergências entre código e decisão aprovada devem ser registradas em `docs/decisions.md` antes de resolução.
2. Não promover recomendação técnica a regra comercial.
3. Toda mudança em autorização, saldo ou estado exige teste automatizado no backend quando ele existir.
4. Textos de interface pertencem a catálogos de idioma; dados privados não entram em projeções públicas.
