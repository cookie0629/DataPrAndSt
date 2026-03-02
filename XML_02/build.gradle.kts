plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("jakarta.xml.bind:jakarta.xml.bind-api:4.0.0")
    implementation("org.glassfish.jaxb:jaxb-runtime:2.3.2")
    implementation("com.sun.xml.bind:jaxb-impl:4.0.0")
    implementation("javax.xml.bind:jaxb-api:2.3.0")
    implementation("javax.activation:activation:1.1")
}

tasks.test {
    useJUnitPlatform()
}