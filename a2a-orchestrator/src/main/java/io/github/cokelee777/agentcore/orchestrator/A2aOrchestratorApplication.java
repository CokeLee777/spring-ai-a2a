package io.github.cokelee777.agentcore.orchestrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the A2A Orchestrator service.
 *
 * <p>
 * Runs on port 9000 as required by the Amazon Bedrock AgentCore Runtime A2A contract and
 * coordinates downstream order, delivery, and payment agents via LLM tool-calling.
 * </p>
 */
@SpringBootApplication
public class A2aOrchestratorApplication {

	/**
	 * Create a new {@link A2aOrchestratorApplication}.
	 */
	public A2aOrchestratorApplication() {
	}

	/**
	 * Application entry point.
	 * @param args command-line arguments passed to {@link SpringApplication}
	 */
	public static void main(String[] args) {
		SpringApplication.run(A2aOrchestratorApplication.class, args);
	}

}
