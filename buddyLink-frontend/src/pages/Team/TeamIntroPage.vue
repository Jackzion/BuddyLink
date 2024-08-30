<template>
  <basic-layout>

    <div v-if="team" style="text-align: center;">
      <van-image
          round
          width="8rem"
          height="8rem"
          src="https://fastly.jsdelivr.net/npm/@vant/assets/ipad.jpeg"
          style="margin-top: 20px; margin-bottom: 20px; box-shadow: 0 0 15px rgba(0, 0, 0, 0.4)"
      />
    </div>

    <div v-if="team">
      <van-cell title="队伍名称" :value="team?.teamName" />
      <van-cell title="队伍描述" :value="team?.description"/>
      <van-cell title="过期时间" :value="team?.expireTime" />
      <van-cell title="队伍最大人数" :value="team?.maxNum" />
      <van-cell title="建队时间" :value="team?.createTime" />
      <van-cell title="队长" :value="(team?.createUserName ? `${team?.createUserName}` : `null`)" />
      <van-cell title="队伍人数" :value="team?.hasJoinNum" />
    </div>
  </basic-layout>

</template>

<script setup lang="ts">
import {useRoute} from "vue-router";
import {ref} from "vue";
import BasicLayout from "../../layouts/BasicLayout.vue";

const route = useRoute();
const team = ref({
  teamName: route.query.teamName,
  description: route.query.description,
  expireTime: route.query.expireTime,
  maxNum: route.query.maxNum,
  status: route.query.status,
  createTime: route.query.createTime,
  createUserName: route.query.createUserName,
  hasJoinNum: route.query.hasJoinNum,
});
</script>

<style scoped>

</style>