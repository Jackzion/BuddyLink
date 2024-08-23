<template>
<!--  吸顶布局 -->
  <van-sticky>
<!--    标题-->
    <van-nav-bar
        :title="`登录`"
        left-arrow
        @click-left="onClickLeft"
    >
    </van-nav-bar>
    <div style="text-align: center; margin-top: 30px; margin-bottom: 15px">
<!--      图标  todo:更换图标 -->
      <van-image
          round
          width="10rem"
          height="8rem"
          src="https://article-images.zsxq.com/Fnj10F-xlXGAHOGBru1dT6tmJsLr"
      />
    </div>
<!--    标题-->
    <div id="title" style="text-align: center; margin-top: 30px; margin-bottom: 30px">
      <h3>homie匹配，专业寻找学习伙伴</h3>
    </div>
    <!--    登录表单-->
    <van-form @submit="onSubmit">
      <van-cell-group inset>
        <van-field
            v-model="userAccount"
            name="userAccount"
            label="账号"
            placeholder="请输入账号"
            :rules="[{ required: true, message: '请输入账号' }]"
        />
        <van-field
            v-model="userPassword"
            type="password"
            name="userPassword"
            label="密码"
            placeholder="请输入密码"
            :rules="[{ required: true, message: '请填写密码' }]"
        />
      </van-cell-group>
      <div style="margin: 16px;">
        <van-button round block type="primary" native-type="submit">
          登录
        </van-button>
      </div>
      <div style="margin: 16px;">
        <van-button round block type="primary" is-link to="/user/register">
          注册
        </van-button>
      </div>
    </van-form>

  </van-sticky>
</template>

<script setup lang="ts">

import {ref} from "vue";
import {useRoute, useRouter} from "vue-router";
import {showSuccessToast} from "vant";
import myAxios from "../../config/myAxios.ts";

const router = useRouter();
const route = useRoute();
const userAccount = ref('');
const userPassword = ref('');

const onSubmit = async () => {
  // 发送登录请求
  const res : any = await myAxios.post('/user/login' , {
    userAccount: userAccount.value,
    userPassword: userPassword.value
  })
  // 请求获取到数据
  if(res.data){
    showSuccessToast('登录成功');
    // 跳转到之前的页面
    const redirectUrl = route.query?.redirect as string ?? '/';
    window.location.href = redirectUrl;
  }else {
    showSuccessToast('登录失败' + (`${res.description}` ? `，${res.description}` : ''));
  }
}

const onClickLeft = () => {
  router.back();
}

</script>

<style lang="scss" scoped>

</style>