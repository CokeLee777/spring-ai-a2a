val a2aVersion = rootProject.extra["a2aVersion"] as String

dependencies {
    implementation(project(":a2a-common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.github.a2asdk:a2a-java-sdk-spec:$a2aVersion")
    implementation("io.github.a2asdk:a2a-java-sdk-client:$a2aVersion")
    implementation("io.github.a2asdk:a2a-java-sdk-server-common:$a2aVersion")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}
