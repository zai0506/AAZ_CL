import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useProfileDialogStore = defineStore('profileDialog', () => {
  const showDialog = ref(false);

  function openDialog() {
    showDialog.value = true;
  }

  function closeDialog() {
    showDialog.value = false;
  }

  return {
    showDialog,
    openDialog,
    closeDialog,
  };
});
