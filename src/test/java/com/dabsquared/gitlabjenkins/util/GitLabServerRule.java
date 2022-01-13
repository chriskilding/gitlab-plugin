package com.dabsquared.gitlabjenkins.util;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

public class GitLabServerRule extends ExternalResource {

    private final Network network = Network.newNetwork();

    private static final int GITLAB_PORT = 80;
    private static final int GITLAB_SSH_PORT = 22;

    // TODO can we use the trusted official 'postgres' image?
    private final GenericContainer<?> postgres = new GenericContainer<>("sameersbn/postgresql:9.5-1")
        .withNetwork(network)
        .withNetworkAliases("it-gitlab-postgres")
        .withExposedPorts(5432)
        .withEnv("DB_NAME", "gitlabhq_production")
        .withEnv("DB_USER", "gitlab")
        .withEnv("DB_PASS", "password")
        .withEnv("DB_EXTENSION", "pg_trgm")
        .waitingFor(Wait.forListeningPort());

    // FIXME no image version pin
    // TODO can we use the trusted official 'redis' image?
    private final GenericContainer<?> redis = new GenericContainer<>("sameersbn/redis")
        .withNetwork(network)
        .withNetworkAliases("it-gitlab-redis")
        .withExposedPorts(6379)
        .waitingFor(Wait.forListeningPort());

    // TODO can we use GitLab's own gitlab image?
    private final GenericContainer<?> gitlab = new GenericContainer<>("sameersbn/gitlab:8.17.4")
        .withNetwork(network)
        .withNetworkAliases("it-gitlab-gitlab")
        .withExposedPorts(GITLAB_PORT, GITLAB_SSH_PORT)
        .withEnv("DEBUG", "false")
        .withEnv("TZ", "Asia/Kolkata")
        .withEnv("GITLAB_TIMEZONE", "Kolkata")
        .withEnv("GITLAB_PORT", String.valueOf(GITLAB_PORT))
        .withEnv("GITLAB_SSH_PORT", String.valueOf(GITLAB_SSH_PORT))
        .withEnv("GITLAB_SECRETS_DB_KEY_BASE", "long-and-random-alpha-numeric-string")
        .withEnv("GITLAB_SECRETS_SECRET_KEY_BASE", "long-and-random-alphanumeric-string")
        .withEnv("GITLAB_SECRETS_OTP_KEY_BASE", "long-and-random-alpha-numeric-string")
        .withEnv("GITLAB_HOST", "172.17.0.1")
        .waitingFor(Wait.forHttp("/"));

    /**
     * @return the GitLab base URL
     */
    public String getUrl() {
        final String host = gitlab.getHost();
        final int port = gitlab.getMappedPort(GITLAB_PORT);
        return String.format("http://%s:%d", host, port);
    }

    public String getPostgresAddress() {
        final String host = postgres.getHost();
        final int port = postgres.getFirstMappedPort();
        return String.format("%s:%d", host, port);
    }

    @Override
    public void before() {
        postgres.start();
        redis.start();
        gitlab.start();
    }

    @Override
    protected void after() {
        gitlab.stop();
        redis.stop();
        postgres.stop();
    }
}
