<template>
  <basic-layout>
    <follow-card-list :followVOList="followVOList"/>
    <van-empty v-if="(!followVOList || followVOList.length < 1) && (type === '0' || type === '2')" description="没有粉丝"/>
    <van-empty v-if="(!followVOList || followVOList.length < 1) && (type === '1' || type === '3')" description="没有关注"/>
  </basic-layout>
</template>

<script setup lang="ts">

import {onMounted, ref} from "vue";
import {useRoute} from "vue-router";
import {showToast} from "vant";
import myAxios from "../../config/myAxios.ts";
import FollowCardList from "../../components/FollowCardList.vue";
import {getCurrentUser} from "../../services/user.ts";
import BasicLayout from "../../layouts/BasicLayout.vue";

const route = useRoute();

const followVOList = ref([]);

// // 查询类型（0 查自己粉丝，1 查自己关注，2 查别人粉丝，3 查别人关注） , todo ： 现在只存在自己的关注列表
const type = ref(route.query.type);

onMounted(async () => {
  const user = await getCurrentUser();
  console.log(type.value)
  // 请求 查看 follow list
  const res : any = await myAxios.post('/follow/list', {
    userId : user.id,
    type: type.value,
  });
  if (res?.code === 0) {
    followVOList.value = res.data;
  } else {
    showToast('查询失败');
  }
  console.log(res.data);
});
</script>

<style scoped>

</style>