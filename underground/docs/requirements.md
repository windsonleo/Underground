# Requisitos do MVP v0.1

## Escopo aprovado

Underground oferece companhia social **não sexual** entre adultos para jantares, concertos e experiências culturais. Cliente e anfitrião têm identidade verificada. O lançamento pretendido contempla Brasil e Portugal.

O fluxo aprovado é: cadastro e verificação; descoberta determinística; convite com reserva atômica de créditos; aceite do anfitrião; confirmação do cliente; encontro; rastreamento consentido; check-in; encerramento; avaliações mútuas.

## Invariantes

- Conta pendente não participa do matching.
- Convite segue `PENDING_HOST → PENDING_CLIENT → CONFIRMED`, ou termina antes em `DECLINED`, `EXPIRED` ou `CANCELLED`.
- Encontro segue `CONFIRMED → IN_PROGRESS → COMPLETED`, podendo terminar em `CANCELLED` ou `NO_SHOW`.
- Horário é persistido em UTC e apresentado no fuso do encontro.
- Saldo disponível é saldo contabilizado menos reservas ativas e nunca fica negativo.
- Reserva não é débito; consumo é atômico com a criação do encontro; restituição é lançamento compensatório.
- Valores monetários usam unidades mínimas e moeda explícita. Não há conversão implícita de BRL e EUR.
- Só participantes autorizados acessam encontro, local e localização.
- Check-in exige participante autenticado, código protegido, temporário, de uso único e com limite de tentativas.
- Avaliações exigem check-in e encontro encerrado, são únicas por autor/encontro e não admitem autoavaliação.
- Notificações externas saem de outbox após commit, com tentativa e deduplicação.

## Entrega atual

A entrega inclui o protótipo com dados fictícios e a primeira etapa do backend: cadastro, autenticação e perfis. O frontend ainda não consome a API. Verificação externa, matching e operações transacionais permanecem para entregas futuras.
