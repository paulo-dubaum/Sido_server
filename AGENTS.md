AGENTS.md — Sido (servidor Java)
1. Papel e prioridade
Você atua como engenheiro de software no projeto Sido, um RPG multiplayer com cliente Unity (C#) e servidor Java baseado em SmartFoxServer 3 (SFS3).
Prioridades, nesta ordem:
Preservar comportamento funcional e integridade do projeto.
Respeitar decisões de design aprovadas pelo responsável pelo projeto.
Implementar mudanças pequenas, verificáveis e reversíveis.
Manter código legível, consistente com o estilo existente e testável.
Este arquivo descreve decisões conhecidas, não substitui a leitura do código. Em caso de divergência entre o código e uma regra aqui descrita, não corrija silenciosamente: apresente a divergência, seu impacto e uma proposta de resolução.
2. Fluxo obrigatório de trabalho
Antes de editar
Leia os arquivos relevantes integralmente, incluindo chamadas, contratos públicos, modelos e configuração.
Procure implementações existentes antes de criar classes, estados, registries, eventos ou fórmulas.
Verifique o estado do Git (`git status`) e a branch atual. Não descarte alterações do usuário.
Identifique dependências e efeitos colaterais sobre outros subsistemas.
Para tarefas não triviais, apresente um plano incremental e aguarde aprovação antes de editar, salvo autorização explícita para implementar.
Se houver informação essencial ausente, faça perguntas objetivas. Não suponha nomes de classes, caminhos, contratos ou parâmetros.
Durante a edição
Faça o menor diff que resolva o problema. Prefira métodos e alterações localizadas a reescrever arquivos completos.
Preserve assinaturas, pacotes, APIs, formatos de mensagens e comportamento não relacionado.
Não introduza dependências, bibliotecas, frameworks, mudanças de build ou alterações de esquema de dados sem aprovação.
Não faça refatorações oportunistas em paralelo à funcionalidade solicitada.
Não altere arquivos gerados, segredos ou configuração de produção sem autorização.
Evite `git reset --hard`, `git clean`, force push e outras operações destrutivas.
Não faça commit, push, merge ou publicação sem solicitação explícita.
Depois da edição
Revise o `git diff` e identifique todos os arquivos modificados.
Execute compilação e testes relevantes, se disponíveis. Não declare sucesso sem executar.
Se não puder compilar ou testar, descreva precisamente o impedimento.
Relate o que mudou, por quê, riscos, testes executados e itens pendentes.
Não apresente uma implementação parcial como funcionalidade concluída.
3. Arquitetura e autoridade do servidor
O servidor SFS3 é a autoridade para combate, dano, vida, validação de alvos, alcance, estados de NPCs e regras de movimentação.
O cliente Unity solicita ações e apresenta o resultado; não determina dano nem valida sozinho ataques.
Use o modelo de sincronização, eventos e ciclo de simulação já existente no projeto.
Em integrações de extensões SFS3, os parâmetros personalizados de respostas são acessados pelos clientes via `EventParam.ExtParams` (`"extParams"`). Não substitua por `"params"`.
Preserve os contratos de rede existentes; qualquer novo comando/evento deve ter payload, validação, destinatários e semântica documentados antes da implementação.
Respeite os mecanismos atuais de sincronização e concorrência. Não presuma que callbacks e ticks são executados na mesma thread.
4. Escopo atual: combate básico do Warrior
O desenvolvimento atual está limitado ao Warrior (classe 1) atacando NPCs. Não implemente ataques de Archer ou Mage nesta etapa.
Ciclo de ataque
Fases conceituais: `PREPARING → IMPACT → RECOVERY`.
`attacksPerSecond` determina o intervalo total entre inícios de ataques consecutivos.
`attackPreparationMs` é explícito; não derive automaticamente seu valor sem uma regra aprovada.
Durante `PREPARING`, o Warrior permanece imóvel; o servidor deve impedir movimentação aceita nesse estado.
Após o impacto, durante `RECOVERY`, a movimentação volta a ser permitida.
O impacto ocorre apenas após a preparação e exige nova validação do alvo e do alcance.
Cancele ataques pendentes quando o personagem morrer ou quando o alvo deixar de ser válido antes do impacto.
Não invente enfileiramento, ataque automático contínuo, consumo de recursos ou regras de interrupção adicionais.
Alcance
Alcance básico do Warrior: 1,5 metro.
Alcance total = 1,5 m + bônus de alcance da arma equipada.
Exemplo: espada longa com `attackReachBonusMeters = 0.5` resulta em 2,0 m.
Faça a validação no servidor usando as posições e a representação espacial efetivamente usadas no projeto; não invente tolerâncias ou regras de colisão.
Dano
Calcule o dano bruto antes da mitigação.
Aplique mitigação de defesa uma única vez: `danoRecebido = danoBruto × (1 − reduçãoDeDefesa)`, com o arredondamento definido pela implementação aprovada.
Não invente a fórmula de composição do dano base da classe com o dano da arma, nem a fórmula da redução de defesa. Identifique a regra existente ou solicite aprovação.
Preserve distinções entre dano físico e elemental onde existirem.
Sorteios e cálculo de dano são executados no servidor.
Armas e catálogo
A instância `character.equipment.Weapon` identifica o item persistido e a definição (`definitionId`); ela não é, por si só, a fonte de todos os atributos de combate.
Consulte as definições do catálogo de itens existente (`items.json` / `items.ItemData`) em vez de duplicar números no código.
`ItemData` oferece `minAttack`, `maxAttack`, `attackSpeed` e `attackReachBonusMeters`.
Valores conhecidos de teste:
Espada longa, definição 10: dano 20–30; velocidade 1,4 ataques/s; bônus de alcance +0,5 m.
Espada curta, definição 11: dano 10–20; velocidade 1,8 ataques/s; bônus de alcance +0,5 m.
Não assuma que `WeaponType` corresponde diretamente ao `ItemType` do catálogo; examine a conversão existente.
Não invente regras de refinamento ou bônus ainda não especificadas.
5. NPCs e comportamento de retorno
Preserve a máquina de estados e a navegação existentes no `NpcManager`.
Um NPC vivo e apto a combater, em `RETURNING`, quando atingido por ataque válido do Warrior, deve interromper o retorno e iniciar `CHASE` contra o primeiro atacante que provocar essa transição.
Remova, para esse NPC e atacante, o bloqueio de reacquisição aplicável (`ignoredUntilExit`), de modo que o ataque efetivamente provoque perseguição.
Não altere a regra normal de desengajamento dos NPCs que não forem atacados.
Revalide estado, alvo e vida antes de mudar a máquina de estados.
A reação ao ataque durante `RETURNING` não equivale à correção do bug de NPCs fisicamente travados ao retornar ao spawn.
Investigue separadamente obstáculos, ocupação do spawn, ausência de progresso, caminhos e condições de saída de `RETURNING`; proponha correção específica, com testes de regressão.
Não considere o bug resolvido sem reproduzir e verificar o caso relevante.
6. Personagens, vida e respawn
A vida máxima depende da classe e da Vitality:
Warrior: `maxHealth = Vitality * 3 + 35`.
Archer: `maxHealth = Vitality * 2 + 30`.
Mage: `maxHealth = Vitality * 2 + 25`.
Quando Vitality aumentar, ajuste a vida atual somente pela diferença entre o novo e o antigo `maxHealth`; não restaure a vida por completo.
Exemplo: Warrior com 50/95 passa a 53/98 após ganhar 1 Vitality.
O respawn é automático 3 segundos após a morte, com a vida restaurada a 100% da vida máxima.
Local de teste atual: mapa 0, posição `(245.0f, 6.039f, 167.5f)`.
Esse local é provisório e não deve virar uma regra global; futuramente os mapas terão regras próprias de respawn.
Preserve o fluxo existente de notificações de morte e retorno à vida.
7. Equipamentos e modificadores futuros
A defesa do personagem deve ser determinada pelos itens equipados, conforme as regras do projeto; não introduza defesa por evolução sem aprovação.
Afixos excelentes e efeitos de conjunto não devem ser implementados agora.
Preserve pontos de extensão para modificadores futuros, que poderão afetar ataque, defesa, recursos, velocidade, recompensas e outras fórmulas.
Não atribua efeitos concretos a esses sistemas antes de aprovação.
8. Integração e testes esperados
Antes de concluir a implementação de combate do Warrior, verificar, no mínimo:
Ataque aceito contra NPC vivo, válido e dentro do alcance.
Ataque rejeitado quando o NPC está fora do alcance.
Bônus de alcance da espada longa: 1,5 m + 0,5 m = 2,0 m.
Movimento rejeitado durante `PREPARING` e permitido durante `RECOVERY`.
Revalidação de alcance e estado no instante do impacto.
Cancelamento de ataque pendente após morte do Warrior ou invalidação do NPC.
Intervalo entre ataques compatível com `attacksPerSecond`, incluindo o tempo de preparação.
Dano mitigado uma única vez e vida do NPC nunca negativa.
NPC em `RETURNING` atingido por ataque válido passa a `CHASE` contra o primeiro atacante.
NPC não atacado mantém o desengajamento normal.
NPC morto segue o fluxo de morte/remoção/respawn já existente.
Sem regressão de navegação e sincronização de posição.
Use testes automatizados quando o projeto oferecer infraestrutura; caso contrário, descreva cenários de validação manual reproduzíveis. Não adicione infraestrutura de testes extensa sem aprovação.
9. Comunicação e decisões pendentes
Responda em português brasileiro. Preserve nomes de classes, métodos, variáveis, estados e APIs em inglês, conforme o projeto.
Distinga explicitamente: implementado, aprovado mas não implementado, hipótese e pendente de decisão.
Não trate exemplos numéricos como regras globais.
Antes de propor uma fórmula nova, exponha alternativas, consequências e solicite decisão.
Ao final de cada tarefa, apresente: resumo das alterações, arquivos afetados, verificações executadas, riscos e próximo passo recomendado.
10. Primeira tarefa sugerida
Na branch `warrior-combat`, analise sem editar a implementação atual de `Character`, `Warrior`, `CharacterDerivedStats`, `CharacterFormulaRegistry`, `CombatSystem`, `NpcManager`, `NpcData`, `NpcItem`, `RoomSimulationContext`, `UserVariableUpdate`, catálogo de itens e handlers de extensão.
Identifique:
Como a classe Warrior recebe seus atributos derivados.
Como as definições de armas são recuperadas por `definitionId`.
Onde inserir o comando de ataque e o tick de combate sem duplicar mecanismos.
Como bloquear movimento durante `PREPARING`.
Como aplicar dano e transições de NPC com segurança.
Quais fórmulas e parâmetros ainda precisam de decisão.
Entregue um plano incremental e aguarde aprovação antes de modificar arquivos.