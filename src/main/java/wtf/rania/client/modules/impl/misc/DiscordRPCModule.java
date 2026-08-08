package wtf.rania.client.modules.impl.misc;

import wtf.rania.client.modules.Category;
import wtf.rania.client.modules.Module;
import wtf.rania.client.modules.ModuleInfo;
import wtf.rania.client.modules.values.impl.BoolValue;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ChatComponentText;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ModuleInfo(name = "DiscordRPC", description = "Discord Rich Presence", category = Category.MISC)
public final class DiscordRPCModule extends Module {

    private static final int OP_HANDSHAKE = 0;
    private static final int OP_FRAME = 1;
    private static final int OP_CLOSE = 2;
    private static final int OP_PING = 3;
    private static final int OP_PONG = 4;

    private static final String CLIENT_ID = "1449729019691012126";

    private final BoolValue hideServer = new BoolValue("Hide server", false, this);

    private final Object sendLock = new Object();

    private volatile boolean running;
    private volatile boolean linkAlive;
    private Transport transport;
    private Thread readerThread;
    private long startTimestamp;

    public DiscordRPCModule() {
        addValue(this.hideServer);
    }

    private void setupActivity() {
        if (this.running) {
            return;
        }

        this.running = true;

        while (this.running) {
            try {
                connect();
                startTimestamp = Instant.now().getEpochSecond();

                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§7[§bDiscord§7] §aDiscord RPC initialized!"));
                }

                while (this.running && this.linkAlive) {
                    try {
                        updateActivity();
                        Thread.sleep(15000L);
                    } catch (InterruptedException e) {
                        break;
                    } catch (IOException e) {
                        System.out.println("[Discord] Error in update loop: " + e.getMessage());
                        break;
                    }
                }
            } catch (IOException e) {
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§7[§bDiscord§7] §cDiscord is not running! Open Discord to use RPC."));
                }
            } catch (Throwable t) {
                String msg = "§cDiscord RPC failed: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
                System.err.println("[Discord] " + msg);
                if (mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(new ChatComponentText("§7[§bDiscord§7] " + msg));
                }
                t.printStackTrace();
            } finally {
                closeQuietly();
            }

            if (this.running) {
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void connect() throws IOException {
        transport = openTransport();

        JSONObject handshake = new JSONObject();
        handshake.put("v", 1);
        handshake.put("client_id", CLIENT_ID);

        synchronized (sendLock) {
            transport.send(OP_HANDSHAKE, handshake.toString());
        }

        linkAlive = true;
        startReader();
    }

    private void startReader() {
        readerThread = new Thread(this::readLoop, "Discord-RPC-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        Transport t = transport;
        try {
            while (this.running && this.linkAlive && t != null) {
                Frame frame = t.receive();
                if (frame.opcode() == OP_PING) {
                    synchronized (sendLock) {
                        t.send(OP_PONG, frame.payload());
                    }
                } else if (frame.opcode() == OP_CLOSE) {
                    linkAlive = false;
                }
            }
        } catch (IOException e) {
            linkAlive = false;
        }
    }

    private void updateActivity() throws IOException {
        String state;
        final ServerData serverData = mc.getCurrentServerData();

        if (serverData == null) {
            state = "Playing alone because of unemployment";
        } else if (this.hideServer.get()) {
            state = "Playing on a Server";
        } else {
            state = "Playing on " + serverData.serverIP;
        }

        JSONObject assets = new JSONObject();
        assets.put("large_image", "icons");
        assets.put("large_text", "rania client");

        JSONObject timestamps = new JSONObject();
        timestamps.put("start", startTimestamp);

        JSONObject activity = new JSONObject();
        activity.put("state", state);
        activity.put("assets", assets);
        activity.put("timestamps", timestamps);

        JSONObject args = new JSONObject();
        args.put("pid", ProcessHandle.current().pid());
        args.put("activity", activity);

        JSONObject frame = new JSONObject();
        frame.put("cmd", "SET_ACTIVITY");
        frame.put("args", args);
        frame.put("nonce", UUID.randomUUID().toString());

        Transport t = transport;
        if (t == null) {
            throw new IOException("Transport closed");
        }

        synchronized (sendLock) {
            t.send(OP_FRAME, frame.toString());
        }
    }

    private void closeQuietly() {
        linkAlive = false;

        Thread reader = readerThread;
        readerThread = null;
        if (reader != null && reader != Thread.currentThread()) {
            reader.interrupt();
        }

        if (transport != null) {
            try {
                transport.close();
            } catch (IOException ignored) {
            }
            transport = null;
        }
    }

    @Override
    public void onEnabled() {
        new Thread(this::setupActivity, "Discord-RPC-Init").start();
    }

    @Override
    public void onDisabled() {
        this.running = false;
        closeQuietly();
    }

    private static Transport openTransport() throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return WindowsTransport.connect();
        }
        return UnixTransport.connect();
    }

    private record Frame(int opcode, String payload) {
    }

    private interface Transport extends Closeable {
        void send(int opcode, String payload) throws IOException;

        Frame receive() throws IOException;
    }

    private static final class WindowsTransport implements Transport {
        private final RandomAccessFile pipe;

        private WindowsTransport(RandomAccessFile pipe) {
            this.pipe = pipe;
        }

        static WindowsTransport connect() throws IOException {
            IOException last = null;
            for (int i = 0; i < 10; i++) {
                try {
                    RandomAccessFile raf = new RandomAccessFile("\\\\.\\pipe\\discord-ipc-" + i, "rw");
                    return new WindowsTransport(raf);
                } catch (IOException e) {
                    last = e;
                }
            }
            throw last != null ? last : new IOException("No Discord IPC pipe found");
        }

        @Override
        public void send(int opcode, String payload) throws IOException {
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            header.putInt(opcode);
            header.putInt(data.length);
            pipe.write(header.array());
            pipe.write(data);
        }

        @Override
        public Frame receive() throws IOException {
            byte[] header = new byte[8];
            pipe.readFully(header);
            ByteBuffer buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);
            int opcode = buf.getInt();
            int length = buf.getInt();
            byte[] payload = new byte[length];
            pipe.readFully(payload);
            return new Frame(opcode, new String(payload, StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            pipe.close();
        }
    }

    private static final class UnixTransport implements Transport {
        private final SocketChannel channel;

        private UnixTransport(SocketChannel channel) {
            this.channel = channel;
        }

        static UnixTransport connect() throws IOException {
            IOException last = null;
            for (Path dir : socketDirs()) {
                for (int i = 0; i < 10; i++) {
                    Path socketPath = dir.resolve("discord-ipc-" + i);
                    if (!Files.exists(socketPath)) {
                        continue;
                    }
                    try {
                        SocketChannel channel = SocketChannel.open(StandardProtocolFamily.UNIX);
                        channel.connect(UnixDomainSocketAddress.of(socketPath));
                        return new UnixTransport(channel);
                    } catch (IOException e) {
                        last = e;
                    }
                }
            }
            throw last != null ? last : new IOException("No Discord IPC socket found");
        }

        private static List<Path> socketDirs() {
            List<Path> dirs = new ArrayList<>();
            for (String envVar : new String[]{"XDG_RUNTIME_DIR", "TMPDIR", "TMP", "TEMP"}) {
                String val = System.getenv(envVar);
                if (val != null) {
                    Path p = Path.of(val);
                    if (Files.isDirectory(p)) {
                        dirs.add(p);
                    }
                }
            }
            dirs.add(Path.of("/tmp"));
            return dirs;
        }

        @Override
        public void send(int opcode, String payload) throws IOException {
            byte[] data = payload.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = ByteBuffer.allocate(8 + data.length).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(opcode);
            buffer.putInt(data.length);
            buffer.put(data);
            buffer.flip();
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        @Override
        public Frame receive() throws IOException {
            ByteBuffer header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            while (header.hasRemaining()) {
                if (channel.read(header) == -1) {
                    throw new IOException("Discord IPC closed");
                }
            }
            header.flip();
            int opcode = header.getInt();
            int length = header.getInt();
            ByteBuffer payload = ByteBuffer.allocate(length);
            while (payload.hasRemaining()) {
                if (channel.read(payload) == -1) {
                    throw new IOException("Discord IPC closed");
                }
            }
            return new Frame(opcode, new String(payload.array(), StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }
    }
}