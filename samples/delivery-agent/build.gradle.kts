dependencies {
	implementation(project(":spring-ai-a2a-agent-common"))
	implementation(project(":spring-ai-a2a-autoconfigure-agent-common"))
	implementation(project(":spring-ai-a2a-server"))
	implementation(project(":spring-ai-a2a-autoconfigure-server"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.ai:spring-ai-starter-model-bedrock-converse")
	implementation("org.springframework.ai:spring-ai-client-chat")
}

tasks.named<Jar>("jar") {
	enabled = true
}
