# Registro de decisões

## Aprovado

- Monólito modular; cliente e anfitrião verificados.
- Matching por preferências, disponibilidade e área, com ordenação explicável.
- Reserva de créditos no convite e consumo após aceite mútuo.
- Recusa e expiração liberam integralmente a reserva.
- Serviço pago no app ou no local, separadamente dos créditos.
- Rastreamento apenas após aceite mútuo e com consentimento.
- Avaliações mútuas após check-in e encerramento.
- Cancelamento/ausência confirmada do anfitrião devolve créditos; desistência do cliente após confirmação mantém consumo.

## Em aberto

Preço, pacotes e validade de créditos; duração de convites; prazo de avaliações; atraso; elegibilidade por falha; cancelamento/restituição do serviço; fornecedores; retenção de dados; suporte; políticas de autorização, captura e restituição.

Nenhuma opção visual no protótipo resolve esses tópicos. Rótulos como “12 créditos” e horários são conteúdo fictício para testar hierarquia.

## Divergências

Nenhuma registrada nesta entrega.

## Implementação técnica da primeira entrega

- Aprovado pelo pedido desta entrega: Java 21, Maven, Spring Boot, MySQL em execução normal, H2 apenas em testes, Flyway, Spring Security e OpenAPI.
- Implementado: sessão de servidor com CSRF, cadastro CLIENT/HOST e estado inicial PENDING. ADMIN existe no modelo, sem criação pública nem acesso irrestrito.
- Implementado: declaração de maioridade obrigatória; ela não substitui verificação de identidade. Não há API que aprove identidade.
- Proposto para evolução: fluxo operacional de verificação, provisionamento administrativo auditado e controles de tentativas. Sem fornecedor ou política comercial presumidos.
- Escolha técnica desta entrega: perfis públicos acessíveis apenas entre contas verificadas; pendentes editam somente seu próprio perfil. Campos públicos: identificador, nome de exibição e bio.
