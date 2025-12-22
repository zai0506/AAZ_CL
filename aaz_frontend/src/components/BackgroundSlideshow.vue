<template>
  <div class="background-slideshow">
    <div v-for="(image, index) in images" :key="image" class="slide" :class="{ visible: index === currentIndex }"
      :style="{ backgroundImage: 'url(' + image + ')' }"></div>
    <div class="overlay"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import { images } from '@/utils/backgroundImages.js';

const currentIndex = ref(0);
let intervalId = null;

onMounted(() => {
  // Set the first image randomly as well
  if (images.length > 0) {
    currentIndex.value = Math.floor(Math.random() * images.length);
  }

  intervalId = setInterval(() => {
    if (images.length < 2) return; // Not enough images to randomize

    let nextIndex;
    do {
      nextIndex = Math.floor(Math.random() * images.length);
    } while (nextIndex === currentIndex.value);

    currentIndex.value = nextIndex;
  }, 7000); // Change image every 7 seconds
});

onUnmounted(() => {
  if (intervalId) {
    clearInterval(intervalId);
  }
});
</script>

<style scoped>
.background-slideshow {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 0;
  /* Base stacking context for the whole app */
  background-color: #000;
  /* Fallback color */
}

.slide {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-size: cover;
  background-position: center;
  opacity: 0;
  transition: opacity 1.5s ease-in-out;
  z-index: 1;
  /* Behind the overlay */
}

.slide.visible {
  opacity: 1;
}

.overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.2);
  z-index: 2;
  /* On top of the slides */
}
</style>
