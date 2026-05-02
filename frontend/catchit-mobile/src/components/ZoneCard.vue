<template>
  <article class="zone-card" :class="{ 'is-owned': owned }" :style="cardStyle">
    <div class="zone-card__header-art" aria-hidden="true">
      <span class="zone-card__shape zone-card__shape--diamond"></span>
      <span class="zone-card__shape zone-card__shape--stripe"></span>
      <span class="zone-card__shape zone-card__shape--cut"></span>
    </div>

    <div class="zone-card__content">
      <p class="zone-card__eyebrow">Zone</p>
      <h3 class="zone-card__name">{{ zoneName }}</h3>

      <div class="zone-card__meta">
        <span class="zone-card__meta-label">Valid until:</span>
        <span class="zone-card__meta-value">{{ displayValidUntil }}</span>
      </div>

      <p v-if="ownerName" class="zone-card__owner">{{ ownerName }}</p>
    </div>

    <div v-if="$slots.actions" class="zone-card__actions">
      <slot name="actions" />
    </div>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  zoneName: string
  zoneColor?: string
  validUntil?: string
  ownerName?: string
  owned?: boolean
}>(), {
  zoneColor: '#111111',
  validUntil: '',
  ownerName: '',
  owned: false,
})

const displayValidUntil = computed(() => props.validUntil?.trim() || ' ')

const cardStyle = computed(() => ({
  '--zone-accent': props.zoneColor,
}))
</script>

<style scoped>


.zone-card {
  position: relative;
  overflow: hidden;
  min-height: 220px;
  padding: 1rem 1rem 0.95rem;
  border-radius: 26px;
  background:
    radial-gradient(circle at 14% 14%, rgba(255, 255, 255, 0.06), transparent 24%),
    radial-gradient(circle at 100% 0%, rgba(255, 255, 255, 0.04), transparent 30%),
    linear-gradient(145deg, #101826 0%, #162131 48%, #1b2637 100%);
  border: 1px solid rgba(148, 163, 184, 0.16);
  box-shadow:
    0 16px 32px rgba(15, 23, 42, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.06);
  color: #f8fafc;
  max-width: 420px;
  
}

.zone-card::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.06), transparent 35%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.03), transparent 22%);
  pointer-events: none;
}

.zone-card__header-art {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.zone-card__shape {
  position: absolute;
  background: var(--zone-accent);
  opacity: 0.92;
}

.zone-card__shape--diamond {
  top: -26px;
  right: 20px;
  width: 92px;
  height: 92px;
  clip-path: polygon(50% 0%, 100% 50%, 50% 100%, 0% 50%);
  filter: drop-shadow(0 8px 18px rgba(0, 0, 0, 0.16));
}

.zone-card__shape--stripe {
  top: 0;
  right: 0;
  width: 42px;
  height: 100%;
  opacity: 0.8;
  clip-path: polygon(34% 0, 100% 0, 100% 100%, 0 100%);
}

.zone-card__shape--cut {
  left: 18px;
  bottom: -10px;
  width: 140px;
  height: 58px;
  opacity: 0.14;
  border-radius: 18px;
  transform: skewX(-18deg);
  background: linear-gradient(90deg, var(--zone-accent), rgba(255, 255, 255, 0));
}

.zone-card__content {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  min-height: 182px;
  gap: 0.35rem;
  padding-top: 48px;
}

.zone-card__eyebrow {
  margin: 0;
  font-size: 0.76rem;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(226, 232, 240, 0.72);
}

.zone-card__name {
  margin: 0;
  font-size: 1.55rem;
  line-height: 1;
  font-weight: 800;
  color: #ffffff;
  text-wrap: balance;
}

.zone-card__meta {
  display: flex;
  flex-direction: column;
  gap: 0.15rem;
  margin-top: 0.4rem;
}

.zone-card__meta-label {
  font-size: 0.92rem;
  font-weight: 700;
  color: rgba(226, 232, 240, 0.92);
}

.zone-card__meta-value {
  min-height: 1.1rem;
  font-size: 0.95rem;
  color: rgba(226, 232, 240, 0.74);
}

.zone-card__owner {
  margin: 0.35rem 0 0;
  font-size: 0.88rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  color: rgba(226, 232, 240, 0.72);
}

.zone-card__actions {
  position: relative;
  z-index: 1;
  margin-top: 0.95rem;
}
</style>