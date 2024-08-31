<template>
  <van-skeleton title avatar :row="3" :loading="loading" v-for="friend in friendList">
    <van-card
        :desc="friend.profile"
        :title="`${friend.username}(${friend.planetCode})`"
        :price="`${friend.distance} km`"
        currency=""
        :thumb="friend.avatarUrl"
    >
      <template #tags>
        <van-tag color="#7232dd" plain v-for="tag in friend.tags" style="margin-right: 8px; margin-top: 8px">
          {{ tag }}
        </van-tag>
      </template>
      <template #footer>
        <van-button size="normal" @click="toChat(friend)">聊天</van-button>
      </template>
    </van-card>
  </van-skeleton>

</template>
<script setup lang="ts">
import {ref} from "vue";
import {UserType} from "../models/user";
import {useRouter} from "vue-router";
const router = useRouter();
interface props {
  friendList: UserType[],
  loading: boolean
}
withDefaults(defineProps<props>(), {loading: false})

const loading = ref(false);
const toChat = (friend: any) => {
  router.push({
    path: '/chat',
    query: {
      id: friend.id,
      username: friend.username,
      userType: 1
    }
  })
}
</script>

<style lang="scss" scoped>

</style>