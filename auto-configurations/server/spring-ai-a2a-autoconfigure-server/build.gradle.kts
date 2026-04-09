dependencies {
	implementation(project(":spring-ai-a2a-server"))
	implementation("org.springframework.ai:spring-ai-client-chat")
	implementation("org.springframework.boot:spring-boot-autoconfigure")
	implementation("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-web")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	enabled = false
}

tasks.named<Jar>("jar") {
	enabled = true
}
