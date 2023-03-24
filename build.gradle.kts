plugins {
    java
    application
}

group = "com.angellane"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("args4j", "args4j", "2.33")

    testImplementation("org.junit.jupiter", "junit-jupiter-api",    "5.9.2")
    testImplementation("org.junit.jupiter", "junit-jupiter-engine", "5.9.2")
}

configure<JavaPluginExtension> {
    // Java 9+ needed for modules, so may as well go to next major LTS
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

application {
    mainClass.set("com.angellane.juggle.Main")
}

afterEvaluate {
    tasks.withType(JavaCompile::class) {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
    }
}

tasks.jar {
    manifest.attributes["Main-Class"] = application.mainClass
    val dependencies = configurations
        .runtimeClasspath
        .get()
        .map(::zipTree)
    from(dependencies)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Test> {
    useJUnitPlatform()
}
