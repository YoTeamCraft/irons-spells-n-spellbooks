package io.redspace.ironsspellbooks.entity.spells;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public abstract class AbstractBulletProjectile extends Projectile {
    protected static final int EXPIRE_TIME = 15 * 20;

    protected float damage;
    protected float explosionRadius;

    /**
     * Client Side, called every tick
     */
    public abstract void trailParticles();
    /**
     * Server Side, called alongside onHit()
     */
    public abstract void impactParticles(double x, double y, double z);
    public abstract float getSpeed();
    public abstract Optional<SoundEvent> getImpactSound();

    public AbstractBulletProjectile(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        if (!respectsGravity()) {
            setNoGravity(true);
        }
    }

    public void shoot(Vec3 rotation) {
        setDeltaMovement(rotation.scale(getSpeed()));
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    public float getDamage() {
        return damage;
    }

    public float getExplosionRadius() {
        return explosionRadius;
    }

    public void setExplosionRadius(float explosionRadius) {
        this.explosionRadius = explosionRadius;
    }

    /**
     * Should be setting isNoGravity, not deleting its functionality
     */
    @Deprecated(forRemoval = true)
    public boolean respectsGravity() {
        return false;
    }

    @Override
    protected boolean canHitEntity(Entity pTarget) {
        return super.canHitEntity(pTarget) && pTarget != getOwner();
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > EXPIRE_TIME) {
            discard();
            return;
        }
        if (level.isClientSide) {
            trailParticles();
        }
        HitResult hitresult = ProjectileUtil.getHitResult(this, this::canHitEntity);
        if (hitresult.getType() != HitResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, hitresult)) {
            onHit(hitresult);
        }
        setPos(position().add(getDeltaMovement()));
        ProjectileUtil.rotateTowardsMovement(this, 1);
        if (!this.isNoGravity()) {
            Vec3 vec34 = this.getDeltaMovement();
            this.setDeltaMovement(vec34.x, vec34.y - (double) 0.05F, vec34.z);
        }
    }

    @Override
    protected void onHit(HitResult hitresult) {
        super.onHit(hitresult);

        if (!level.isClientSide) {
            impactParticles(getX(), getY(), getZ());
            getImpactSound().ifPresent(this::doImpactSound);
        }
    }

    protected void doImpactSound(SoundEvent sound) {
        level.playSound(null, getX(), getY(), getZ(), sound, SoundSource.NEUTRAL, 2, .9f + Utils.random.nextFloat() * .2f);
    }

    @Override
    protected void defineSynchedData() {

    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.getDamage());
        if (explosionRadius != 0) {
            tag.putFloat("ExplosionRadius", explosionRadius);
        }
        tag.putInt("Age", tickCount);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
        if (tag.contains("ExplosionRadius")) {
            this.explosionRadius = tag.getFloat("ExplosionRadius");
        }
        this.tickCount = tag.getInt("Age");
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        if (!shouldPierceShields() && (pResult.getEntity() instanceof ShieldPart || pResult.getEntity() instanceof AbstractShieldEntity))
            this.onHitBlock(new BlockHitResult(pResult.getEntity().position(), Direction.fromYRot(this.getYRot()), pResult.getEntity().blockPosition(), false));
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    protected boolean shouldPierceShields() {
        return false;
    }
}
