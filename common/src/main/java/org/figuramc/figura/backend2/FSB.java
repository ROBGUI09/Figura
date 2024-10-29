package org.figuramc.figura.backend2;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.avatar.UserData;
import org.figuramc.figura.avatar.local.CacheAvatarLoader;
import org.figuramc.figura.ducks.ServerDataAccessor;
import org.figuramc.figura.gui.FiguraToast;
import org.figuramc.figura.server.avatars.EHashPair;
import org.figuramc.figura.server.packets.AvatarDataPacket;
import org.figuramc.figura.server.packets.CloseIncomingStreamPacket;
import org.figuramc.figura.server.packets.Packet;
import org.figuramc.figura.server.packets.c2s.*;
import org.figuramc.figura.server.packets.s2c.S2CBackendHandshakePacket;
import org.figuramc.figura.server.packets.s2c.S2CPingPacket;
import org.figuramc.figura.server.packets.s2c.S2CUserdataPacket;
import org.figuramc.figura.server.utils.Hash;
import com.mojang.datafixers.util.Pair;
import org.figuramc.figura.server.utils.StatusCode;
import org.figuramc.figura.server.utils.Utils;
import org.figuramc.figura.utils.FiguraText;

import java.io.*;
import java.util.*;

public abstract class FSB {
    private static FSB instance;
    private byte[] key;
    private S2CBackendHandshakePacket s2CHandshake;
    private State state = State.Uninitialized;
    private final HashMap<UUID, UserData> awaitingUserdata = new HashMap<>();
    private int nextOutputId;
    private final HashMap<Integer, AvatarOutputStream> outputStreams = new HashMap<>();
    private int nextInputId;
    private final HashMap<Integer, AvatarInputStream> inputStreams = new HashMap<>();

    private int handshakeTick = 0;
    private int handshakeAttempts = 0;
    private static final int HANDSHAKE_SEND_DELAY = 40;
    private static final int MAX_ATTEMPTS_TO_CONNECT = 10;

    protected FSB() {
        if (instance != null) throw new IllegalStateException("Unable to create more than one FSB instance");
        instance = this;
    }

    public static FSB instance() {
        return instance;
    }

    public State state() {
        return state;
    }

    public boolean connected() {
        return s2CHandshake != null && state == State.Connected;
    }

    public S2CBackendHandshakePacket handshake() {
        return s2CHandshake;
    }

    private static boolean fsbAllowed() {
        ServerDataAccessor data = (ServerDataAccessor) Minecraft.getInstance().getCurrentServer();
        return data != null && data.figura$allowFigura();
    }

    public void handleHandshake(S2CBackendHandshakePacket packet) {
        if (fsbAllowed() && state == State.HandshakeSent) {
            s2CHandshake = packet;
            state = State.Connected;
            sendPacket(new C2SBackendHandshakePacket());
            FiguraToast.sendToast(FiguraText.of("backend.fsb_connected"));
            AvatarManager.clearAllAvatars();
        }
    }

    public void handleConnectionRefusal() {

    }

    public void getUser(UserData userData) {
        awaitingUserdata.put(userData.id, userData);
        sendPacket(new C2SFetchUserdataPacket(userData.id));
    }

    public void uploadAvatar(String avatarId, byte[] avatarData) {
        outputStreams.put(nextOutputId, new AvatarOutputStream(this, avatarId, nextOutputId, avatarData));
        Hash hash = Utils.getHash(avatarData);
        Hash ehash = getEHash(hash);
        sendPacket(new C2SUploadAvatarPacket(nextOutputId, avatarId, hash, ehash));
        nextOutputId++;
    }

    public void deleteAvatar(String avatarId) {
        sendPacket(new C2SDeleteAvatarPacket(avatarId));
    }

    public void equipAvatar(List<Pair<String, Hash>> avatars) {
        HashMap<String, EHashPair> eHashPairs = new HashMap<>();
        for (Pair<String, Hash> pair: avatars) {
            eHashPairs.put(pair.getFirst(), new EHashPair(pair.getSecond(), getEHash(pair.getSecond())));
        }
        sendPacket(new C2SEquipAvatarsPacket(eHashPairs));
    }

    public void onDisconnect() {
        s2CHandshake = null;
        state = State.Uninitialized;
        inputStreams.clear();
        outputStreams.clear();
        nextInputId = 0;
        nextOutputId = 0;
        NetworkStuff.backendStatus = 1;
        // TODO: Handling disconnecting properly, closing all incoming/outcoming streams, etc.
    }

    public void tick() {
        if (!fsbAllowed()) return;
        if (!connected()) {
            if (state != State.Refused && handshakeAttempts < MAX_ATTEMPTS_TO_CONNECT) {
                handshakeTick++;
                if (handshakeTick == HANDSHAKE_SEND_DELAY) {
                    sendPacket(new C2SBackendHandshakePacket());
                    state = State.HandshakeSent;
                    handshakeTick = 0;
                    handshakeAttempts++;
                }
            }
        }
        else outputStreams.forEach((i, s) -> s.tick());
    }

    public void getAvatar(UserData target, String hash) {
        Hash h = Utils.parseHash(hash);
        inputStreams.put(nextInputId, new AvatarInputStream(this, nextInputId, h, getEHash(h), target));
        sendPacket(new C2SFetchAvatarPacket(nextInputId, h));
        nextInputId++;
    }

    public void handleUserdata(S2CUserdataPacket packet) {
        UserData user = awaitingUserdata.get(packet.target());
        if (user != null) {
            boolean isHost = FiguraMod.isLocal(user.id);
            ArrayList<Pair<String, Pair<String, UUID>>> list = new ArrayList<>();
            packet.avatars().forEach((id, hashPair) -> {
                if (!isHost || getEHash(hashPair.hash()).equals(hashPair.ehash())) {
                    list.add(new Pair<>(hashPair.hash().toString(), new Pair<>(id, user.id)));
                }
            });
            user.loadData(list, new Pair<>(packet.prideBadges(), new BitSet()));
            awaitingUserdata.remove(packet.target());
        }
    }

    public void handleAvatarData(int streamId, byte[] chunk, boolean finalChunk) {
        var inputStream = inputStreams.get(streamId);
        if (inputStream == null) {
            sendPacket(new CloseIncomingStreamPacket(streamId, StatusCode.INVALID_STREAM_ID));
            return;
        }
        inputStream.acceptDataChunk(chunk, finalChunk);
    }

    public void handleAllow(int stream) {
        var outputStream = outputStreams.get(stream);
        if (outputStream != null) {
            outputStream.allow();
        }
    }

    public void closeIncomingStream(int streamId, StatusCode code) {
        var inputStream = inputStreams.get(streamId);
        if (inputStream != null) {
            inputStream.close(code);
        }
    }

    public void closeOutcomingStreamPacket(int streamId, StatusCode code) {
        var outputStream = outputStreams.get(streamId);
        if (outputStream != null) {
            outputStream.close(code);
        }
    }

    public void handlePing(S2CPingPacket packet) {
        Avatar avatar = AvatarManager.getLoadedAvatar(packet.sender());
        if (avatar == null)
            return;
        avatar.runPing(packet.id(), packet.data());
    }

    public abstract void sendPacket(Packet packet);

    public Hash getEHash(Hash hash) {
        byte[] hashBytes = hash.get();
        byte[] key = getKey();
        byte[] ehashBytes = new byte[hashBytes.length + key.length];
        System.arraycopy(hashBytes, 0, ehashBytes, 0, hashBytes.length);
        System.arraycopy(key, 0, ehashBytes, hashBytes.length, key.length);
        return Utils.getHash(ehashBytes);
    }

    private static File keyFile() {
        return FiguraMod.getFiguraDirectory().resolve(".fsbkey").toFile();
    }

    public byte[] getKey() {
        if (key == null) {
            var f = keyFile();
            if (f.exists()) {
                try (FileInputStream fis = new FileInputStream(f)) {
                    key = fis.readAllBytes();
                }
                catch (IOException e) {
                    FiguraMod.LOGGER.error("Error occured while getting a key for FSB: ", e);
                    key = new byte[16];;
                }
            }
            else {
                regenerateKey();
            }
        }
        return key;
    }

    public void regenerateKey() {
        Random rnd = new Random();
        key = new byte[16];
        rnd.nextBytes(key);
        try (FileOutputStream fos = new FileOutputStream(keyFile())) {
            fos.write(key);
        }
        catch (IOException e) {
            FiguraMod.LOGGER.error("Error occured while writing a key for FSB: ", e);
        }
    }

    private static class AvatarInputStream {
        private final FSB parent;
        private final int id;
        private final Hash hash;
        private final Hash ehash;
        private final UserData target;
        private final LinkedList<byte[]> dataChunks = new LinkedList<>();
        private int size = 0;

        private AvatarInputStream(FSB parent, int id, Hash hash, Hash ehash, UserData target) {
            this.parent = parent;
            this.id = id;
            this.hash = hash;
            this.ehash = ehash;
            this.target = target;
        }

        private void acceptDataChunk(byte[] chunk, boolean finalChunk) {
            dataChunks.add(chunk);
            size += chunk.length;
            if (finalChunk) {
                byte[] avatarData = new byte[size];
                int offset = 0;
                for (byte[] dataChunk : dataChunks) {
                    System.arraycopy(dataChunk, 0, avatarData, offset, dataChunk.length);
                    offset += dataChunk.length;
                }
                Hash resultHash = Utils.getHash(avatarData);
                if (!resultHash.equals(hash)) {
                    parent.sendPacket(new CloseIncomingStreamPacket(id, StatusCode.INVALID_HASH));
                }
                if (FiguraMod.isLocal(target.id) && !parent.getEHash(hash).equals(ehash)) {
                    parent.sendPacket(new CloseIncomingStreamPacket(id, StatusCode.OWNERSHIP_CHECK_ERROR));
                }

                try {
                    ByteArrayInputStream bais = new ByteArrayInputStream(avatarData);
                    CompoundTag tag = NbtIo.readCompressed(bais);
                    CacheAvatarLoader.save(hash.toString(), tag);
                    target.loadAvatar(tag);
                }
                catch (Exception e) {
                    FiguraMod.LOGGER.error("Failed to load avatar for " + target.id, e);
                }
                parent.inputStreams.remove(id);
            }
        }

        private void close(StatusCode code) {
            switch (code) {
                case AVATAR_DOES_NOT_EXIST -> FiguraMod.LOGGER.info("Avatar with hash %s does not exist on this server".formatted(hash));
                default -> FiguraMod.LOGGER.error("Incoming stream was closed by unexpected reason: %s".formatted(code.name()));
            }
            parent.inputStreams.remove(id);
        }
    }

    private static class AvatarOutputStream {
        private final FSB parent;
        private final String avatarId;
        private final int id;
        private final byte[] data;
        private int position;
        private boolean upload;

        private AvatarOutputStream(FSB parent, String avatarId, int id, byte[] data) {
            this.parent = parent;
            this.avatarId = avatarId;
            this.id = id;
            this.data = data;
        }

        private void tick() {
            if (upload) {
                int size = nextChunkSize();
                byte[] chunk = new byte[size];
                System.arraycopy(data, position, chunk, 0, chunk.length);
                position += size;
                boolean finalChunk = data.length == position;
                parent.sendPacket(new AvatarDataPacket(id, finalChunk, chunk));
                if (finalChunk) upload = false;
            }
        }

        private int nextChunkSize() {
            return Math.min(AvatarDataPacket.MAX_CHUNK_SIZE, data.length - position);
        }

        private void allow() {
            upload = true;
        }

        private void close(StatusCode code) {
            switch (code) {
                case FINISHED, ALREADY_EXISTS -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_success"));
                    parent.equipAvatar(List.of(Pair.of(avatarId, Utils.getHash(data))));
                    AvatarManager.localUploaded = true;
                }
                case MAX_AVATAR_SIZE_EXCEEDED -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_too_big"), FiguraToast.ToastType.ERROR);
                }
                default -> {
                    FiguraToast.sendToast(FiguraText.of("backend.upload_error"), code, FiguraToast.ToastType.ERROR);
                }
            }
            parent.outputStreams.remove(id);
        }
    }

    public enum State {
        Uninitialized,
        HandshakeSent,
        Connected,
        Refused
    }
}
