import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useProfileDialogStore = defineStore('profileDialog', () => {
  const showDialog = ref(false);
  const userIcon = ref('mdi-account'); // 使用者的 icon

  function openDialog(icon = 'mdi-account') {
    userIcon.value = icon;
    showDialog.value = true;
  }

  function closeDialog() {
    showDialog.value = false;
  }

  return {
    showDialog,
    userIcon,
    openDialog,
    closeDialog,
  };
});
