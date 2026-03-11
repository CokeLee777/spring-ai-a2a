package io.github.cokelee777.agentcore.autoconfigure.controller;

import io.a2a.spec.AgentCard;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller serving the agent card at the RFC 8615 well-known endpoint.
 *
 * <p>
 * Each server module provides an {@link AgentCard} bean; this controller serves it at
 * {@code /.well-known/agent-card.json} so that other agents can discover this agent's
 * capabilities and skills.
 * </p>
 */
@RestController
public class A2AAgentCardController {

	private final AgentCard agentCard;

	/**
	 * Create a new {@link A2AAgentCardController}.
	 * @param agentCard the agent card bean to serve
	 */
	public A2AAgentCardController(AgentCard agentCard) {
		this.agentCard = agentCard;
	}

	/**
	 * Returns the agent card for A2A discovery.
	 * @return {@code 200 OK} with the {@link AgentCard} as JSON
	 */
	@GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AgentCard> getAgentCard() {
		return ResponseEntity.ok(agentCard);
	}

}
