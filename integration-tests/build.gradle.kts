dependencies {
    testImplementation(project(":agent-common"))
    testImplementation(project(":agents:host-agent"))
    testImplementation(project(":agents:order-agent"))
    testImplementation(project(":agents:delivery-agent"))
    testImplementation(project(":agents:payment-agent"))
    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.ai:spring-ai-client-chat")
}

tasks.named<Test>("test") {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = false
}

sourceSets {
    test {
        java.srcDir("src/test/java")
        resources.srcDir("src/test/resources")
    }
}
