# Arquitetura proposta

## Forma

Monólito modular com uma API e um banco MySQL. Controladores autenticam e validam contratos; serviços de aplicação coordenam casos de uso; domínio protege regras; infraestrutura acessa banco e fornecedores. O frontend nunca determina saldo, autorização ou estado final.

## Módulos

| Módulo | Responsabilidade | Dependência permitida principal |
| --- | --- | --- |
| identity | Conta, autenticação, idade e verificação | políticas de elegibilidade |
| profiles | Projeções públicas, idiomas, interesses, disponibilidade | identity por interface pública |
| matching | Elegibilidade e ranking explicável | profiles |
| encounters | Convites, aceites, check-in e ciclo do encontro | credits por interface pública |
| credits | Carteira, reservas e lançamentos imutáveis | nenhuma tabela externa |
| payments | Dinheiro e gateway externo | interfaces do provedor |
| tracking | Consentimento e localização temporária | autorização do encontro |
| reviews | Elegibilidade, submissão e publicação | estado público do encontro |
| support | Contestação, moderação e auditoria | interfaces públicas dos módulos |

Cada módulo futuro deve conter apenas camadas com responsabilidade real (`api`, `application`, `domain`, `infrastructure`). A confirmação de encontro e o consumo da reserva compartilham uma transação. Eventos externos são gravados na outbox na mesma transação.

## Tecnologia proposta, não aprovada como regra comercial

Java 21 com Spring Boot e MySQL no backend; React com TypeScript para evolução da interface web. As versões devem ser verificadas quando a implementação dependente começar. Este protótipo estático evita fixar dependências sem validação.
