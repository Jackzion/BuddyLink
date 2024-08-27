<template>
  <div v-for="followVO in followVOList"
       style="display: flex; align-items: center; justify-content: space-between; height: 56px">
<!--    userAvatar and userName-->
    <div style="display: flex; align-items: center; justify-content: center; margin-left: 12px">
      <van-image
          round
          width="3rem"
          height="3rem"
          :src="followVO.avatarUrl"
          style="margin-top: 20px; margin-bottom: 20px; box-shadow: 0 0 15px rgba(0, 0, 0, 0.4)"
      />
      <span style="margin-left: 12px; color: #999999">{{ followVO.username }}</span>
    </div>
    <van-button v-if="!followVO.isFollowed" type="primary" style="margin-right: 20px"
                @click="addFollow(followVO.id, followVO.isFollowed)">关注
    </van-button>
    <van-button v-else type="success" style="margin-right: 20px"
                @click="deleteFollow(followVO.id, followVO.isFollowed)">取关
    </van-button>
  </div>
  <van-divider/>

</template>

<script setup lang="ts">

import {FollowVOType} from "../models/followVO";
import myAxios from "../config/myAxios.ts";
import {showSuccessToast, showToast} from "vant";

interface FollowCardList {
  followVOList: FollowVOType[];
}

withDefaults(defineProps<FollowCardList>(), {
  followVOList: () => []
});

// 关注
const addFollow = async (followeeId: number, isFollowed: boolean) => {
  // 构建请求参数
  const followAddRequest = {
    followeeId: followeeId,
    isFollowed: isFollowed
  }
  // 发送请求
  const res = await myAxios.post("/follow/addFollow", followAddRequest);
  if (res?.code === 0) {
    showSuccessToast('关注成功');
  } else {
    showToast('关注失败');
  }
  console.log(res.data);
};

// 取消关注
const deleteFollow = async (followeeId: number, isFollowed: boolean) => {
  // 构建请求参数
  const followAddRequest = {
    followeeId: followeeId,
    isFollowed: isFollowed
  }
  // 发送请求
  const res = await myAxios.post("/follow/addFollow", followAddRequest);
  if (res?.code === 0) {
    showSuccessToast('取消关注成功');
  } else {
    showToast('取消关注失败');
  }
  console.log(res.data);
};
</script>

<style scoped>

</style>