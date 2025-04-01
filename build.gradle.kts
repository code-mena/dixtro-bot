plugins {
	java
	id("org.springframework.boot") version "3.3.6"
	id("io.spring.dependency-management") version "1.1.5"
}

group = "com.vaatu.bots"
version = "1.0.0"

java {
	sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
	maven("https://jitpack.io")
	maven("https://maven.lavalink.dev/releases")
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("net.dv8tion:JDA:5.3.1")
	implementation("dev.arbjerg:lavaplayer:2.2.3")
	implementation("dev.lavalink.youtube:v2:1.12.0")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
