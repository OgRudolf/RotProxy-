package com.rotproxy.proxy;

import com.rotproxy.RotProxyMod;
import com.rotproxy.config.ProxyConfig;
import com.rotproxy.config.ProxyProfile;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.proxy.HttpProxyHandler;
import io.netty.handler.proxy.ProxyConnectionEvent;
import io.netty.handler.proxy.ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class ProxyManager {
    private static final URI IP_CHECK_URI = URI.create("http://api.ipify.org/");
    private static final String IP_CHECK_HOST = IP_CHECK_URI.getHost();
    private static final int IP_CHECK_PORT = 80;
    private static final Object AUTH_LOCK = new Object();

    public enum Status {
        DISABLED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    private static volatile Status status = Status.DISABLED;
    private static volatile String currentIp = "Unknown";
    private static volatile long latencyMs = -1L;
    private static volatile ProxyProfile activeProfile;
    private static volatile String lastError = "";

    private static ScheduledExecutorService scheduler;
    private static ScheduledFuture<?> heartbeatTask;
    private static NioEventLoopGroup probeEventLoop;

    private ProxyManager() {
    }

    public static void initialize() {
        if (scheduler != null) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "RotProxy-Heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        probeEventLoop = new NioEventLoopGroup(1, runnable -> {
            Thread thread = new Thread(runnable, "RotProxy-Probe");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static void restoreConfiguredProxy() {
        if (!ProxyConfig.isEnabled()) {
            clearRuntimeState();
            return;
        }

        ProxyProfile profile = ProxyConfig.getActiveProfile();
        if (profile == null || !profile.isValid()) {
            ProxyConfig.setEnabled(false);
            ProxyConfig.save();
            clearRuntimeState();
            return;
        }

        applyProxy(profile);
    }

    public static void applyProxy(ProxyProfile profile) {
        initialize();
        stopHeartbeat();

        if (profile == null || !profile.isValid()) {
            clearProxy();
            return;
        }

        activeProfile = profile.copy();
        status = Status.CONNECTING;
        currentIp = "Checking...";
        latencyMs = -1L;
        lastError = "";

        ProxyConfig.setEnabled(true);
        ProxyConfig.save();

        testAndUpdate(activeProfile);
        startHeartbeat(activeProfile);
    }

    public static void clearProxy() {
        stopHeartbeat();
        ProxyConfig.setEnabled(false);
        ProxyConfig.save();
        clearRuntimeState();
    }

    private static void clearRuntimeState() {
        activeProfile = null;
        status = Status.DISABLED;
        currentIp = "Unknown";
        latencyMs = -1L;
        lastError = "";
    }

    public static ProxyHandler createNettyProxyHandler() {
        ProxyProfile profile = activeProfile;
        if (!shouldInjectProxy(profile)) {
            return null;
        }

        return createNettyProxyHandler(profile);
    }

    private static ProxyHandler createNettyProxyHandler(ProxyProfile profile) {
        SocketAddress proxyAddress = new InetSocketAddress(profile.host.trim(), profile.port);
        ProxyHandler handler = switch (profile.type) {
            case SOCKS5 -> hasCredentials(profile)
                    ? new Socks5ProxyHandler(proxyAddress, profile.username, emptyToNull(profile.password))
                    : new Socks5ProxyHandler(proxyAddress);
            case HTTP, HTTPS -> hasCredentials(profile)
                    ? new HttpProxyHandler(proxyAddress, profile.username, profile.password)
                    : new HttpProxyHandler(proxyAddress);
        };

        handler.setConnectTimeoutMillis(5000L);
        return handler;
    }

    public static boolean shouldInjectProxy() {
        return shouldInjectProxy(activeProfile);
    }

    private static boolean shouldInjectProxy(ProxyProfile profile) {
        return ProxyConfig.isEnabled() && profile != null && profile.isValid();
    }

    private static void testAndUpdate(ProxyProfile profile) {
        CompletableFuture.runAsync(() -> {
            try {
                ProxyProbeResult result = probe(profile);
                if (!sameProfile(profile)) {
                    return;
                }

                latencyMs = result.latencyMs();
                currentIp = result.ipAddress();
                status = Status.CONNECTED;
                lastError = "";
            } catch (Exception exception) {
                if (!sameProfile(profile)) {
                    return;
                }

                setErrorState(exception);
            }
        });
    }

    private static ProxyProbeResult probe(ProxyProfile profile) throws Exception {
        if (profile.type == ProxyProfile.ProxyType.SOCKS5) {
            return probeSocks5(profile);
        }

        long started = System.nanoTime();
        java.net.URLConnection connection = openHttpIpConnection(profile);
        try (java.io.InputStream inputStream = connection.getInputStream()) {
            String ipAddress = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            return new ProxyProbeResult(ipAddress.isBlank() ? "Unknown" : ipAddress, elapsed);
        }
    }

    private static java.net.URLConnection openHttpIpConnection(ProxyProfile profile) throws Exception {
        java.net.Proxy proxy = switch (profile.type) {
            case SOCKS5 -> new java.net.Proxy(java.net.Proxy.Type.SOCKS, new InetSocketAddress(profile.host.trim(), profile.port));
            case HTTP, HTTPS -> new java.net.Proxy(java.net.Proxy.Type.HTTP, new InetSocketAddress(profile.host.trim(), profile.port));
        };

        return withAuthenticator(profile, () -> {
            URLConnection connection = IP_CHECK_URI.toURL().openConnection(proxy);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "RotProxy/1.1");

            if ((profile.type == ProxyProfile.ProxyType.HTTP || profile.type == ProxyProfile.ProxyType.HTTPS) && hasCredentials(profile)) {
                String raw = profile.username + ":" + Objects.requireNonNullElse(profile.password, "");
                String encoded = Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
                connection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
            }

            return connection;
        });
    }

    private static ProxyProbeResult probeSocks5(ProxyProfile profile) throws Exception {
        initialize();

        CompletableFuture<ProxyProbeResult> resultFuture = new CompletableFuture<>();
        long started = System.nanoTime();

        Bootstrap bootstrap = new Bootstrap()
                .group(probeEventLoop)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) {
                        channel.pipeline().addLast("rotproxy_probe_proxy", createNettyProxyHandler(profile));
                        channel.pipeline().addLast("rotproxy_probe_http", new HttpClientCodec());
                        channel.pipeline().addLast("rotproxy_probe_aggregator", new HttpObjectAggregator(1024));
                        channel.pipeline().addLast("rotproxy_probe_handler", new SimpleChannelInboundHandler<FullHttpResponse>() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext context, Object event) {
                                if (event instanceof ProxyConnectionEvent) {
                                    DefaultFullHttpRequest request = new DefaultFullHttpRequest(
                                            HttpVersion.HTTP_1_1,
                                            HttpMethod.GET,
                                            "/"
                                    );
                                    request.headers().set(HttpHeaderNames.HOST, IP_CHECK_HOST);
                                    request.headers().set(HttpHeaderNames.USER_AGENT, "RotProxy/1.1");
                                    request.headers().set(HttpHeaderNames.ACCEPT, "text/plain");
                                    request.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                                    context.writeAndFlush(request);
                                    return;
                                }

                                context.fireUserEventTriggered(event);
                            }

                            @Override
                            protected void channelRead0(ChannelHandlerContext context, FullHttpResponse response) {
                                if (!response.status().equals(HttpResponseStatus.OK)) {
                                    resultFuture.completeExceptionally(new IllegalStateException("IP check returned " + response.status()));
                                } else {
                                    String ipAddress = response.content().toString(StandardCharsets.UTF_8).trim();
                                    long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
                                    resultFuture.complete(new ProxyProbeResult(ipAddress.isBlank() ? "Unknown" : ipAddress, elapsed));
                                }
                                context.close();
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
                                resultFuture.completeExceptionally(cause);
                                context.close();
                            }

                            @Override
                            public void channelInactive(ChannelHandlerContext context) {
                                if (!resultFuture.isDone()) {
                                    resultFuture.completeExceptionally(new IllegalStateException("Proxy closed before IP check completed"));
                                }
                            }
                        });
                    }
                });

        ChannelFuture future = bootstrap.connect(new InetSocketAddress(IP_CHECK_HOST, IP_CHECK_PORT));
        future.addListener(connectFuture -> {
            if (!connectFuture.isSuccess()) {
                resultFuture.completeExceptionally(connectFuture.cause());
            }
        });

        return resultFuture.get(7, TimeUnit.SECONDS);
    }

    private static <T> T withAuthenticator(ProxyProfile profile, ThrowingSupplier<T> supplier) throws Exception {
        synchronized (AUTH_LOCK) {
            if (hasCredentials(profile)) {
                Authenticator.setDefault(new ProxyAuthenticator(profile.username, profile.password));
            } else {
                Authenticator.setDefault(null);
            }

            try {
                return supplier.get();
            } finally {
                Authenticator.setDefault(null);
            }
        }
    }

    private static void startHeartbeat(ProxyProfile profile) {
        heartbeatTask = scheduler.scheduleAtFixedRate(() -> {
            if (!sameProfile(profile) || !ProxyConfig.isEnabled()) {
                return;
            }

            try {
                ProxyProbeResult result = probe(profile);
                latencyMs = result.latencyMs();
                currentIp = result.ipAddress();
                status = Status.CONNECTED;
                lastError = "";
            } catch (Exception exception) {
                setErrorState(exception);
            }
        }, 15L, 15L, TimeUnit.SECONDS);
    }

    private static void stopHeartbeat() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
        }
    }

    private static boolean sameProfile(ProxyProfile profile) {
        ProxyProfile current = activeProfile;
        return current != null
                && current.type == profile.type
                && current.port == profile.port
                && Objects.equals(current.host, profile.host)
                && Objects.equals(current.username, profile.username)
                && Objects.equals(current.password, profile.password);
    }

    private static void setErrorState(Exception exception) {
        status = Status.ERROR;
        currentIp = "Unknown";
        latencyMs = -1L;
        lastError = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
        RotProxyMod.LOGGER.warn("RotProxy proxy probe failed: {}", lastError);
    }

    private static boolean hasCredentials(ProxyProfile profile) {
        return profile.username != null && !profile.username.isBlank();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public static Status getStatus() {
        return status;
    }

    public static String getCurrentIp() {
        return currentIp;
    }

    public static long getLatencyMs() {
        return latencyMs;
    }

    public static boolean isProxyHealthy() {
        return status == Status.CONNECTED;
    }

    public static boolean isEnabled() {
        return ProxyConfig.isEnabled();
    }

    public static String getLastError() {
        return lastError;
    }

    public static String getActiveProfileName() {
        ProxyProfile profile = activeProfile;
        return profile == null ? "None" : profile.name;
    }

    private record ProxyProbeResult(String ipAddress, long latencyMs) {
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static final class ProxyAuthenticator extends Authenticator {
        private final String username;
        private final String password;

        private ProxyAuthenticator(String username, String password) {
            this.username = username;
            this.password = password == null ? "" : password;
        }

        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(username, password.toCharArray());
        }
    }
}
