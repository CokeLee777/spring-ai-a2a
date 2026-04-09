dependencies {
	implementation(project(":spring-ai-a2a-starter-agent-common"))
	implementation(project(":spring-ai-a2a-starter-server"))
	implementation("org.springframework.ai:spring-ai-client-chat")
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	implementation("org.springframework.ai:spring-ai-starter-model-bedrock-converse")
	implementation("org.springframework.boot:spring-boot-starter-web")
}
