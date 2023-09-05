package io.redspace.ironsspellbooks.api.util;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.entity.spells.EarthquakeAoe;
import io.redspace.ironsspellbooks.network.ClientboundSyncCameraShake;
import io.redspace.ironsspellbooks.setup.Messages;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

//TODO: make IManager and shit
@Mod.EventBusSubscriber
public class CameraShakeManager {
    public static final List<CameraShakeData> cameraShakeData = new ArrayList<>();
    public static final List<CameraShakeData> clientCameraShakeData = new ArrayList<>();

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (!event.level.isClientSide && event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel serverLevel) {
            ArrayList<CameraShakeData> finished = new ArrayList<>();
            for (CameraShakeData data : cameraShakeData) {
                if (data.tickCount++ >= data.duration) {
                    finished.add(data);
                }
            }
            if (!finished.isEmpty()) {
                cameraShakeData.removeAll(finished);
                doSync(serverLevel);
            }
        }
    }

    public static void addCameraShake(ServerLevel level, CameraShakeData data) {
        cameraShakeData.add(data);
        doSync(level);
    }

    public static void removeCameraShake(CameraShakeData data) {
        cameraShakeData.remove(data);
    }

    private static void doSync(ServerLevel serverLevel) {
        serverLevel.players().forEach(serverPlayer -> Messages.sendToPlayer(new ClientboundSyncCameraShake(cameraShakeData), serverPlayer));
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void handleCameraShake(ViewportEvent.ComputeCameraAngles event) {
        if (clientCameraShakeData.isEmpty()) {
            return;
        }
        var player = event.getCamera().getEntity();
        List<CameraShakeData> closestPositions = clientCameraShakeData.stream().sorted((o1, o2) -> (int) (o1.origin.distanceToSqr(player.position()) - o2.origin.distanceToSqr(player.position()))).toList();
        var closestPos = closestPositions.get(0).origin;
        //.0039f is 1/15^2
        float intensity = (float) Mth.clampedLerp(1, 0, closestPos.distanceToSqr(player.position()) * 0.0039f);
        float f = (float) (player.tickCount + event.getPartialTick());
        float yaw = Mth.cos(f * 1.5f) * intensity * .35f;
        float pitch = Mth.cos(f * 2f) * intensity * .35f;
        float roll = Mth.sin(f * 2.2f) * intensity * .35f;
        event.setYaw(event.getYaw() + yaw);
        event.setRoll(event.getRoll() + roll);
        event.setPitch(event.getPitch() + pitch);
    }
}
