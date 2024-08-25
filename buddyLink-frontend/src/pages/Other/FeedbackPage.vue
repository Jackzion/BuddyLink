<template>
  <basic-layout>
    <div style="margin-left: 32px; margin-top: 30px">
      <span style="color: #C8C9CC; font-size: 15px">给网站评个分吧！</span>
      <van-rate v-model="score" color="#ffd21e" void-icon="star" allow-half @click="doRate"/>
    </div>

    <div style="position: relative">
      <van-cell-group inset>
        <van-field
            v-model="advice"
            rows="1"
            size="large"
            type="textarea"
            maxlength="500"
            placeholder="对网站的小建议"
            show-word-limit
        />
      </van-cell-group>
      <van-button type="success" style="position:absolute; right: 0; transform: translateY(-50%);  margin-top: 20px; margin-right: 12px;" @click="addFeedback">提交</van-button>
    </div>
  </basic-layout>
</template>

<script setup lang="ts">

import {ref} from "vue";
import {showToast} from "vant";
import BasicLayout from "../../layouts/BasicLayout.vue";
import myAxios from "../../config/myAxios.ts";

const score = ref(0);
const advice = ref('');

console.log(value)

let rate = null;

// 提交分数
const doRate = () => {
  rate = score.value;
};

// 提交反馈
const addFeedback = async () => {
  // 发出请求
  const res : any = await myAxios.post('/feedback/add', {
    rate: rate,
    advice: advice.value,
  });
  if (res?.code === 0) {
    showToast('提交成功，谢谢你的反馈');
    // 重置
    score.value = 0;
    advice.value = '  ';
  } else {
    showToast('提交失败');
  }
};


</script>

<style scoped>

</style>