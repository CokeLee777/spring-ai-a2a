dependencies {
	implementation(project(":spring-ai-a2a-model-chat-memory-repository-bedrock-agent-core"))
	implementation("org.springframework.ai:spring-ai-autoconfigure-model-bedrock-ai")
	implementation("org.springframework.ai:spring-ai-autoconfigure-model-chat-memory")
	implementation("org.springframework.ai:spring-ai-client-chat")
	implementation("org.springframework.boot:spring-boot-autoconfigure")
	implementation("org.springframework.boot:spring-boot-configuration-processor")
	implementation("software.amazon.awssdk:bedrockagentcore")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	enabled = false
}

tasks.named<Jar>("jar") {
	enabled = true
}
